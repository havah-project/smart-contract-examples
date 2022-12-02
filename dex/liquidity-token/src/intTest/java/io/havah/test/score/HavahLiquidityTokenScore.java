package io.havah.test.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static foundation.icon.test.Env.LOG;

public class HavahLiquidityTokenScore extends Score {

    public static final String name = "HavahLiquidityToken";
    public static final String symbol = "HLT";

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

    public void ensureTransfer(TransactionResult result, Address from, Address to, BigInteger value)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "Transfer(Address,Address,int)");
        if (event != null) {
            Address _from = event.getIndexed().get(1).asAddress();
            Address _to = event.getIndexed().get(2).asAddress();
            BigInteger _value = event.getIndexed().get(3).asInteger();

            if (from.equals(_from) && to.equals(_to) && value.equals(_value)) {
                return; // ensured
            }
        }
        throw new IOException("ensureTransfer failed.");
    }

    public void ensureApproval(TransactionResult result, Address owner, Address spender, BigInteger value)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "Approval(Address,Address,int)");
        if (event != null) {
            Address _owner = event.getIndexed().get(1).asAddress();
            Address _spender = event.getIndexed().get(2).asAddress();
            BigInteger _value = event.getIndexed().get(3).asInteger();
            if (_owner.equals(owner) && _spender.equals(spender) && value.equals(_value)) {
                return; // ensured
            }
        }
        throw new IOException("ensureApproval failed.");
    }

    public HavahLiquidityTokenScore(Score other) {
        super(other);
    }

    public static HavahLiquidityTokenScore mustDeploy(TransactionHandler txHandler, Wallet owner)
            throws TransactionFailureException, IOException, ResultTimeoutException {
        LOG.infoEntering("deploy", name);
        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue(name))
                .put("_symbol", new RpcValue(symbol))
                .put("_decimals", new RpcValue(BigInteger.ZERO))
                .build();
        Score score = txHandler.deploy(owner, getFilePath("liquidity-token"), params);
        LOG.infoExiting();
        return new HavahLiquidityTokenScore(score);
    }

    public String name() throws IOException {
        return call("name", null).asString();
    }

    public String symbol() throws IOException {
        return call("symbol", null).asString();
    }

    public BigInteger decimals() throws IOException {
        return call("decimals", null).asInteger();
    }

    public BigInteger totalSupply() throws IOException {
        return call("totalSupply", null).asInteger();
    }

    public BigInteger balanceOf(Address owner) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(owner))
                .build();
        return call("balanceOf", params).asInteger();
    }

    public TransactionResult mint(Wallet wallet, BigInteger value)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_value", new RpcValue(value))
                .build();

        return invokeAndWaitResult(wallet, "mint", params, null);
    }

    public TransactionResult mintTo(Wallet wallet, Address to, BigInteger value)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_to", new RpcValue(to))
                .put("_value", new RpcValue(value))
                .build();

        return invokeAndWaitResult(wallet, "mintTo", params, null);
    }

    public TransactionResult burn(Wallet wallet, BigInteger value)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_value", new RpcValue(value))
                .build();

        return invokeAndWaitResult(wallet, "burn", params, null);
    }

    public TransactionResult approve(Wallet wallet, Address spender, BigInteger value)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_spender", new RpcValue(spender))
                .put("_value", new RpcValue(value))
                .build();

        return invokeAndWaitResult(wallet, "approve", params, null);
    }

    public BigInteger allowance(Address owner, Address spender) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(owner))
                .put("_spender", new RpcValue(spender))
                .build();
        return call("allowance", params).asInteger();
    }

    public TransactionResult transfer(Wallet wallet, Address to, BigInteger value)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_to", new RpcValue(to))
                .put("_value", new RpcValue(value))
                .build();

        return invokeAndWaitResult(wallet, "transfer", params, null);
    }

    public TransactionResult transferFrom(Wallet wallet, Address from, Address to, BigInteger value)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_from", new RpcValue(from))
                .put("_to", new RpcValue(to))
                .put("_value", new RpcValue(value))
                .build();

        return invokeAndWaitResult(wallet, "transferFrom", params, null);
    }

    public TransactionResult setMinter(Wallet wallet, Address minter) throws Exception {
        RpcObject param = new RpcObject.Builder()
                .put("_minter", new RpcValue(minter))
                .build();
        return invokeAndWaitResult(wallet, "setMinter", param, null);
    }

    public Address minter(Wallet wallet) throws Exception {
        return call("minter", null).asAddress();
    }
}
