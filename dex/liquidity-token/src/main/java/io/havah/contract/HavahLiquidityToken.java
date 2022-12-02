package io.havah.contract;

import io.havah.contract.token.hsp20.HSP20Basic;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

import java.math.BigInteger;

public class HavahLiquidityToken extends HSP20Basic {

    private final VarDB<Address> minter = Context.newVarDB("minter", Address.class);

    public HavahLiquidityToken(String _name, String _symbol, int _decimals) {
        super(_name, _symbol, _decimals);

        // By default, set the minter role to the owner
        if (minter.get() == null) {
            minter.set(Context.getOwner());
        }
    }

    /**
     * Destroys `_value` tokens from the caller.
     */
    @External
    public void burn(BigInteger _value) {
        _burn(Context.getCaller(), _value);
    }

    /**
     * Creates _value number of tokens, and assigns to caller.
     * Increases the balance of that account and the total supply.
     */
    @External
    public void mint(BigInteger _value) {
        // simple access control - only the minter can mint new token
        Context.require(Context.getCaller().equals(minter.get()));
        _mint(Context.getCaller(), _value);
    }

    /**
     * Creates _value number of tokens, and assigns to _to.
     * Increases the balance of that account and the total supply.
     */
    @External
    public void mintTo(Address _to, BigInteger _value) {
        // simple access control - only the minter can mint new token
        Context.require(Context.getCaller().equals(minter.get()));
        _mint(_to, _value);
    }

    @External
    public void setMinter(Address _minter) {
        // simple access control - only the contract owner can set new minter
        Context.require(Context.getCaller().equals(Context.getOwner()));
        minter.set(_minter);
    }

    @External(readonly = true)
    public Address minter() {
        return minter.get();
    }
}
