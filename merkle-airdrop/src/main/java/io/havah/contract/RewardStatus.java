package io.havah.contract;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class RewardStatus {
    BigInteger total;
    BigInteger claimed;
    BigInteger remained;

    private RewardStatus() {
    }

    public RewardStatus(BigInteger total, BigInteger claimed, BigInteger remained) {
        this.total = total;
        this.claimed = claimed;
        this.remained = remained;
    }

    public static void writeObject(ObjectWriter w, RewardStatus r) {
        w.beginList(3);
        w.write(r.total);
        w.write(r.claimed);
        w.write(r.remained);
        w.end();
    }

    public static RewardStatus readObject(ObjectReader r) {
        r.beginList();
        RewardStatus s = new RewardStatus(
                r.readBigInteger(),
                r.readBigInteger(),
                r.readBigInteger());
        r.end();
        return s;
    }

}
