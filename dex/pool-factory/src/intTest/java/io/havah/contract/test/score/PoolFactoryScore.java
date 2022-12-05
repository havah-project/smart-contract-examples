package io.havah.contract.test.score;

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

public class PoolFactoryScore extends Score {
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

    public Address ensurePoolCreated(TransactionResult result, Address base, Address quote)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "PoolCreated(Address,Address,Address)");
        if (event != null) {
            Address _base = event.getIndexed().get(1).asAddress();
            Address _quote = event.getIndexed().get(2).asAddress();
            Address _pool = event.getIndexed().get(3).asAddress();

            if ((base.equals(_base) || base.equals(_quote)) && (quote.equals(_quote) || quote.equals(_base))) {
                return _pool; // ensured
            }
        }
        throw new IOException("ensurePoolCreated failed.");
    }

    public PoolFactoryScore(Score other) {
        super(other);
    }

    public static PoolFactoryScore mustDeploy(TransactionHandler txHandler, Wallet owner)
            throws TransactionFailureException, IOException, ResultTimeoutException {
        LOG.infoEntering("deploy", "PoolFactory");
        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue("pool-factory"))
                .build();
        Score score = txHandler.deploy(owner, getFilePath("pool-factory"), params);
        LOG.infoExiting();
        return new PoolFactoryScore(score);
    }

    public TransactionResult setLpTokenContract(Wallet wallet, byte[] _contract)
            throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_contract", new RpcValue(_contract))
                .build();

        return invokeAndWaitResult(wallet, "setLpTokenContract", params, BigInteger.valueOf(10000000));
    }

    public TransactionResult setPoolContract(Wallet wallet, byte[] _contract) throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_contract", new RpcValue(_contract))
                .build();

        return invokeAndWaitResult(wallet, "setPoolContract", params, BigInteger.valueOf(10000000));
    }

    public Address findPool(Address base, Address quote) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_base", new RpcValue(base))
                .put("_quote", new RpcValue(quote))
                .build();
        return call("findPool", params).asAddress();
    }

    public TransactionResult createPool(Wallet wallet, Address base, Address quote) throws ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_base", new RpcValue(base))
                .put("_quote", new RpcValue(quote))
                .build();

        return invokeAndWaitResult(wallet, "createPool", params, new BigInteger("10000000000"));
    }
}
