package io.havah.contract;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

public class MerkleAirdrop {
    protected static final Address ZERO_ADDRESS = Address.fromString("hx0000000000000000000000000000000000000000");
    protected static final VarDB<String> name = Context.newVarDB("name", String.class);
    protected static final VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    protected static final DictDB<Integer, Airdrop> airdrops = Context.newDictDB("airdrops", Airdrop.class);
    protected static final VarDB<Integer> lastId = Context.newVarDB("last_id", Integer.class);
    protected static final BranchDB<Integer, DictDB<Address, Boolean>> claimed = Context.newBranchDB("claimed", Boolean.class);
    protected static final DictDB<Integer, BigInteger> totalClaimed = Context.newDictDB("total_claimed", BigInteger.class);

    protected boolean _isCaller(Address address) {
        return Context.getCaller().equals(address);
    }

    protected void _require(boolean condition, String err) {
        if (!condition) {
            Context.revert(err);
        }
    }

    @External
    public void setAdmin(Address _admin) {
        _onlyAdmin();
        admin.set(_admin);
    }

    @External(readonly = true)
    public Address admin() {
        return admin.getOrDefault(Context.getOwner());
    }

    @External(readonly = true)
    public String name() {
        return name.get();
    }

    protected void _onlyAdmin() {
        _require(_isCaller(admin()), "Only administrator can call this method");
    }

    protected void _checkContract(Address address) {
        _require(address.equals(ZERO_ADDRESS) || address.isContract(), "Not contract address");
    }

    protected void _checkNotEmpty(byte[] hash) {
        _require(hash != null && hash.length > 0, "Empty hash");
    }

    protected void _checkTime(long start, long end) {
        _require(end == 0 || end > start, "Invalid time");
    }

    protected void _checkAmount(BigInteger amount) {
        _require(amount == null || amount.signum() > 0, "Invalid amount");
    }

    protected void _checkExistId(int id) {
        _require(id > -1 && id <= lastId.get(), "Invalid id");
    }

    protected void _checkNotStarted(Airdrop airdrop) {
        _require(Context.getBlockTimestamp() < airdrop.startTime, "Already started airdrop");
    }

    protected void _checkOpenAirdrop(Airdrop airdrop) {
        long time = Context.getBlockTimestamp();
        _require(time >= airdrop.startTime, "Not open airdrop");
        if(airdrop.endTime > 0)
            _require( time < airdrop.endTime, "Not open airdrop");
    }

    protected void _checkNotClaimed(int id, Address caller) {
        _require(!claimed.at(id).getOrDefault(caller, false), "Already claimed");
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

    protected void _claim(int _id, Address _recipient, BigInteger _amount, byte[][] _proof) {
        _checkExistId(_id);
        _checkNotClaimed(_id, _recipient);
        Airdrop airdrop = airdrops.get(_id);
        _checkOpenAirdrop(airdrop);
        _require(_verifyProof(airdrop.merkleRoot, _recipient, _amount, _proof), "Invalid proof");

        claimed.at(_id).set(_recipient, true);
        totalClaimed.set(_id, totalClaimed.getOrDefault(_id, BigInteger.ZERO).add(_amount));
        _transfer(airdrop.token, _recipient, _amount);
        Claimed(_id, airdrop.token, _recipient, _amount);
    }

    public MerkleAirdrop(String _name) {
        name.set(_name);
    }

    @Payable
    public void fallback() {
        BigInteger value = Context.getValue();
        if (value.signum() > 0) {
            Deposited(Context.getCaller(), value);
        }
    }

    @External
    public void addAirdrop(Address _token, byte[] _merkleRoot, long _startTime, @Optional long _endTime, @Optional BigInteger _totalAmount) {
        _onlyAdmin();
        _checkContract(_token);
        _checkNotEmpty(_merkleRoot);
        _checkTime(_startTime, _endTime);
        _checkAmount(_totalAmount);

        int id = lastId() + 1;
        airdrops.set(id, new Airdrop(id, _token, _merkleRoot, _startTime, _endTime, _totalAmount));
        lastId.set(id);

        AirdropAdded(id, _token, _merkleRoot, _startTime, _endTime, _getSafeString(_totalAmount));
    }

    @External
    public void updateAirdrop(int _id, long _startTime, @Optional long _endTime, @Optional BigInteger _totalAmount) {
        _onlyAdmin();
        _checkExistId(_id);
        _checkTime(_startTime, _endTime);
        _checkAmount(_totalAmount);

        Airdrop airdrop = airdrops.get(_id);
        _checkNotStarted(airdrop);

        airdrop.startTime = _startTime;
        airdrop.endTime = _endTime;
        airdrop.totalAmount = _totalAmount;

        airdrops.set(_id, airdrop);
        AirdropUpdated(_id, _startTime, _endTime, _getSafeString(_totalAmount));
    }

    @External
    public void claim(int _id, BigInteger _amount, byte[][] _proof) {
        _claim(_id, Context.getCaller(), _amount, _proof);
    }

    @External
    public void giveaway(int _id, Address _recipient, BigInteger _amount, byte[][] _proof) {
        _claim(_id, _recipient, _amount, _proof);
    }

    @External
    public void withdraw(Address _token, BigInteger _amount, @Optional Address _recipient) {
        _onlyAdmin();
        _recipient = _recipient == null ? admin() : _recipient;
        _transfer(_token, _recipient, _amount);
        Withdrawn(_token, _recipient, _amount);
    }

    @External(readonly = true)
    public int lastId() {
        return lastId.getOrDefault(-1);
    }

    @External(readonly = true)
    public byte[] merkleRoot(int _id) {
        Airdrop airdrop = airdrops.get(_id);
        if(airdrop != null) {
            return airdrop.merkleRoot;
        }
        return null;
    }

    @External(readonly = true)
    public Map info(int _id) {
        Airdrop airdrop = airdrops.get(_id);
        if(airdrop != null) {
            long time = Context.getBlockTimestamp();
            return Map.of(
                    "id", _id,
                    "token", airdrop.token,
                    "start", airdrop.startTime,
                    "end", airdrop.endTime,
                    "total", airdrop.totalAmount,
                    "claimed", totalClaimed.getOrDefault(_id, BigInteger.ZERO),
                    "remain", airdrop.totalAmount != null ? airdrop.totalAmount.subtract(totalClaimed.getOrDefault(_id, BigInteger.ZERO)) : BigInteger.ZERO
            );
        }
        return Map.of();
    }

    @External(readonly = true)
    public boolean isClaimed(int _id, Address _address) {
        return claimed.at(_id).getOrDefault(_address, false);
    }

    @External(readonly = true)
    public boolean isClaimable(int _id, Address _address, BigInteger _amount, byte[][] _proof) {
        Airdrop airdrop = airdrops.get(_id);
        if(airdrop != null) {
            long time = Context.getBlockTimestamp();
            if(airdrop.startTime <= time && (airdrop.endTime == 0 || airdrop.endTime > time))
                return _verifyProof(airdrop.merkleRoot, _address, _amount, _proof);
        }
        return false;
    }

    @External(readonly = true)
    public boolean isValidProof(byte[] _merkleRoot, byte[] _hash, byte[][] _proof) {
        byte[] hash = _hash;
        for(byte[] leaf : _proof) {
            if(_compare(hash, leaf) <= 0) {
                hash = _makeHash(hash, leaf);
            } else {
                hash = _makeHash(leaf, hash);
            }
        }

        return _compare(hash, _merkleRoot) == 0;
    }

    @EventLog
    public void Deposited(Address _sender,  BigInteger _amount) {}

    @EventLog
    public void AirdropAdded(int _id, Address _token, byte[] _merkleRoot, long _startTime, long _endTime, String _totalAmount) {}

    @EventLog
    public void AirdropUpdated(int _id, long _startTime, long _endTime, String _totalAmount) {}

    @EventLog
    public void Claimed(int _id, Address _token, Address _recipient, BigInteger _amount) {}

    @EventLog
    public void Withdrawn(Address _token, Address _recipient, BigInteger _amount) {}
}
