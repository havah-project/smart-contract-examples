package io.havah.contract;

import score.Address;
import score.Context;

import java.math.BigInteger;

public class Token {
    private Address token;
    public Token(Address address) {
       this.token = address;
    }
    public void transfer(Address to, BigInteger amount) {
        Context.call(this.token, "transfer", to, amount);
    }
}
