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

    public TransactionResult addAirdrop(Wallet wallet, Address token, byte[] merkleRoot, BigInteger startTime, BigInteger endTime, BigInteger totalAmount)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_merkleRoot", new RpcValue(merkleRoot))
                .put("_startTime", new RpcValue(startTime));
        if(endTime != null) params.put("_endTime", new RpcValue(endTime));
        if(totalAmount != null) params.put("_totalAmount", new RpcValue(totalAmount));

        return invokeAndWaitResult(wallet, "addAirdrop", params.build());
    }

    public TransactionResult updateAirdrop(Wallet wallet, BigInteger id, BigInteger startTime, BigInteger endTime, BigInteger totalAmount)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_startTime", new RpcValue(startTime));
        if(endTime != null) params.put("_endTime", new RpcValue(endTime));
        if(totalAmount != null) params.put("_totalAmount", new RpcValue(totalAmount));

        return invokeAndWaitResult(wallet, "updateAirdrop", params.build());
    }

    public TransactionResult claim(Wallet wallet, BigInteger id, BigInteger amount, byte[][] proof)
            throws IOException, ResultTimeoutException {
        RpcArray.Builder array = new RpcArray.Builder();
        for(byte[] leaf : proof) {
            array.add(new RpcValue(leaf));
        }

        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_amount", new RpcValue(amount))
                .put("_proof", array.build())
                .build();

        return invokeAndWaitResult(wallet, "claim", params);
    }

    public TransactionResult giveaway(Wallet wallet, BigInteger id, Address recipient, BigInteger amount, byte[][] proof)
            throws IOException, ResultTimeoutException {
        RpcArray.Builder array = new RpcArray.Builder();
        for(byte[] leaf : proof) {
            array.add(new RpcValue(leaf));
        }

        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_recipient", new RpcValue(recipient))
                .put("_amount", new RpcValue(amount))
                .put("_proof", array.build())
                .build();

        return invokeAndWaitResult(wallet, "giveaway", params);
    }

    public TransactionResult withdraw(Wallet wallet, Address token, BigInteger amount, Address recipient)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder params = new RpcObject.Builder()
                .put("_token", new RpcValue(token))
                .put("_amount", new RpcValue(amount));
        if(recipient != null) params.put("_recipient", new RpcValue(recipient));

        return invokeAndWaitResult(wallet, "withdraw", params.build());
    }

    public BigInteger lastId() throws IOException {
        return call("lastId", null).asInteger();
    }

    public byte[] merkleRoot(BigInteger id) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .build();
        return call("merkleRoot", params).asByteArray();
    }

    public Map info(BigInteger id) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .build();
        RpcObject obj = call("info", params).asObject();
        return Map.of(
                "id", obj.getItem("id").asInteger(),
                "token", obj.getItem("token").asAddress(),
                "start_time", obj.getItem("start_time").asInteger(),
                "end_time", obj.getItem("end_time").asInteger(),
                "total_amount", obj.getItem("total_amount").asInteger(),
                "claimed_amount", obj.getItem("claimed_amount").asInteger(),
                "left_amount", obj.getItem("left_amount").asInteger()
        );
    }

    public boolean isClaimed(BigInteger id, Address address) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_address", new RpcValue(address))
                .build();
        return call("isClaimed", params).asBoolean();
    }

    public boolean isClaimable(BigInteger id, Address address, BigInteger amount, byte[][] proof) throws IOException {
        RpcArray.Builder array = new RpcArray.Builder();
        for(byte[] leaf : proof) {
            array.add(new RpcValue(leaf));
        }

        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(id))
                .put("_address", new RpcValue(address))
                .put("_amount", new RpcValue(amount))
                .put("_proof", array.build())
                .build();
        return call("isClaimable", params).asBoolean();
    }

    public boolean isValidProof(byte[] merkleRoot, byte[] hash, byte[][] _proof)
            throws IOException {
        RpcArray.Builder array = new RpcArray.Builder();
        for(byte[] leaf : _proof) {
            array.add(new RpcValue(leaf));
        }

        RpcObject params = new RpcObject.Builder()
                .put("_merkleRoot", new RpcValue(merkleRoot))
                .put("_hash", new RpcValue(hash))
                .put("_proof", array.build())
                .build();

        return call("isValidProof", params).asBoolean();
    }
}
