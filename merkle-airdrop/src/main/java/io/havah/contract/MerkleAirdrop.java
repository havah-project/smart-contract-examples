package io.havah.contract;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

public class MerkleAirdrop {
    static final VarDB<String> name = Context.newVarDB("name", String.class);
    static final Address ZERO_ADDRESS = Address.fromString("hx0000000000000000000000000000000000000000");
    final VarDB<Address> token = Context.newVarDB("token", Address.class);
    final VarDB<byte[]> merkleRoot = Context.newVarDB("merkleRoot", byte[].class);
    final VarDB<Long> startTime = Context.newVarDB("startTime", Long.class);
    final VarDB<Long> endTime = Context.newVarDB("endTime", Long.class);
    final VarDB<BigInteger> totalAmount = Context.newVarDB("totalAmount", BigInteger.class);
    final DictDB<Address, Boolean> claimed = Context.newDictDB("claimed", Boolean.class);
    final VarDB<BigInteger> totalClaimed = Context.newVarDB("total_claimed", BigInteger.class);

    protected boolean _isCaller(Address address) {
        return Context.getCaller().equals(address);
    }

    protected void _onlyOwner() {
        Context.require(_isCaller(Context.getOwner()), "Only owner can call this method");
    }

    protected void _checkTokenAddress(Address address) {
        Context.require(address.equals(ZERO_ADDRESS) || address.isContract(), "Not token address");
    }

    protected void _checkNotEmpty(byte[] hash) {
        Context.require(hash != null && hash.length > 0, "Empty hash");
    }

    protected void _checkTime(long start, long end) {
        Context.require(end == 0 || end > start, "Invalid time");
    }

    protected void _checkAmount(BigInteger amount) {
        Context.require(amount == null || amount.signum() > 0, "Invalid amount");
    }

    protected void _requireNotRegisterMerkleRoot() {
        Context.require(merkleRoot.get() == null, "Merkle root was registered");
    }

    protected void _requireRegisterMerkleRoot() {
        Context.require(merkleRoot.get() != null, "Merkle root was not registered");
    }

    protected void _requireNotStartAirdrop() {
        Context.require(Context.getBlockTimestamp() < startTime.get(), "Already started");
    }

    protected void _requireStartAirdrop() {
        long time = Context.getBlockTimestamp();
        Context.require(time >= startTime.get(), "Not started");
        if(endTime.get() > 0)
            Context.require( time < endTime.get(), "Not started");
    }

    protected void _checkNotClaimed(Address caller) {
        Context.require(!claimed.getOrDefault(caller, false), "Already claimed");
    }

    protected boolean _verifyProof(byte[] merkleRoot, Address caller, BigInteger amount, byte[][] proof) {
        byte[] hash = _makeHash(caller.toString().getBytes(), amount.toString().getBytes());
        for(byte[] leaf : proof) {
            if(_compare(hash, leaf) <= 0) {
                hash = _makeHash(hash, leaf);
            } else {
                hash = _makeHash(leaf, hash);
            }
        }

        return _compare(hash, merkleRoot) == 0;
    }

    protected String _getSafeString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public int _compare(byte[] a, byte[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int count = Math.min(a.length, b.length);
        for(int i=0; i<count; i++) {
            int cmp = Integer.compare(0xff & a[i], 0xff &b[i]);
            if(cmp != 0) {
                return cmp;
            }
        }

        return a.length - b.length;
    }

    byte[] _concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    protected byte[] _makeHash(byte[] a, byte[] b) {
        return Context.hash("keccak-256", _concat(a, b));
    }

    protected void _transfer(Address _token, Address _recipient, BigInteger _amount) {
        if(_token.equals(ZERO_ADDRESS)) {
            Context.transfer(_recipient, _amount);
        } else {
            Context.call(_token, "transfer", _recipient, _amount);
        }
    }

    public MerkleAirdrop(String _name) {
        name.set(_name);
    }

    @External(readonly = true)
    public String name() {
        return name.get();
    }

    @Payable
    public void fallback() {
        BigInteger value = Context.getValue();
        if (value.signum() > 0) {
            Deposited(Context.getCaller(), value);
        }
    }

    @External
    public void registerMerkleRoot(Address _token, byte[] _merkleRoot, long _startTime, @Optional long _endTime, @Optional BigInteger _totalAmount) {
        _onlyOwner();
        _requireNotRegisterMerkleRoot();
        _checkTokenAddress(_token);
        _checkNotEmpty(_merkleRoot);
        _checkTime(_startTime, _endTime);
        _checkAmount(_totalAmount);

        token.set(_token);
        merkleRoot.set(_merkleRoot);
        startTime.set(_startTime);
        endTime.set(_endTime);
        if(_totalAmount != null)
            totalAmount.set(_totalAmount);

        MerkleRootRegistered(_token, _merkleRoot, _startTime, _endTime, _getSafeString(_totalAmount));
    }

    @External
    public void claim(BigInteger _amount, byte[][] _proof) {
        Address caller = Context.getCaller();
        _checkNotClaimed(caller);
        _requireStartAirdrop();
        Context.require(_verifyProof(merkleRoot.get(), caller, _amount, _proof), "Invalid proof");

        claimed.set(caller, true);
        totalClaimed.set(totalClaimed.getOrDefault(BigInteger.ZERO).add(_amount));
        _transfer(token.get(), caller, _amount);
        Claimed(token.get(), caller, _amount);
    }

    @External
    public void withdraw(Address _token, BigInteger _amount, @Optional Address _recipient) {
        _onlyOwner();
        _recipient = _recipient == null ? Context.getCaller() : _recipient;
        _transfer(_token, _recipient, _amount);
        Withdrawn(_token, _recipient, _amount);
    }

    @External(readonly = true)
    public byte[] merkleRoot() {
        return merkleRoot.get();
    }

    @External(readonly = true)
    public Map info() {
        Address airdropToken = token.get();
        if(airdropToken != null) {
            long time = Context.getBlockTimestamp();
            long start = startTime.get();
            long end = endTime.get();
            return Map.of(
                    "token", airdropToken,
                    "start", start,
                    "end", end,
                    "total", totalAmount.getOrDefault(BigInteger.ZERO),
                    "claimed", totalClaimed.getOrDefault(BigInteger.ZERO),
                    "remain", totalAmount.get() != null ? totalAmount.get().subtract(totalClaimed.getOrDefault(BigInteger.ZERO)) : BigInteger.ZERO,
                    "opened", start <= time && (end == 0 || end > time)
            );
        }
        return Map.of();
    }

    @External(readonly = true)
    public boolean isClaimed(Address _address) {
        return claimed.getOrDefault(_address, false);
    }

    @External(readonly = true)
    public boolean isClaimable(Address _address, BigInteger _amount, byte[][] _proof) {
        byte[] root = merkleRoot.get();
        if(root != null) {
            long time = Context.getBlockTimestamp();
            if (startTime.get() <= time && (endTime.get() == 0 || endTime.get() > time))
                return _verifyProof(merkleRoot.get(), _address, _amount, _proof);
        }
        return false;
    }

    @EventLog
    public void Deposited(Address _sender,  BigInteger _amount) {}

    @EventLog
    public void MerkleRootRegistered(Address _token, byte[] _merkleRoot, long _startTime, long _endTime, String _totalAmount) {}

    @EventLog
    public void Claimed(Address _token, Address _recipient, BigInteger _amount) {}

    @EventLog
    public void Withdrawn(Address _token, Address _recipient, BigInteger _amount) {}
}
