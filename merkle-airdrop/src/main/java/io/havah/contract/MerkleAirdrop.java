package io.havah.contract;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

public class MerkleAirdrop {
    private static final int REWARD_OPTION_DEFAULT = 0;
    private static final int REWARD_OPTION_INSTANT_CLAIM = 1;
    private static final int REWARD_OPTION_VESTING = 2;

    protected static final Address ZERO_ADDRESS = Address.fromString("hx0000000000000000000000000000000000000000");
    protected static final VarDB<String> name = Context.newVarDB("name", String.class);
    protected static final VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    protected static final VarDB<Address> treasury = Context.newVarDB("treasury", Address.class);
    protected static final VarDB<Address> vestingContract = Context.newVarDB("vesting_contract", Address.class);
    protected static final VarDB<Airdrop> airdropDb = Context.newVarDB("airdrops", Airdrop.class);
    protected static final VarDB<Integer> vestingId = Context.newVarDB("vesting_id", Integer.class);
    protected static final VarDB<Address> rewardToken = Context.newVarDB("reward_token", Address.class);
    protected static final DictDB<Address, Integer> selectedRewardOption = Context.newDictDB("selected_reward_option", Integer.class);
    protected static final BranchDB<Integer, DictDB<Address, Boolean>> claimed = Context.newBranchDB("claimed", Boolean.class);
    protected static final DictDB<Integer, BigInteger> totalClaimed = Context.newDictDB("total_claimed", BigInteger.class);
    protected static final DictDB<Address, RewardStatus> rewardStatusDict = Context.newDictDB("reward_status", RewardStatus.class);

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

    protected void _checkNotStarted(Airdrop airdrop) {
        _require(Context.getBlockTimestamp() < airdrop.startTime, "Already started airdrop");
    }

    protected boolean _verifyProof(byte[] merkleRoot, Address caller, BigInteger amount, byte[][] proof) {
        byte[] hash = _makeHash(caller.toString().getBytes(), amount.toString().getBytes());
        for (byte[] leaf : proof) {
            if (_compare(hash, leaf) <= 0) {
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
        for (int i = 0; i < count; i++) {
            int cmp = Integer.compare(0xff & a[i], 0xff & b[i]);
            if (cmp != 0) {
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
        if (_token.equals(ZERO_ADDRESS)) {
            Context.transfer(_recipient, _amount);
        } else {
            Context.call(_token, "transfer", _recipient, _amount);
        }
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
    public void setAirdrop(Address _token, byte[] _merkleRoot, long _startTime, @Optional long _endTime, @Optional BigInteger _totalAmount) {
        _onlyAdmin();
        _checkContract(_token);
        _checkNotEmpty(_merkleRoot);
        _checkTime(_startTime, _endTime);
        _checkAmount(_totalAmount);

        airdropDb.set(new Airdrop( _token, _merkleRoot, _startTime, _endTime, _totalAmount));
        AirdropSet(_token, _merkleRoot, _startTime, _endTime, _getSafeString(_totalAmount));
    }

    @External
    public void updateAirdrop(long _startTime, @Optional long _endTime, @Optional BigInteger _totalAmount) {
        _onlyAdmin();
        _checkTime(_startTime, _endTime);
        _checkAmount(_totalAmount);

        Airdrop airdrop = airdropDb.get();
        _checkNotStarted(airdrop);

        airdrop.startTime = _startTime;
        airdrop.endTime = _endTime;
        airdrop.totalAmount = _totalAmount;

        airdropDb.set(airdrop);
        AirdropUpdated(_startTime, _endTime, _getSafeString(_totalAmount));
    }

    @External
    public void withdraw(Address _token, BigInteger _amount, @Optional Address _recipient) {
        _onlyAdmin();
        _recipient = _recipient == null ? admin() : _recipient;
        _transfer(_token, _recipient, _amount);
        Withdrawn(_token, _recipient, _amount);
    }

    @External(readonly = true)
    public byte[] merkleRoot(int _id) {
        Airdrop airdrop = airdropDb.get();
        if (airdrop != null) {
            return airdrop.merkleRoot;
        }
        return null;
    }

    @External(readonly = true)
    public Map info(int _id) {
        Airdrop airdrop = airdropDb.get();
        if (airdrop != null) {
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
    public boolean isValidProof(byte[] _merkleRoot, byte[] _hash, byte[][] _proof) {
        byte[] hash = _hash;
        for (byte[] leaf : _proof) {
            if (_compare(hash, leaf) <= 0) {
                hash = _makeHash(hash, leaf);
            } else {
                hash = _makeHash(leaf, hash);
            }
        }

        return _compare(hash, _merkleRoot) == 0;
    }

    @EventLog
    public void Deposited(Address _sender, BigInteger _amount) {
    }

    @EventLog
    public void AirdropSet(Address _token, byte[] _merkleRoot, long _startTime, long _endTime, String _totalAmount) {
    }

    @EventLog
    public void AirdropUpdated(long _startTime, long _endTime, String _totalAmount) {
    }

    @EventLog
    public void Withdrawn(Address _token, Address _recipient, BigInteger _amount) {
    }

    private void _handleRewardOption(Address claimer, int option, BigInteger amount) {
        RewardStatus rewardStatus;
        if (option == 1) { // instant claim
            BigInteger claimed = amount.divide(BigInteger.TWO);
            Token token = new Token(rewardToken.get());
            token.transfer(claimer, claimed);

            Address _treasury = treasury.get();
            BigInteger remained = amount.subtract(claimed);
            token.transfer(_treasury, remained);

            rewardStatus = new RewardStatus(claimed, claimed, BigInteger.ZERO);
        } else { // vesting
            rewardStatus = new RewardStatus(amount, BigInteger.ZERO, BigInteger.ZERO);
            // set vesting
            Vesting vesting = new Vesting(vestingContract.get());
            vesting.addVestingAccount(getVestingId(), claimer, amount);
        }
        rewardStatusDict.set(claimer, rewardStatus);
    }

    @External
    public void selectRewardOption(int option, BigInteger amount, byte[][] proof) {
        Address caller = Context.getCaller();
        int selected = selectedRewardOption.getOrDefault(caller, REWARD_OPTION_DEFAULT);
        if (selected != REWARD_OPTION_DEFAULT) {
            Context.revert("already selected");
        } else if ((option != REWARD_OPTION_INSTANT_CLAIM) && (option != REWARD_OPTION_VESTING)) {
            Context.revert("Invalid option");
        }

        Airdrop _airdrop = airdropDb.get();
        _require(_verifyProof(_airdrop.merkleRoot, caller, amount, proof), "Invalid proof");

        selectedRewardOption.set(caller, option);
        _handleRewardOption(caller, option, amount);
    }

    @External(readonly = true)
    public int getSelectedRewardOption(Address address) {
        return selectedRewardOption.getOrDefault(address, REWARD_OPTION_DEFAULT);
    }

    @External
    public void claimScheduledReward() {
        Address caller = Context.getCaller();
        int option = getSelectedRewardOption(caller);
        _require(option == REWARD_OPTION_VESTING, "not authorized");

        Vesting vesting = new Vesting(vestingContract.get());
        BigInteger claimable = vesting.claimable(getVestingId(), caller);
        if (claimable.compareTo(BigInteger.ZERO) > 0) {
            vesting.claim(getVestingId(), caller);
            RewardStatus rewardStatus = rewardStatusDict.get(caller);
            BigInteger claimed = rewardStatus.claimed.add(claimable);
            rewardStatus.claimed = claimed;
            rewardStatus.remained = rewardStatus.total.subtract(claimed);
            rewardStatusDict.set(caller, rewardStatus);
        }
    }

    @External
    public void setRewardToken(Address address) {
        rewardToken.set(address);
    }

    @External(readonly = true)
    public Address getRewardToken() {
        return rewardToken.get();
    }

    @External
    public void setVestingContract(Address contract) {
        _onlyAdmin();
        vestingContract.set(contract);
    }

    @External(readonly = true)
    public Address getVestingContract() {
        return vestingContract.get();
    }

    @External
    public void setTreasury(Address _treasury) {
        _onlyAdmin();
        treasury.set(_treasury);
    }

    @External
    public void setVestingId(int id) {
        _onlyAdmin();
        vestingId.set(id);
    }

    @External(readonly = true)
    public int getVestingId() {
        return vestingId.getOrDefault(0);
    }

    @External(readonly = true)
    public Map getRewardStatus(Address address) {
        // TODO: claimableAmount
        int selection = selectedRewardOption.get(address);
        if (selection == REWARD_OPTION_DEFAULT) {
            return Map.of(
                    "rewardOption", REWARD_OPTION_DEFAULT,
                    "total", -1,
                    "claimable", -1,
                    "remained", -1
            );
        }

        RewardStatus status = rewardStatusDict.get(address);
        BigInteger claimable = BigInteger.ZERO;
        if (selection == REWARD_OPTION_VESTING) {
            Vesting vesting = new Vesting(vestingContract.get());
            claimable = vesting.claimable(getVestingId(), address);
        }
        return Map.of(
                "rewardOption", selection,
                "total", status.total,
                "claimable", claimable,
                "remained", status.remained
        );
    }
}

