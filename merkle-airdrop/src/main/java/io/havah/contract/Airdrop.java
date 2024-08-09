package io.havah.contract;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;
import score.annotation.Optional;

import java.math.BigInteger;

public class Airdrop {
    int id;
    Address token;
    byte[] merkleRoot;
    long startTime;
    long endTime;
    BigInteger totalAmount;

    private Airdrop() {}

    public Airdrop(int id, Address token, byte[] merkleRoot, long startTime, @Optional long endTime, @Optional BigInteger totalAmount) {
        this.id = id;
        this.token = token;
        this.merkleRoot = merkleRoot;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;
    }

    public static void writeObject(ObjectWriter w, Airdrop s) {
        w.beginList(6);
        w.write(s.id);
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
                r.readInt(),
                r.readAddress(),
                r.readByteArray(),
                r.readLong(),
                r.readLong(),
                r.readNullable(BigInteger.class));
        r.end();
        return s;
    }
}
