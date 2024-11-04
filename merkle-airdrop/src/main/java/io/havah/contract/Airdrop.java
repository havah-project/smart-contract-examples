package io.havah.contract;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;
import score.annotation.Optional;

import java.math.BigInteger;

public class Airdrop {
    private Address token;
    private byte[] merkleRoot;
    private long startTime;
    private long endTime;
    private BigInteger totalAmount;

    private Airdrop() {
    }

    public Airdrop(Address token, byte[] merkleRoot, long startTime,
                   @Optional long endTime, @Optional BigInteger totalAmount) {
        this.token = token;
        this.merkleRoot = merkleRoot;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;
    }

    public Address getToken() {
        return token;
    }

    public void setToken(Address token) {
        this.token = token;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(byte[] merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public BigInteger getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigInteger totalAmount) {
        this.totalAmount = totalAmount;
    }

    public static void writeObject(ObjectWriter w, Airdrop s) {
        w.beginList(6);
        w.write(s.token);
        w.write(s.merkleRoot);
        w.write(s.startTime);
        w.write(s.endTime);
        w.writeNullable(s.totalAmount);
        w.end();
    }

    public static Airdrop readObject(ObjectReader r) {
        r.beginList();
        Airdrop s = new Airdrop(
                r.readAddress(),
                r.readByteArray(),
                r.readLong(),
                r.readLong(),
                r.readNullable(BigInteger.class));
        r.end();
        return s;
    }
}
