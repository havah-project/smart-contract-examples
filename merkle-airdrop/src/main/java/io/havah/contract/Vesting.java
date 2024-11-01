package io.havah.contract;

import score.Address;
import score.Context;

import java.math.BigInteger;

public class Vesting {
    private Address contractAddr;
    public Vesting(Address address) {
        contractAddr = address;
    }

    public void addVestingAccount(Integer id, Address address, BigInteger amount) {
        Context.call(contractAddr, "addVestingAccount", id, address, amount);
    }

    public void claim(int id, Address claimer) {
        Context.call(contractAddr, "claim", id, claimer);
    }

    public BigInteger claimable(int id, Address claimer) {
        return (BigInteger) Context.call(contractAddr, "claimableAmount", id, claimer);
    }
}
