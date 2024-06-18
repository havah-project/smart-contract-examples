package io.havah.test.merkleairdrop.score;

import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class LogFinder {
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

    public static void ensureDeposited(TransactionResult result, Address score, Address address, BigInteger value)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, score, "Deposited(Address,int)");
        if (event != null) {
            Address _address = event.getData().get(0).asAddress();
            BigInteger _value = event.getData().get(1).asInteger();

            if (address.equals(_address) && value.equals(_value)) {
                return; // ensured
            }
        }
        throw new IOException("ensureDeposited failed.");
    }

    public static void ensureMerkleRootRegistered(TransactionResult result, Address score, Address token,
                                                  byte[] merkleRoot, BigInteger startTime, BigInteger endTime, String totalAmount)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, score, "MerkleRootRegistered(Address,bytes,int,int,str)");
        if (event != null) {
            Address _token = event.getData().get(0).asAddress();
            byte[] _merkleRoot = event.getData().get(1).asByteArray();
            BigInteger _startTime = event.getData().get(2).asInteger();
            BigInteger _endTime = event.getData().get(3).asInteger();
            String _totalAmount = event.getData().get(4).asString();

            if (token.equals(_token) && Arrays.equals(merkleRoot, _merkleRoot) && startTime.equals(_startTime)
                    && endTime.equals(_endTime) && totalAmount.equals(_totalAmount)) {
                return; // ensured
            }
        }
        throw new IOException("ensureMerkleRootRegistered failed.");
    }

    public static void ensureClaimed(TransactionResult result, Address score, Address token, Address recipient,
                                     BigInteger amount) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, score, "Claimed(Address,Address,int)");
        if (event != null) {
            Address _token = event.getData().get(0).asAddress();
            Address _recipient = event.getData().get(1).asAddress();
            BigInteger _amount = event.getData().get(2).asInteger();

            if (token.equals(_token) && recipient.equals(_recipient) && amount.equals(_amount)) {
                return; // ensured
            }
        }
        throw new IOException("ensureClaimed failed.");
    }

    public static void ensureWithdrawn(TransactionResult result, Address score, Address token,
                                      Address recipient, BigInteger amount) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, score, "Withdrawn(Address,Address,int)");
        if (event != null) {
            Address _token = event.getData().get(0).asAddress();
            Address _recipient = event.getData().get(1).asAddress();
            BigInteger _amount = event.getData().get(2).asInteger();

            if (token.equals(_token) && amount.equals(_amount) && recipient.equals(_recipient)) {
                return; // ensured
            }
        }
        throw new IOException("ensureWithdrawn failed.");
    }
}
