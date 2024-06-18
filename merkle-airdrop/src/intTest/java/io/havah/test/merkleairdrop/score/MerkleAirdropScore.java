package io.havah.test.merkleairdrop.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcArray;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static foundation.icon.test.Env.LOG;

public class MerkleAirdropScore extends Score {
    public MerkleAirdropScore(Score other) {
        super(other);
    }

    public static MerkleAirdropScore mustDeploy(TransactionHandler txHandler, Wallet wallet)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "MerkleAirdrop");

        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue("MerkleAirdrop"))
                .build();

        Score score = txHandler.deploy(wallet, getFilePath("merkle-airdrop"), params);
        LOG.infoExiting("scoreAddr = " + score.getAddress());
        return new MerkleAirdropScore(score);
    }

    public TransactionResult registerMerkleRoot(Wallet wallet, Address token, byte[] merkleRoot, BigInteger startTime, BigInteger endTime, BigInteger totalAmount)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_merkleRoot", new RpcValue(merkleRoot))
                .put("_startTime", new RpcValue(startTime));
        if(endTime != null) params.put("_endTime", new RpcValue(endTime));
        if(totalAmount != null) params.put("_totalAmount", new RpcValue(totalAmount));

        return invokeAndWaitResult(wallet, "registerMerkleRoot", params.build());
    }

    public TransactionResult claim(Wallet wallet,BigInteger amount, byte[][] proof)
            throws IOException, ResultTimeoutException {
        RpcArray.Builder array = new RpcArray.Builder();
        for(byte[] leaf : proof) {
            array.add(new RpcValue(leaf));
        }

        RpcObject params = new RpcObject.Builder()
                .put("_amount", new RpcValue(amount))
                .put("_proof", array.build()).build();

        return invokeAndWaitResult(wallet, "claim", params);
    }

    public TransactionResult withdraw(Wallet wallet, Address token, BigInteger amount, Address recipient)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_amount", new RpcValue(amount));
        if(recipient != null) params.put("_recipient", new RpcValue(recipient));

        return invokeAndWaitResult(wallet, "withdraw", params.build());
    }

    public byte[] merkleRoot() throws IOException {
        return call("merkleRoot", null).asByteArray();
    }

    public Map info() throws IOException {
        RpcObject obj = call("info", null).asObject();
        return Map.of(
                "token", obj.getItem("token").asAddress(),
                "start", obj.getItem("start").asInteger(),
                "end", obj.getItem("end").asInteger(),
                "total", obj.getItem("total").asInteger(),
                "claimed", obj.getItem("claimed").asInteger(),
                "remain", obj.getItem("remain").asInteger(),
                "opened", obj.getItem("opened").asBoolean()
        );
    }

    public boolean isClaimed(Address address) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_address", new RpcValue(address))
                .build();
        return call("isClaimed", params).asBoolean();
    }

    public boolean isClaimable(Address address, BigInteger amount, byte[][] proof) throws IOException {
        RpcArray.Builder array = new RpcArray.Builder();
        for(byte[] leaf : proof) {
            array.add(new RpcValue(leaf));
        }

        RpcObject params = new RpcObject.Builder()
                .put("_address", new RpcValue(address))
                .put("_amount", new RpcValue(amount))
                .put("_proof", array.build())
                .build();
        return call("isClaimable", params).asBoolean();
    }
}
