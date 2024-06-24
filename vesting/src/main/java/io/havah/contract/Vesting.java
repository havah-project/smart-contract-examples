package io.havah.contract;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class Vesting {
    static final Address ZERO_ADDRESS = Address.fromString("hx0000000000000000000000000000000000000000");

    protected static final VarDB<String> name = Context.newVarDB("name", String.class);
    protected final DictDB<Integer, BigInteger> totalClaimed = Context.newDictDB("total_claimed", BigInteger.class);
    protected final BranchDB<Integer, ArrayDB<Long>> vestingTimes = Context.newBranchDB("vesting_times", Long.class);
    protected final DictDB<Integer, BigInteger> totalAmount = Context.newDictDB("total_amount", BigInteger.class);
    protected final BranchDB<Integer, DictDB<Address, BigInteger>> accountClaimed = Context.newBranchDB("account_claimed", BigInteger.class);
    protected final BranchDB<Integer, DictDB<Address, AccountInfo>> accountInfo = Context.newBranchDB("account_info", AccountInfo.class);
    protected final BranchDB<Integer, DictDB<Integer, Address>> idxAccountDict = Context.newBranchDB("idx_accounts_dict", Address.class);
    protected final BranchDB<Integer, DictDB<Address, Integer>> accountIdxDict = Context.newBranchDB("accounts_idx_dict", Integer.class);
    protected final DictDB<Integer, Integer> accountInfoCount = Context.newDictDB("account_info_count", Integer.class);

    protected final DictDB<Integer, VestingSchedule> vestingSchedule = Context.newDictDB("vesting_schedule", VestingSchedule.class);
    protected final VarDB<Integer> vestingId = Context.newVarDB("vesting_id", Integer.class);

    protected boolean _isCaller(Address address) {
        return Context.getCaller().equals(address);
    }

    protected void _require(boolean condition, String err) {
        if(!condition)
            Context.revert(err);
    }

    protected void _onlyOwner() {
        _require(_isCaller(Context.getOwner()), "Only owner can call this method");
    }

    protected void _requireValidDate(int[] array, int start, int end) {
        for(int i : array) {
            Context.require(start <= i && i <= end, "invalid schedule");
        }
    }

    protected void _checkStartEndTime(VestingScheduleType type, long start, long end) {
        _require(start > Datetime.GENESIS_TIMESTAMP, "the start_time must be after 2024.01.01 00:00(UTC)");
        if(type != VestingScheduleType.Onetime)
            _require(start < end, "the start_time must be less than the end_time");
    }

    protected void _checkScheduleParams4Type(VestingScheduleType type, @Optional int month, @Optional int day,
                                             @Optional int weekday, @Optional int hour) {
        switch (type) {
            case Daily:
            case Weekly:
            case Monthly:
            case Yearly:
                _require(hour > -1 && hour < 24, "must have a hour");
        }
        switch (type) {
            case Weekly:
                _require(weekday > -1 && weekday < 7, "must have a weekday");
                break;
            case Yearly:
                _require(month > -1 && month < 13, "must have a month");
            case Monthly:
                _require(day > -1 && day < 32, "must have a day");
        }
    }

    protected void _checkVestingTimes(VestingScheduleType style, List vestingTimes) {
        switch (style) {
            case Daily:
            case Weekly:
            case Monthly:
            case Yearly:
                _require(vestingTimes.size() > 0, "empty vesting times");
        }
    }

    public Vesting(String _name) {
        name.set(_name);
    }

    @Payable
    public void fallback() {
        BigInteger value = Context.getValue();
        if (value.signum() > 0) {
            Deposited(Context.getCaller(), value);
        }
    }

    protected BigInteger _totalAmountFrom(VestingScheduleType style, int count, AccountInfo account) {
        _require(account.getEachAmount().signum() > 0 || account.getTotalAmount().signum() > 0, "an account must have each_amount or total_amount");
        if(account.getEachAmount().signum() > 0) {
            _require(style != VestingScheduleType.Onetime && style != VestingScheduleType.Linear, "cannot use each_amount");
            return account.getEachAmount().multiply(BigInteger.valueOf(count));
        }
        return account.getTotalAmount();
    }

    @External(readonly = true)
    public int lastId() {
        return vestingId.getOrDefault(-1);
    }

    @External
    public void registerOnetimeVesting(Address _token, long _startTime, AccountInfo[] _accounts) {
        registerConditionalVesting(0, _token, _startTime, 0, 0, _accounts,
                -1, -1, -1, -1);
    }

    @External
    public void registerLinearVesting(Address _token, long _startTime, long _endTime, AccountInfo[] _accounts) {
        registerConditionalVesting(1, _token, _startTime, _endTime, 0, _accounts,
                -1, -1, -1, -1);
    }

    @External
    public void registerPeriodicVesting(Address _token, long _startTime, long _endTime, long _timeInterval, AccountInfo[] _accounts) {
        registerConditionalVesting(2, _token, _startTime, _endTime, _timeInterval, _accounts,
                -1, -1, -1, -1);
    }

    @External
    public void registerConditionalVesting(int _type, Address _token, long _startTime, long _endTime, long _timeInterval,
                                           AccountInfo[] _accounts, int _month, int _day,
                                           int _weekday, int _hour) {
        _onlyOwner();
        VestingScheduleType type = VestingScheduleType.Onetime;
        switch (_type) {
            case 0:
                type = VestingScheduleType.Onetime;
                break;
            case 1:
                type = VestingScheduleType.Linear;
                break;
            case 2:
                type = VestingScheduleType.Periodic;
                break;
            case 3:
                type = VestingScheduleType.Daily;
                break;
            case 4:
                type = VestingScheduleType.Weekly;
                break;
            case 5:
                type = VestingScheduleType.Monthly;
                break;
            case 6:
                type = VestingScheduleType.Yearly;
                break;
        }
        _checkStartEndTime(type, _startTime, _endTime);
        _checkScheduleParams4Type(type, _month, _day, _weekday, _hour);

        VestingSchedule schedule = new VestingSchedule();
        schedule.setType(type);
        schedule.setToken(_token);
        schedule.setStartTime(_startTime);
        schedule.setEndTime(_endTime);
        schedule.setTimeInterval(_timeInterval);
        schedule.setMonth(_month);
        schedule.setDay(_day);
        schedule.setWeekday(_weekday);
        schedule.setHour(_hour);

        int id = lastId() + 1;
        vestingId.set(id);
        vestingSchedule.set(id, schedule);

        List vestingTime = schedule.calculateVestingTime();
        _checkVestingTimes(type, vestingTime);
        for(int i=0; i<vestingTime.size(); i++) {
            vestingTimes.at(id).add((long) vestingTime.get(i));
        }

        BigInteger total = BigInteger.ZERO;
        int idx = accountInfoCount.getOrDefault(id, 0);
        for(AccountInfo account : _accounts) {
            _require(accountInfo.at(id).get(account.getAddress()) == null, "duplicated address");

            BigInteger accountTotal = _totalAmountFrom(type, vestingTime.size(), account);
            total = total.add(accountTotal);
            account.setTotalAmount(accountTotal);

            Address address = account.getAddress();
            accountInfo.at(id).set(address, account);
            idxAccountDict.at(id).set(idx, address);
            accountIdxDict.at(id).set(address, idx);
            idx++;
        }
        accountInfoCount.set(id, idx);
        totalAmount.set(id, total);
    }

    @External
    public void addVestingAccounts(int _id, AccountInfo[] _accounts) {
        _onlyOwner();
        _require(_accounts.length > 0, "no accounts");
        VestingSchedule schedule = vestingSchedule.get(_id);
        _require(schedule != null, "vesting was not registered");

        VestingScheduleType style = schedule.getType();
        BigInteger sumAmount = totalAmount.get(_id);
        int size = vestingTimes.at(_id).size();
        int idx = accountInfoCount.getOrDefault(_id, 0);

        for(AccountInfo account : _accounts) {
            _require(accountInfo.at(_id).get(account.getAddress()) == null, "duplicated address");

            BigInteger total = _totalAmountFrom(style, size, account);
            sumAmount = sumAmount.add(total);
            account.setTotalAmount(total);

            Address address = account.getAddress();
            accountInfo.at(_id).set(address, account);
            idxAccountDict.at(_id).set(idx, address);
            accountIdxDict.at(_id).set(address, idx);
            idx++;
        }
        accountInfoCount.set(_id, idx);
        totalAmount.set(_id, sumAmount);
    }

    @External
    public void removeVestingAccounts(int _id, Address[] _accounts) {
        _onlyOwner();
        _require(_accounts.length > 0, "no accounts");
        VestingSchedule schedule = vestingSchedule.get(_id);
        _require(schedule != null, "vesting was not registered");

        int count = accountInfoCount.getOrDefault(_id, 0) - 1;
        for(Address address : _accounts) {
            AccountInfo info = accountInfo.at(_id).get(address);
            _require(info != null, "vesting entry is not found");

            BigInteger claimedAmount = accountClaimed.at(_id).getOrDefault(address, BigInteger.ZERO);
            BigInteger total = totalAmount.get(_id);

            BigInteger claimable_amount = info.getTotalAmount().subtract(claimedAmount);
            total = total.subtract(claimable_amount);
            totalAmount.set(_id, total);

            accountClaimed.at(_id).set(address, null);

            accountInfo.at(_id).set(address, null);

            Address last = idxAccountDict.at(_id).get(count);
            int idx = accountIdxDict.at(_id).get(address);
            accountIdxDict.at(_id).set(last, idx);
            idxAccountDict.at(_id).set(idx, last);
            accountIdxDict.at(_id).set(address, null);
            idxAccountDict.at(_id).set(count, null);

            count--;
        }
        accountInfoCount.set(_id, count);
    }

    protected void _transfer(Address token, Address recipient, BigInteger amount) {
        if(token.equals(ZERO_ADDRESS)) {
            Context.transfer(recipient, amount);
        } else {
            Context.call(token, "transfer", recipient, amount);
        }
    }

    @External
    public void withdraw(Address _token, BigInteger _amount, @Optional Address _recipient) {
        _onlyOwner();
        Address recipient = _recipient != null ? _recipient : Context.getCaller();
        _transfer(_token, recipient, _amount);
        Withdrawn(_token, recipient, _amount);
    }

    protected BigInteger _vestedAmountFrom(VestingScheduleType style, long startTime, long endTime, ArrayDB<Long> vestingTime, long blockTime, AccountInfo info) {
        if(info.getEachAmount().signum() > 0) {
            _require(style != VestingScheduleType.Onetime && style != VestingScheduleType.Linear, "cannot use each_amount");
            if(blockTime >= endTime) {
                return info.getEachAmount().multiply(BigInteger.valueOf(vestingTime.size()));
            }
            return info.getEachAmount().multiply(BigInteger.valueOf(_passedCountFrom(vestingTime, blockTime)));
        }
        BigInteger total = info.getTotalAmount();
        if(style == VestingScheduleType.Onetime) {
            if(startTime <= blockTime) {
                return total;
            }
            return BigInteger.ZERO;
        }
        if(blockTime >= endTime) {
            return total;
        }
        if(style == VestingScheduleType.Linear) {
            if(blockTime < startTime) {
                return BigInteger.ZERO;
            }
            return total.multiply(BigInteger.valueOf(blockTime).subtract(BigInteger.valueOf(startTime)))
                    .divide(BigInteger.valueOf(endTime).subtract(BigInteger.valueOf(startTime)));
        }
        return total.multiply(BigInteger.valueOf(_passedCountFrom(vestingTime, blockTime)))
                .divide(BigInteger.valueOf(vestingTime.size()));
    }

    protected int _passedCountFrom(ArrayDB<Long> vestingTime, long blockTime) {
        int passed = 0;
        for(int i=0; i<vestingTime.size(); i++) {
            if(vestingTime.get(i) > blockTime)
                return passed;
            passed++;
        }
        return passed;
    }

    @External
    public void claim(int _id) {
        VestingSchedule schedule = vestingSchedule.get(_id);
        _require(schedule != null, "vesting was not registered");
        Address caller = Context.getCaller();
        AccountInfo info = accountInfo.at(_id).get(caller);
        _require(info != null, "vesting entry is not found");

        Address token = schedule.token;

        BigInteger claimed = accountClaimed.at(_id).getOrDefault(caller, BigInteger.ZERO);
        _require(info.getTotalAmount().compareTo(claimed) >= 0, "no claimable amount");

        BigInteger vestedAmount = _vestedAmountFrom(schedule.type, schedule.startTime, schedule.endTime, vestingTimes.at(_id), Context.getBlockTimestamp(), info);
        if(vestedAmount.compareTo(claimed) <= 0)
            return;

        BigInteger claimableAmount = vestedAmount.subtract(claimed);
        accountClaimed.at(_id).set(caller, claimed.add(claimableAmount));
        totalClaimed.set(_id, totalClaimed.getOrDefault(_id, BigInteger.ZERO).add(claimableAmount));
        _transfer(token, caller, claimableAmount);

        Claimed(token, caller, claimableAmount);
    }

    @External(readonly = true)
    public List getAccounts(int _id) {
        List list = new ArrayList();
        int size = accountCount(_id);
        for(int i=0; i<size; i++) {
            list.add(idxAccountDict.at(_id).get(i));
        }
        return list;
    }

    @External(readonly = true)
    public Map getAccountInfo(int _id, Address _address) {
        AccountInfo aInfo = accountInfo.at(_id).get(_address);
        if(aInfo == null) return Map.of();

        VestingSchedule schedule = vestingSchedule.get(_id);
        BigInteger vested = _vestedAmountFrom(schedule.type, schedule.startTime, schedule.endTime, vestingTimes.at(_id), Context.getBlockTimestamp(), aInfo);
        BigInteger claimed = accountClaimed.at(_id).getOrDefault(_address, BigInteger.ZERO);
        return Map.of(
                "total", aInfo.getTotalAmount(),
                "vested", vested,
                "claimed", claimed,
                "claimable", vested.subtract(claimed)
        );
    }

    @External(readonly = true)
    public Map info(int _id) {
        VestingSchedule schedule = vestingSchedule.get(_id);
        if(schedule != null)
            return Map.of(
                    "type", schedule.type.name(),
                    "startTime", schedule.startTime,
                    "endTime", schedule.endTime,
                    "timeInterval", schedule.timeInterval,
                    "month", schedule.month,
                    "day", schedule.day,
                    "weekday", schedule.weekday,
                    "hour", schedule.hour,
                    "totalAmount", totalAmount.getOrDefault(_id, BigInteger.ZERO),
                    "totalClaimed", totalClaimed.getOrDefault(_id, BigInteger.ZERO)
            );
        return Map.of();
    }

    @External(readonly = true)
    public BigInteger claimableAmount(int _id, Address _address) {
        VestingSchedule schedule = vestingSchedule.get(_id);
        _require(schedule != null, "vesting was not registered");
        AccountInfo info = accountInfo.at(_id).get(_address);
        _require(info != null, "vesting entry is not found");

        BigInteger claimed = accountClaimed.at(_id).getOrDefault(_address, BigInteger.ZERO);
        if(info.getTotalAmount().compareTo(claimed) <= 0)
            return BigInteger.ZERO;

        BigInteger vestedAmount = _vestedAmountFrom(schedule.type, schedule.startTime, schedule.endTime, vestingTimes.at(_id), Context.getBlockTimestamp(), info);
        if(vestedAmount.compareTo(claimed) <= 0)
            return BigInteger.ZERO;

        return vestedAmount.subtract(claimed);
    }

    @External(readonly = true)
    public int accountCount(int _id) {
        return accountInfoCount.getOrDefault(_id, 0);
    }

    @External(readonly = true)
    public List vestingTimes(int _id) {
        List list = new ArrayList();
        for(int i=0; i<vestingTimes.at(_id).size(); i++) {
            list.add(vestingTimes.at(_id).get(i));
        }
        return list;
    }

    @EventLog
    public void Deposited(Address _sender,  BigInteger _amount) {}

    @EventLog
    public void Withdrawn(Address _token, Address _recipient, BigInteger _amount) {}

    @EventLog
    public void Claimed(Address _token, Address _recipient, BigInteger _amount) {}
}
