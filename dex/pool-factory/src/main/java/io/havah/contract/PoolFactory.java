package io.havah.contract;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public class PoolFactory {
    protected final String name;
    private static final Address ZERO_ADDRESS = Address.fromString("cx0000000000000000000000000000000000000000");
    protected VarDB<byte[]> poolContract = Context.newVarDB("poolContract", byte[].class);
    protected VarDB<byte[]> tokenContract = Context.newVarDB("tokenContract", byte[].class);
    protected BranchDB<Address, DictDB<Address, Address>> liquidityPools = Context.newBranchDB("liquidityPools", Address.class);

    public PoolFactory(String _name) {
        this.name = _name;
    }

    @External(readonly = true) 
    public String name() {
        return name;
    }

    protected Address _findPool(Address _base, Address _quote) {
        Context.require(!_base.equals(_quote), "base and quote is same");
        Context.require(_base.isContract(), "base is not contract");
        Context.require(_quote.isContract(), "quote is not contract");

        if(_isReverse(_base, _quote)) {
            Address temp = _base;
            _base = _quote;
            _quote = temp;
        }

        return liquidityPools.at(_base).get(_quote);
    }

    protected boolean _isReverse(Address base, Address quote) {
        return base.hashCode() > quote.hashCode() ? !base.equals(ZERO_ADDRESS) : quote.equals(ZERO_ADDRESS);
    }

    @External
    public void setPoolContract(byte[] _contract) {
        // simple access control - only the contract owner can set new pool contract
        Context.require(Context.getCaller().equals(Context.getOwner()));
        Context.require(_contract != null, "_contract is null");
        Context.require(_contract.length > 0, "_contract length is 0");
        poolContract.set(_contract);
    }

    @External
    public void setLpTokenContract(byte[] _contract) {
        // simple access control - only the contract owner can set new token contract
        Context.require(Context.getCaller().equals(Context.getOwner()));
        Context.require(_contract != null, "_contract is null");
        Context.require(_contract.length > 0, "_contract length is 0");
        tokenContract.set(_contract);
    }

    @External(readonly = true)
    public Address findPool(Address _base, Address _quote) {
        return _findPool(_base, _quote);
    }

    @External
    public void createPool(Address _base, Address _quote) {
        // simple access control - only the contract owner can create new pool
        Context.require(Context.getCaller().equals(Context.getOwner()));

        Context.require(tokenContract.get() != null, "token contract not set");
        Context.require(poolContract.get() != null, "pool contract not set");

        Context.require(_findPool(_base, _quote) == null, "pool already created!");

        if(_isReverse(_base, _quote)) {
            Address temp = _base;
            _base = _quote;
            _quote = temp;
        }

        Address token = Context.deploy(tokenContract.get(), "HavahLiquidityToken", "HLT", BigInteger.valueOf(18));
        Context.require(token != null, "token is not deployed.");

        Address pool = Context.deploy(poolContract.get(), "LiquidityPool");
        Context.require(pool != null, "pool is not deployed.");
        Context.call(token, "setMinter", pool);

        Context.call(pool, "initialize", _base, _quote, token);

        liquidityPools.at(_base).set(_quote, pool);

        PoolCreated(_base, _quote, pool);
    }

    @EventLog(indexed=3)
    public void PoolCreated(Address _baseToken, Address _qouteToken, Address _poolAddress) {}
}
