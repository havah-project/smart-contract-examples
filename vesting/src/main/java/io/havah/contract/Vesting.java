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
                _require(hour > -1 && hour < 24, "invalid hour");
        }
        switch (type) {
            case Weekly:
                _require(weekday > -1 && weekday < 7, "invalid weekday");
                break;
            case Yearly:
                _require(month > -1 && month < 13, "invalid month");
            case Monthly:
                _require(day > -1 && day < 32, "invalid day");
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

    @External(readonly = true)
    public int lastId() {
        return vestingId.getOrDefault(-1);
    }

    @External
    public void registerOnetimeVesting(Address _token, long _startTime, AccountInfo[] _accounts) {
        int id = _registerConditionalVesting(VestingScheduleType.Onetime, _token, _startTime, 0, 0, _accounts,
                -1, -1, -1, -1);
        RegisteredOnetimeVesting(id, _token, _startTime);
    }

    @External
    public void registerLinearVesting(Address _token, long _startTime, long _endTime, AccountInfo[] _accounts) {
        int id = _registerConditionalVesting(VestingScheduleType.Linear, _token, _startTime, _endTime, 0, _accounts,
                -1, -1, -1, -1);
        RegisteredLinearVesting(id, _token, _startTime, _endTime);
    }

    @External
    public void registerPeriodicVesting(Address _token, long _startTime, long _endTime, long _timeInterval, AccountInfo[] _accounts) {
        int id = _registerConditionalVesting(VestingScheduleType.Periodic, _token, _startTime, _endTime, _timeInterval, _accounts,
                -1, -1, -1, -1);
        RegisteredPeriodicVesting(id, _token, _startTime, _endTime, _timeInterval);
    }

    @External
    public void registerDailyVesting(Address _token, long _startTime, long _endTime, int _hour, AccountInfo[] _accounts) {
        int id = _registerConditionalVesting(VestingScheduleType.Daily, _token, _startTime, _endTime, 0, _accounts,
                -1, -1, -1, _hour);
        RegisteredDailyVesting(id, _token, _startTime, _endTime, _hour);
    }

    @External
    public void registerWeeklyVesting(Address _token, long _startTime, long _endTime, int _weekday, int _hour, AccountInfo[] _accounts) {
        int id = _registerConditionalVesting(VestingScheduleType.Weekly, _token, _startTime, _endTime, 0, _accounts,
                -1, -1, _weekday, _hour);
        RegisteredWeeklyVesting(id, _token, _startTime, _endTime, _weekday, _hour);
    }

    @External
    public void registerMonthlyVesting(Address _token, long _startTime, long _endTime, int _day, int _hour, AccountInfo[] _accounts) {
        int id = _registerConditionalVesting(VestingScheduleType.Monthly, _token, _startTime, _endTime, 0, _accounts,
                -1, _day, -1, _hour);
        RegisteredMonthlyVesting(id, _token, _startTime, _endTime, _day, _hour);
    }

    @External
    public void registerYearlyVesting(Address _token, long _startTime, long _endTime, int _month, int _day, int _hour, AccountInfo[] _accounts) {
        int id = _registerConditionalVesting(VestingScheduleType.Yearly, _token, _startTime, _endTime, 0, _accounts,
                _month, _day, -1, _hour);
        RegisteredYearlyVesting(id, _token, _startTime, _endTime, _month, _day, _hour);
    }

    protected int _registerConditionalVesting(VestingScheduleType type, Address token, long startTime, long endTime, long timeInterval,
                                               AccountInfo[] accounts, int month, int day,
                                               int weekday, int hour) {
        _onlyOwner();
        _checkStartEndTime(type, startTime, endTime);
        _checkScheduleParams4Type(type, month, day, weekday, hour);

        VestingSchedule schedule = new VestingSchedule();
        schedule.setType(type);
        schedule.setToken(token);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setTimeInterval(timeInterval);
        schedule.setMonth(month);
        schedule.setDay(day);
        schedule.setWeekday(weekday);
        schedule.setHour(hour);

        int id = lastId() + 1;
        vestingId.set(id);
        vestingSchedule.set(id, schedule);

        List vestingTime = VestingScheduleType.calculateVestingTime(schedule);
        _checkVestingTimes(type, vestingTime);
        for(int i=0; i<vestingTime.size(); i++) {
            vestingTimes.at(id).add((long) vestingTime.get(i));
        }

        BigInteger total = BigInteger.ZERO;
        int idx = accountInfoCount.getOrDefault(id, 0);
        for(AccountInfo account : accounts) {
            _require(accountInfo.at(id).get(account.getAddress()) == null, "duplicated address");

            BigInteger accountTotal = account.getTotalAmount();
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

        return id;
    }

    @External
    public void addVestingAccounts(int _id, AccountInfo[] _accounts) {
        _onlyOwner();
        _require(_accounts.length > 0, "no accounts");
        VestingSchedule schedule = vestingSchedule.get(_id);
        _require(schedule != null, "vesting was not registered");

        //VestingScheduleType style = schedule.getType();
        BigInteger sumAmount = totalAmount.get(_id);
        //int size = vestingTimes.at(_id).size();
        int idx = accountInfoCount.getOrDefault(_id, 0);

        for(AccountInfo account : _accounts) {
            _require(accountInfo.at(_id).get(account.getAddress()) == null, "duplicated address");

            BigInteger total = account.getTotalAmount();
            sumAmount = sumAmount.add(total);
            //account.setTotalAmount(total);

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
        _require(info.getTotalAmount().compareTo(claimed) > 0, "no claimable amount");

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

    @EventLog
    public void RegisteredOnetimeVesting(int _id, Address _token, long _startTime) {}

    @EventLog
    public void RegisteredLinearVesting(int _id, Address _token, long _startTime, long _endTime) {}

    @EventLog
    public void RegisteredPeriodicVesting(int _id, Address _token, long _startTime, long _endTime, long _timeInterval) {}

    @EventLog
    public void RegisteredDailyVesting(int _id, Address _token, long _startTime, long _endTime, int _hour) {}

    @EventLog
    public void RegisteredWeeklyVesting(int _id, Address _token, long _startTime, long _endTime, int _weekday, int _hour) {}

    @EventLog
    public void RegisteredMonthlyVesting(int _id, Address _token, long _startTime, long _endTime, int _day, int _hour) {}

    @EventLog
    public void RegisteredYearlyVesting(int _id, Address _token, long _startTime, long _endTime, int _month, int _day, int _hour) {}
}
