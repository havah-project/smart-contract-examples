package io.havah.contract.test.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static foundation.icon.test.Env.LOG;

public class LiquidityPoolScore extends Score {

    private static final Address SYSTEM_ADDRESS = new Address("cx0000000000000000000000000000000000000000");

    protected static TransactionResult.EventLog findEventLog(TransactionResult result, Address scoreAddress, String funcSig) {
        List<TransactionResult.EventLog> eventLogs = result.getEventLogs();
        for (TransactionResult.EventLog event : eventLogs) {
            if (event.getScoreAddress().equals(scoreAddress.toString())) {
                String signature = event.getIndexed().get(0).asString();
                if (funcSig.equals(signature)) {
                    return event;
                }
            }
        }
        return null;
    }

    public void ensureLiquidityAdded(TransactionResult result, BigInteger base, BigInteger qoute)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "LiquidityAdded(Address,int,int,int)");
        if (event != null) {
            BigInteger _base = event.getData().get(2).asInteger();
            BigInteger _qoute = event.getData().get(3).asInteger();

            if (base.equals(_base) && qoute.equals(_qoute)) {
                return; // ensured
            }
        }
        throw new IOException("ensureLiquidityAdded failed.");
    }

    public void ensureLiquidityRemoved(TransactionResult result, BigInteger value)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "LiquidityRemoved(Address,int,int,int)");
        if (event != null) {
            BigInteger _value = event.getData().get(1).asInteger();

            if (value.equals(_value)) {
                return; // ensured
            }
        }
        throw new IOException("ensureLiquidityRemoved failed.");
    }

    public void ensureSwap(TransactionResult result, Address from, Address to, BigInteger value)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "Swap(Address,Address,Address,Address,int,int,int)");
        if (event != null) {
            Address _from = event.getData().get(0).asAddress();
            Address _to = event.getData().get(1).asAddress();
            BigInteger _value = event.getData().get(4).asInteger();

            if (from.equals(_from) && to.equals(_to) && value.equals(_value)) {
                return; // ensured
            }
        }
        throw new IOException("ensureSwap failed.");
    }

    public LiquidityPoolScore(Score other) {
        super(other);
    }

    public static LiquidityPoolScore mustDeploy(TransactionHandler txHandler, Wallet owner)
            throws TransactionFailureException, IOException, ResultTimeoutException {
        LOG.infoEntering("deploy", "LiquidityPool");

        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue("liquidity-pool"))
                .build();

        Score score = txHandler.deploy(owner, getFilePath("liquidity-pool"), params);
        LOG.infoExiting();
        return new LiquidityPoolScore(score);
    }

    public TransactionResult initialize(Wallet wallet, Address baseToken, Address quoteToken, Address lpToken)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_baseToken", new RpcValue(baseToken))
                .put("_quoteToken", new RpcValue(quoteToken))
                .put("_lpToken", new RpcValue(lpToken))
                .build();

        return invokeAndWaitResult(wallet, "initialize", params, BigInteger.valueOf(1000000));
    }

    public TransactionResult add(Wallet wallet, BigInteger value, BigInteger baseValue, BigInteger quoteValue)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_baseValue", new RpcValue(baseValue))
                .put("_quoteValue", new RpcValue(quoteValue))
                .build();

        return invokeAndWaitResult(wallet, "add", params, value, BigInteger.valueOf(10000000));
    }

    public TransactionResult remove(Wallet wallet, BigInteger lpAmount)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_lpAmount", new RpcValue(lpAmount))
                .build();

        return invokeAndWaitResult(wallet, "remove", params, BigInteger.valueOf(10000000));
    }

    public TransactionResult swap(Wallet wallet, Address fromToken, Address toToken, BigInteger value, Address receiver, BigInteger minimumReceive)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_receiver", new RpcValue(receiver))
                .put("_fromToken", new RpcValue(fromToken))
                .put("_toToken", new RpcValue(toToken))
                .put("_value", new RpcValue(value))
                .put("_minimumReceive", new RpcValue(minimumReceive))
                .build();

        return invokeAndWaitResult(wallet, "swap", params, fromToken.equals(SYSTEM_ADDRESS) ? value : BigInteger.ZERO, BigInteger.valueOf(10000000));
    }

    public TransactionResult setFee(Wallet wallet, BigInteger fee)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_fee", new RpcValue(fee))
                .build();

        return invokeAndWaitResult(wallet, "setFee", params, null);
    }

    public BigInteger fee() throws IOException {
        return call("fee", null).asInteger();
    }

    public BigInteger feeScale() throws IOException {
        return call("feeScale", null).asInteger();
    }

    public Address baseToken() throws IOException {
        RpcItem item = call("baseToken", null);
        return item == null ? null : item.asAddress();
    }

    public Address quoteToken() throws IOException {
        return call("quoteToken", null).asAddress();
    }

    public Address lpToken() throws IOException {
        return call("lpToken", null).asAddress();
    }

    public Map<String, Object> getBasePriceInQuote() throws Exception {
        RpcObject item = (RpcObject) call("getBasePriceInQuote", null);
        Map<String, Object> map = new HashMap<>();
        if (!item.isEmpty()) {
            map.put("price", item.getItem("price").asInteger());
            map.put("decimals", item.getItem("decimals").asInteger());
        }
        return map;
    }

    public Map<String, Object> getQuotePriceInBase() throws Exception {
        RpcObject item = (RpcObject) call("getQuotePriceInBase", null);
        Map<String, Object> map = new HashMap<>();
        if (!item.isEmpty()) {
            map.put("price", item.getItem("price").asInteger());
            map.put("decimals", item.getItem("decimals").asInteger());
        }
        return map;
    }
}
