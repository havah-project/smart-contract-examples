package io.havah.contract;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.io.Serializable;
import java.math.BigInteger;

public class AccountInfo implements Serializable {
    private Address address;
    private BigInteger totalAmount;

    // 없을 경우 컴파일 에러발생.
    public AccountInfo() {

    }

    public AccountInfo(Address address, BigInteger amount) {
        this.address = address;
        this.totalAmount = amount;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public BigInteger getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigInteger totalAmount) {
        this.totalAmount = totalAmount;
    }

    public static void writeObject(ObjectWriter w, AccountInfo a) {
        w.beginList(2);
        w.write(a.address);
        w.write(a.totalAmount);
        w.end();
    }

    public static AccountInfo readObject(ObjectReader r) {
        r.beginList();
        AccountInfo a = new AccountInfo(r.readAddress(), r.readBigInteger());
        r.end();
        return a;
    }
}
