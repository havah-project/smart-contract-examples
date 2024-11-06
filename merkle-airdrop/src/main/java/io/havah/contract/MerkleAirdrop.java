package io.havah.contract;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

import static io.havah.contract.Errors.*;

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
    protected static final DictDB<Address, Integer> selectedRewardOption = Context.newDictDB("selected_reward_option", Integer.class);
    protected static final BranchDB<Integer, DictDB<Address, Boolean>> claimed = Context.newBranchDB("claimed", Boolean.class);
    protected static final VarDB<BigInteger> totalClaimed = Context.newVarDB("total_claimed", BigInteger.class);
    protected static final DictDB<Address, RewardStatus> rewardStatusDict = Context.newDictDB("reward_status", RewardStatus.class);

    protected boolean _isCaller(Address address) {
        return Context.getCaller().equals(address);
    }

    protected void _require(boolean condition, Errors error) {
        if (!condition) {
            Context.revert(error.ordinal(), error.getMessage());
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
        _require(_isCaller(admin()), ERR_NOT_ADMIN);
    }

    protected void _checkContract(Address address) {
        _require(address.equals(ZERO_ADDRESS) || address.isContract(), ERR_NOT_CONTRACT_ADDRESS);
    }

    protected void _checkNotEmpty(byte[] hash) {
        _require(hash != null && hash.length > 0, ERR_EMPTY_HASH);
    }

    protected void _checkTime(long start, long end) {
        _require(end == 0 || end > start, ERR_INVALID_TIME);
    }

    protected void _checkAmount(BigInteger amount) {
        _require(amount == null || amount.signum() > 0, ERR_INVALID_AMOUNT);
    }

    protected void _checkNotStarted(Airdrop airdrop) {
        _require(Context.getBlockTimestamp() < airdrop.getStartTime(), ERR_ALREADY_STARTED);
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
    public void setAirdrop(Address _token, byte[] _merkleRoot, long _startTime,
                           @Optional long _endTime, @Optional BigInteger _totalAmount) {
        _onlyAdmin();
        _checkContract(_token);
        _checkNotEmpty(_merkleRoot);
        _checkTime(_startTime, _endTime);
        _checkAmount(_totalAmount);

        airdropDb.set(new Airdrop(_token, _merkleRoot, _startTime, _endTime, _totalAmount));
        AirdropSet(_token, _merkleRoot, _startTime, _endTime, _getSafeString(_totalAmount));
    }

    @External
    public void updateAirdrop(long _startTime, @Optional long _endTime, @Optional BigInteger _totalAmount) {
        _onlyAdmin();
        _checkTime(_startTime, _endTime);
        _checkAmount(_totalAmount);

        Airdrop airdrop = airdropDb.get();
        _checkNotStarted(airdrop);

        airdrop.setStartTime(_startTime);
        airdrop.setEndTime(_endTime);
        airdrop.setTotalAmount(_totalAmount);

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
    public byte[] merkleRoot() {
        Airdrop airdrop = airdropDb.get();
        if (airdrop != null) {
            return airdrop.getMerkleRoot();
        }
        return null;
    }

    @External(readonly = true)
    public Map info(int _id) {
        Airdrop airdrop = airdropDb.get();
        if (airdrop != null) {
            BigInteger total = airdrop.getTotalAmount();
            BigInteger claimed = totalClaimed.getOrDefault(BigInteger.ZERO);
            BigInteger remained = total == null ? null : total.subtract(claimed);
            return Map.of(
                    "token", airdrop.getToken(),
                    "start", airdrop.getStartTime(),
                    "end", airdrop.getEndTime(),
                    "total", total,
                    "claimed", claimed,
                    "remain", remained
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
        Token token = new Token(getRewardToken());
        if (option == 1) { // instant claim
            BigInteger claimed = amount.divide(BigInteger.TWO);
            token.transfer(claimer, claimed);

            Address _treasury = getTreasury();
            BigInteger remained = amount.subtract(claimed);
            token.transfer(_treasury, remained);

            rewardStatus = new RewardStatus(claimed, claimed, BigInteger.ZERO);
        } else { // vesting
            rewardStatus = new RewardStatus(amount, BigInteger.ZERO, BigInteger.ZERO);
            // set vesting
            Vesting vesting = new Vesting(getVestingContract());
            vesting.addVestingAccount(getVestingId(), claimer, amount);
            token.transfer(getVestingContract(), amount);
        }
        rewardStatusDict.set(claimer, rewardStatus);
    }

    @External
    public void selectRewardOption(int _option, BigInteger _amount, byte[][] _proof) {
        Address caller = Context.getCaller();
        boolean isValidAmount = _amount != null && _amount.signum() > 0;
        Context.require(isValidAmount);
        int selected = selectedRewardOption.getOrDefault(caller, REWARD_OPTION_DEFAULT);
        _require(selected == REWARD_OPTION_DEFAULT, ERR_ALREADY_OPTION_SELECTED);

        boolean isValidOption = (_option == REWARD_OPTION_INSTANT_CLAIM) || (_option == REWARD_OPTION_VESTING);
        _require(isValidOption, ERR_NOT_A_VALID_OPTION);

        Airdrop _airdrop = airdropDb.get();
        boolean isValidProof = _verifyProof(_airdrop.getMerkleRoot(), caller, _amount, _proof);
        _require(isValidProof, ERR_INVALID_PROOF);

        selectedRewardOption.set(caller, _option);
        BigInteger claimed = totalClaimed.getOrDefault(BigInteger.ZERO);
        totalClaimed.set(claimed.add(_amount));
        _handleRewardOption(caller, _option, _amount);
    }

    @External(readonly = true)
    public int getSelectedRewardOption(Address address) {
        return selectedRewardOption.getOrDefault(address, REWARD_OPTION_DEFAULT);
    }

    @External
    public void claimScheduledReward() {
        Address caller = Context.getCaller();
        int option = getSelectedRewardOption(caller);
        _require(option == REWARD_OPTION_VESTING, ERR_NOT_SELECT_VESTING_OPTION);

        Vesting vesting = new Vesting(getVestingContract());
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

    @External(readonly = true)
    public Address getRewardToken() {
        return airdropDb.get().getToken();
    }

    @External
    public void setVestingContract(Address _contract) {
        _onlyAdmin();
        vestingContract.set(_contract);
    }

    @External(readonly = true)
    public Address getVestingContract() {
        return vestingContract.get();
    }

    @External(readonly = true)
    public Address getTreasury() {
        return treasury.get();
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
    public Map getRewardStatus(Address _address) {
        int selection = selectedRewardOption.getOrDefault(_address, REWARD_OPTION_DEFAULT);
        if (selection == REWARD_OPTION_DEFAULT) {
            return Map.of(
                    "rewardOption", REWARD_OPTION_DEFAULT,
                    "total", -1,
                    "claimable", -1,
                    "remained", -1
            );
        }

        RewardStatus status = rewardStatusDict.get(_address);
        BigInteger claimable = BigInteger.ZERO;
        if (selection == REWARD_OPTION_VESTING) {
            Vesting vesting = new Vesting(getVestingContract());
            claimable = vesting.claimable(getVestingId(), _address);
        }
        return Map.of(
                "rewardOption", selection,
                "total", status.total,
                "claimable", claimable,
                "remained", status.remained
        );
    }
}

