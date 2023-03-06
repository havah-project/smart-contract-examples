package io.havah.contract;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static score.Context.require;

public class LiquidityPool {
    protected final String name;
    private static final Address NATIVE_COIN = Address.fromString("cx0000000000000000000000000000000000000000");
    private static final BigInteger FEE_SCALE = BigInteger.valueOf(10_000);
    private static final BigInteger MIN_LIQUIDITY = BigInteger.valueOf(1_000);
    protected final VarDB<Address> baseToken = Context.newVarDB("baseToken", Address.class);
    protected final VarDB<Address> quoteToken = Context.newVarDB("quoteToken", Address.class);
    protected final VarDB<Address> lpToken = Context.newVarDB("lpToken", Address.class);
    protected final VarDB<BigInteger> fee = Context.newVarDB("fee", BigInteger.class);

    public LiquidityPool(String _name) {
        this.name = _name;
        fee.set(BigInteger.valueOf(30));
    }

    @External(readonly = true)
    public String name() {
        return name;
    }

    @Payable
    public void fallback() {
        Context.require(lpToken.get() != null, "Pool not yet initialized");
        Context.require(NATIVE_COIN.equals(baseToken.get()), "not supported token");
    }

    @External
    public void initialize(Address _baseToken, Address _quoteToken, Address _lpToken) {
        // simple access control - only the contract owner can set new fee
        Context.require(Context.getCaller().equals(Context.getOwner()));

        Context.require(lpToken.get() == null, "Pool already initialized");
        Context.require(!_baseToken.equals(_quoteToken), "base and quote token is same address");
        Context.require(!_quoteToken.equals(NATIVE_COIN), "quoteToken is SYSTEM_ADDRESS");

        Context.require(_baseToken.isContract(), "baseToken is not contract");
        baseToken.set(_baseToken);

        Context.require(_quoteToken.isContract(), "quoteToken is not contract");
        quoteToken.set(_quoteToken);

        Context.require(_lpToken.isContract(), "lpToken is not contract");
        Context.require(Context.getAddress().equals(Context.call(_lpToken, "minter")), "lpToken minter is not pool");
        lpToken.set(_lpToken);
    }

    @Payable
    @External
    public void add(BigInteger _baseValue, BigInteger _quoteValue) {
        Context.require(lpToken.get() != null, "Pool not yet initialized");
        Context.require(_baseValue.signum() > 0, "_baseValue is a negative or zero value");
        Context.require(_quoteValue.signum() > 0, "_quoteValue is a negative or zero value");

        Address caller = Context.getCaller();

        BigInteger poolBaseAmount, poolQuoteAmount;

        Address base = baseToken.get();
        if (base.equals(NATIVE_COIN)) {
            Context.require(Context.getValue().equals(_baseValue), "invalid _baseAmount");
            poolBaseAmount = Context.getBalance(Context.getAddress()).subtract(_baseValue);
        } else {
            Context.require(Context.getValue().compareTo(BigInteger.ZERO) == 0, "msg.value is positive value");
            Context.require(_baseValue.compareTo((BigInteger) Context.call(base, "balanceOf", caller)) <= 0, "caller insufficient baseToken balance");
            Context.require(_baseValue.compareTo((BigInteger) Context.call(base, "allowance", caller, Context.getAddress())) <= 0, "caller insufficient baseToken balance");
            poolBaseAmount = (BigInteger) Context.call(base,"balanceOf", Context.getAddress());
            Context.call(base, "transferFrom", caller, Context.getAddress(), _baseValue);
        }

        Address quote = quoteToken.get();
        Context.require(_quoteValue.compareTo((BigInteger) Context.call(quote, "balanceOf", caller)) <= 0, "caller insufficient quoteValue balance");
        Context.require(_quoteValue.compareTo((BigInteger) Context.call(quote, "allowance", caller, Context.getAddress())) <= 0, "caller insufficient quoteValue balance");
        poolQuoteAmount = (BigInteger) Context.call(quote,"balanceOf", Context.getAddress());
        Context.call(quote, "transferFrom", caller, Context.getAddress(), _quoteValue);

        BigInteger baseToCommit = _baseValue;
        BigInteger quoteToCommit = _quoteValue;

        Address lp = lpToken.get();
        BigInteger liquidity = (BigInteger) Context.call(lp, "totalSupply");

        if(liquidity.signum() == 0) {
            liquidity = baseToCommit.multiply(quoteToCommit).sqrt();
            Context.require(liquidity.compareTo(MIN_LIQUIDITY) >= 0, "not enough liquidity");
        } else {
            BigInteger baseFromQuote = _quoteValue.multiply(poolBaseAmount).divide(poolQuoteAmount);
            BigInteger quoteFromBase = _baseValue.multiply(poolQuoteAmount).divide(poolBaseAmount);

            if (quoteFromBase.compareTo(_quoteValue) <= 0) {
                quoteToCommit = quoteFromBase;
            } else {
                baseToCommit = baseFromQuote;
            }

            BigInteger liquidityFromBase = (liquidity.multiply(baseToCommit)).divide(poolBaseAmount);
            BigInteger liquidityFromQuote = (liquidity.multiply(quoteToCommit)).divide(poolQuoteAmount);

            liquidity = liquidityFromBase.min(liquidityFromQuote);
            require(liquidity.compareTo(BigInteger.ZERO) >= 0, "LP tokens to mint is less than zero");
        }

        Context.call(lp, "mintTo", caller, liquidity);

        LiquidityAdded(caller, liquidity, _baseValue, _quoteValue);
    }

    @External
    public void remove(BigInteger _lpAmount) {
        Address lp = lpToken.get();
        Context.require(lpToken.get() != null, "Pool not yet initialized");
        Context.require(_lpAmount.signum() > 0, "_lpAmount is a negative or zero value");
        Address caller = Context.getCaller();

        BigInteger callerTotal = (BigInteger) Context.call(lp, "balanceOf", caller);
        Context.require(callerTotal.signum() > 0, "caller insufficient lp balance");
        Context.require(callerTotal.compareTo(_lpAmount) >= 0, "_lpAmount grater then caller lp balance");

        BigInteger liquidity = (BigInteger) Context.call(lp, "totalSupply");
        Context.require(liquidity.compareTo(_lpAmount) >= 0, "insufficient balance");

        Context.call(lpToken.get(), "transferFrom", caller, Context.getAddress(), _lpAmount);
        Context.call(lpToken.get(), "burn", _lpAmount);

        BigInteger poolBaseAmount, poolQuoteAmount;

        Address baseAddress = baseToken.get();
        poolBaseAmount = baseAddress.equals(NATIVE_COIN) ? Context.getBalance(Context.getAddress()) : (BigInteger) Context.call(baseAddress,"balanceOf", Context.getAddress());

        Address quoteAddress = quoteToken.get();
        poolQuoteAmount = (BigInteger) Context.call(quoteAddress,"balanceOf", Context.getAddress());

        BigInteger baseToWithdraw = _lpAmount.multiply(poolBaseAmount).divide(liquidity);
        BigInteger quoteToWithdraw = _lpAmount.multiply(poolQuoteAmount).divide(liquidity);

        if (baseAddress.equals(NATIVE_COIN)) {
            Context.transfer(caller, baseToWithdraw);
        } else {
            Context.call(baseAddress, "transfer", caller, baseToWithdraw);
        }
        Context.call(quoteAddress, "transfer", caller, quoteToWithdraw);

        LiquidityRemoved(caller, _lpAmount, baseToWithdraw, quoteToWithdraw);
    }

    @Payable
    @External
    public void swap(Address _receiver, Address _fromToken, Address _toToken, BigInteger _value, BigInteger _minimumReceive) {
        Context.require(lpToken.get() != null, "Pool not yet initialized");

        Context.require(_value.signum() > 0, "_value is a negative or zero value");
        Context.require(!_fromToken.equals(_toToken), "from and to token is same address");

        Context.require(_fromToken.isContract(), "_fromToken is not contract");
        Context.require(_toToken.isContract(), "_toToken is not contract");

        BigInteger liquidity = (BigInteger) Context.call(lpToken.get(), "totalSupply");
        Context.require(liquidity.compareTo(MIN_LIQUIDITY) >= 0, "not enough liquidity");

        if(_fromToken.equals(NATIVE_COIN))
            Context.require(Context.getValue().compareTo(_value) == 0, "invalid _value");
        else {
            Context.require(Context.getValue().equals(BigInteger.ZERO), "msg.value is not zero");
        }

        Address pool = Context.getAddress();
        Address base = baseToken();
        Address quote = quoteToken();

        if (!_fromToken.equals(NATIVE_COIN)) {
            Context.require(_fromToken.equals(base) || _fromToken.equals(quote), "_fromToken is not supported");
        }
        if (!_toToken.equals(NATIVE_COIN)) {
            Context.require(_toToken.equals(base) || _toToken.equals(quote), "_toToken is not supported");
        }

        Address from, to;
        if (quote.equals(_toToken)) {
            from = base;
            to = quote;
        } else {
            from = quote;
            to = base;
        }
        BigInteger lpFees = _value.multiply(fee()).divide(FEE_SCALE);

        BigInteger oldFromToken = from.equals(NATIVE_COIN) ? Context.getBalance(pool).subtract(_value) : (BigInteger) Context.call(from, "balanceOf", pool);
        BigInteger oldToToken = to.equals(NATIVE_COIN) ? Context.getBalance(pool) : (BigInteger) Context.call(to, "balanceOf", pool);

        BigInteger inputWithoutFees = _value.subtract(lpFees);

        BigInteger newFromToken = oldFromToken.add(inputWithoutFees);
        BigInteger newToToken = (oldFromToken.multiply(oldToToken)).divide(newFromToken);

        BigInteger sendAmount = oldToToken.subtract(newToToken);

        Context.require(sendAmount.compareTo(BigInteger.ZERO) > 0, "Invalid output amount in trade.");
        Context.require(sendAmount.compareTo(_minimumReceive) >= 0,
                "MinimumReceiveError: Receive amount " + sendAmount + " below supplied minimum");

        if (!from.equals(NATIVE_COIN))
            Context.call(from, "transferFrom", Context.getCaller(), pool, _value);

        if (to.equals(NATIVE_COIN)) {
            Context.transfer(_receiver, sendAmount);
        } else {
            Context.call(to, "transfer", _receiver, sendAmount);
        }

        Swap(_fromToken, _toToken, Context.getCaller(), _receiver, _value, sendAmount, lpFees);
    }

    @External
    public void setFee(BigInteger _fee) {
        // simple access control - only the contract owner can set new fee
        Context.require(Context.getCaller().equals(Context.getOwner()));
        Context.require(_fee.signum() >= 0, "_fee is a negative value");
        fee.set(_fee);
    }

    @External(readonly = true)
    public BigInteger fee() {
        return fee.get();
    }

    @External(readonly = true)
    public BigInteger feeScale() {
        return FEE_SCALE;
    }

    @External(readonly = true)
    public Address baseToken() {
        Context.require(lpToken.get() != null, "Pool not yet initialized");
        return baseToken.get();
    }

    @External(readonly = true)
    public Address quoteToken() {
        Context.require(lpToken.get() != null, "Pool not yet initialized");
        return quoteToken.get();
    }

    @External(readonly = true)
    public Address lpToken() {
        Context.require(lpToken.get() != null, "Pool not yet initialized");
        return lpToken.get();
    }

    protected Map<String, Object> _priceOfAInB(Address tokenA, Address tokenB) {
        BigInteger ATokenTotal = tokenA.equals(NATIVE_COIN) ? Context.getBalance(Context.getAddress()) : (BigInteger) Context.call(tokenA, "totalSupply");
        BigInteger BTokenTotal = tokenB.equals(NATIVE_COIN) ? Context.getBalance(Context.getAddress()) : (BigInteger) Context.call(tokenB, "totalSupply");
        BigInteger ATokenDecimals = tokenA.equals(NATIVE_COIN) ? BigInteger.valueOf(18) : (BigInteger) Context.call(tokenA, "decimals");
        BigInteger BTokenDecimals = tokenB.equals(NATIVE_COIN) ? BigInteger.valueOf(18) : (BigInteger) Context.call(tokenB, "decimals");

        BigInteger price, decimals;
        switch (ATokenDecimals.compareTo(BTokenDecimals)) {
            case -1:
                price = BTokenTotal.divide(ATokenTotal.multiply(_pow(BigInteger.TEN, BTokenDecimals.subtract(ATokenDecimals).intValue())));
                decimals = BTokenDecimals;
                break;
            case 1:
                price = BTokenTotal.multiply(_pow(BigInteger.TEN, ATokenDecimals.subtract(BTokenDecimals).intValue())).divide(ATokenTotal);
                decimals = ATokenDecimals;
                break;
            default:
                price = BTokenTotal.divide(ATokenTotal);
                decimals = ATokenDecimals;
                break;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("price", price);
        map.put("decimals", decimals);
        return map;
    }

    protected BigInteger _pow(BigInteger base, int exponent) {
        BigInteger res = BigInteger.ONE;
        for (int i = 1; i <= exponent; i++) {
            res = res.multiply(base);
        }
        return res;
    }

    @External(readonly = true)
    public Map<String, Object> getBasePriceInQuote() {
        Context.require(lpToken.get() != null, "Pool not yet initialized");

        return _priceOfAInB(baseToken(), quoteToken());
    }

    @External(readonly = true)
    public Map<String, Object> getQuotePriceInBase() {
        Context.require(lpToken.get() != null, "Pool not yet initialized");

        return _priceOfAInB(quoteToken(), baseToken());
    }

    @EventLog
    public void LiquidityAdded(Address _provider, BigInteger _value, BigInteger _base, BigInteger _qoute) {}

    @EventLog
    public void LiquidityRemoved(Address _provider, BigInteger _value, BigInteger _base, BigInteger _qoute) {}

    @EventLog
    public void Swap(Address _fromToken, Address _toToken, Address _sender, Address _receiver,
                     BigInteger _fromValue, BigInteger _toValue, BigInteger _lpFees) {}
}
