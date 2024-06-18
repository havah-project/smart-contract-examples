package io.havah.contract;

import io.havah.contract.util.EnumerableMap;
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
    protected final VarDB<BigInteger> totalClaimed = Context.newVarDB("total_claimed", BigInteger.class);
    protected final VarDB<Address> vestingToken = Context.newVarDB("tokens", Address.class);
    protected final VarDB<VestingScheduleType> scheduleType = Context.newVarDB("schedule_type", VestingScheduleType.class);
    protected final ArrayDB<Long> vestingTimes = Context.newArrayDB("vesting_times", Long.class);
    protected final VarDB<BigInteger> totalAmount = Context.newVarDB("total_amount", BigInteger.class);
    protected final DictDB<Address, BigInteger> accountClaimed = Context.newDictDB("account_claimed", BigInteger.class);
    protected final EnumerableMap<Address, AccountInfo> accountInfo = new EnumerableMap("account_info", Address.class, AccountInfo.class);
    protected final VarDB<Long> startTime = Context.newVarDB("start_time", Long.class);
    protected final VarDB<Long> endTime = Context.newVarDB("end_time", Long.class);
    protected final VarDB<Long> timeInterval = Context.newVarDB("time_interval", Long.class);
    protected final VarDB<int[]> month = Context.newVarDB("month", int[].class);
    protected final VarDB<int[]> day = Context.newVarDB("day", int[].class);
    protected final VarDB<int[]> weekday = Context.newVarDB("weekday", int[].class);
    protected final VarDB<int[]> hour = Context.newVarDB("hour", int[].class);

    protected boolean _isCaller(Address address) {
        return Context.getCaller().equals(address);
    }

    protected void _onlyOwner() {
        Context.require(_isCaller(Context.getOwner()), "Only owner can call this method");
    }

    protected void _checkStartEndTime(VestingScheduleType type, long start, long end) {
        Context.require(start > Datetime.GENESIS_TIMESTAMP, "the start_time must be after 2024.01.01 00:00(UTC)");
        if(type != VestingScheduleType.Onetime)
            Context.require(start < end, "the start_time must be less than the end_time");
    }

    protected void _checkScheduleParams4Type(VestingScheduleType type, @Optional int[] month, @Optional int[] day,
                                             @Optional int[] weekday, @Optional int[] hour) {
        switch (type) {
            case Daily:
            case Weekly:
            case Monthly:
            case Yearly:
                Context.require(hour != null && hour.length > 0, "must have a least 1 item of hour list");
        }
        switch (type) {
            case Weekly:
                Context.require(weekday != null && weekday.length > 0, "must have a least 1 item of weekday list");
                break;
            case Yearly:
                Context.require(month != null && month.length > 0, "must have a least 1 item of month list");
            case Monthly:
                Context.require(day != null && day.length > 0, "must have a least 1 item of day list");
        }
    }

    protected void _checkVestingTimes(VestingScheduleType style, List vestingTimes) {
        switch (style) {
            case Daily:
            case Weekly:
            case Monthly:
            case Yearly:
                Context.require(vestingTimes.size() > 0, "empty vesting times");
        }
    }

    protected void _requireValidSchedule(int[] array, int start, int end) {
        for(int i : array) {
            Context.require(start <= i && i <= end, "invalid schedule");
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

    private static int binarySearch(int[] a, int fromIndex, int toIndex,
                                    int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    protected List _calculateVestingTime(VestingScheduleType type, long startTime, long endTime, long timeInterval,
                                         @Optional int[] months, @Optional int[] days,
                                         @Optional int[] weekdays, @Optional int[] hours) {
        List list = new ArrayList();
        switch (type) {
            case Onetime:
                list.add(startTime);
                break;
            case Linear:
                // do nothing..
                break;
            case Periodic:
            {
                long cur = startTime;
                while (cur < endTime) {
                    list.add(cur);
                    cur += timeInterval;
                }
            }
            break;
            case Daily:
            {
                long startDt = Datetime.getDateFromTime(startTime);
                long endDt = Datetime.getDateFromTime(endTime);
                long curDt = startDt;
                while (true) {
                    long dayTime = curDt * Datetime.ONE_DAY;
                    for(int h : hours) {
                        long cur = dayTime + (h * Datetime.HOUR);
                        if(cur < startTime) continue;
                        if(cur > endTime) break;
                        list.add(cur);
                    }
                    curDt++;
                    if(curDt > endDt) break;
                }
            }
            break;
            case Weekly:
            {
                long startDt = Datetime.getDateFromTime(startTime);
                long endDt = Datetime.getDateFromTime(endTime);
                long curDt = startDt;
                while (true) {
                    long dayTime = curDt * Datetime.ONE_DAY;
                    int curWeekday = Datetime.getWeek(dayTime);
                    if(binarySearch(weekdays, 0, weekdays.length, curWeekday) > -1) {
                        for (int h : hours) {
                            long cur = dayTime + (h * Datetime.HOUR);
                            if (cur < startTime) continue;
                            if (cur > endTime) break;
                            list.add(cur);
                        }
                    }
                    curDt++;
                    if(curDt > endDt) break;
                }
            }
            break;
            case Monthly:
            {
                long[] info = Datetime.getMonthlyInfo(startTime);
                long year = info[0];
                long month = info[1];
                long monthTime = info[2];

                info = Datetime.getMonthlyInfo(endTime);
                long endYear = info[0];
                long endMonth = info[1];
                while (true) {
                    long monthday = Datetime.getMonthDay(year, month);
                    for(int d : days) {
                        if(d <= 0) {
                            if(StrictMath.abs(d) > monthday) continue;
                            // 말일 기준 -값 만큼 앞의 날
                            d = (int) (monthday - (d * -1));
                        } else if (d > monthday)
                            continue;
                        d -= 1;
                        long dayTime = monthTime + (d * Datetime.ONE_DAY);
                        for (int h : hours) {
                            long cur = dayTime + (h * Datetime.HOUR);
                            if (cur < startTime) continue;
                            if (cur > endTime) break;
                            list.add(cur);
                        }
                    }
                    monthTime += monthday * Datetime.ONE_DAY;
                    month++;
                    if(month > 12) {
                        year++;
                        month = 1;
                    }
                    if(endYear < year || (endYear == year && endMonth < month))
                        break;
                }
            }
            break;
            case Yearly:
            {
                long[] info = Datetime.getYearInfo(startTime);
                long year = info[0];
                long yearTime = info[1];

                info = Datetime.getYearInfo(endTime);
                long endYear = info[0];
                while (true) {
                    for(int m : months) {
                        long monthday = Datetime.getMonthDay(year, m);
                        long monthTime = yearTime + Datetime.getMonthAccTime(year, m - 1);
                        for (int d : days) {
                            if (d <= 0) {
                                if (StrictMath.abs(d) > monthday) continue;
                                // 말일 기준 -값 만큼 앞의 날
                                d = (int) (monthday - (d * -1));
                            } else if (d > monthday)
                                continue;
                            d -= 1;
                            long dayTime = monthTime + (d * Datetime.ONE_DAY);
                            for (int h : hours) {
                                long cur = dayTime + (h * Datetime.HOUR);
                                if (cur < startTime) continue;
                                if (cur > endTime) break;
                                list.add(cur);
                            }
                        }
                    }
                    yearTime += (Datetime.isLeapYear(year) ? Datetime.LEAF_YEAR : Datetime.NORMAL_YEAR);
                    year++;
                    if(endYear < year)
                        break;
                }
            }
            break;
            default:
        }

        return list;
    }

    protected String _getSafeArray2String(int[] arr) {
        if(arr == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(arr[0]);
        for(int i=1; i<arr.length; i++) {
            sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    protected BigInteger _totalAmountFrom(VestingScheduleType style, int count, AccountInfo account) {
        Context.require(account.getEachAmount().signum() > 0 || account.getTotalAmount().signum() > 0, "an account must have each_amount or total_amount");
        if(account.getEachAmount().signum() > 0) {
            Context.require(style != VestingScheduleType.Onetime && style != VestingScheduleType.Linear, "cannot use each_amount");
            return account.getEachAmount().multiply(BigInteger.valueOf(count));
        }
        return account.getTotalAmount();
    }

    @External
    public void registerOnetimeVesting(Address _token, long _startTime, AccountInfo[] _accounts) {
        registerConditionalVesting(0, _token, _startTime, 0, 0, _accounts,
                null, null, null, null);
    }

    @External
    public void registerLinearVesting(Address _token, long _startTime, long _endTime, AccountInfo[] _accounts) {
        registerConditionalVesting(1, _token, _startTime, _endTime, 0, _accounts,
                null, null, null, null);
    }

    @External
    public void registerPeriodicVesting(Address _token, long _startTime, long _endTime, long _timeInterval, AccountInfo[] _accounts) {
        registerConditionalVesting(2, _token, _startTime, _endTime, _timeInterval, _accounts,
                null, null, null, null);
    }

    @External
    public void registerConditionalVesting(int _type, Address _token, long _startTime, long _endTime, long _timeInterval,
                                           AccountInfo[] _accounts, @Optional int[] _month, @Optional int[] _day,
                                           @Optional int[] _weekday, @Optional int[] _hour) {
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

        List vestingTime = _calculateVestingTime(type, _startTime, _endTime, _timeInterval, _month, _day, _weekday, _hour);
        _checkVestingTimes(type, vestingTime);
        for(int i=0; i<vestingTime.size(); i++) {
            vestingTimes.add((long) vestingTime.get(i));
        }

        BigInteger total = BigInteger.ZERO;
        for(AccountInfo account : _accounts) {
            BigInteger accountTotal = _totalAmountFrom(type, vestingTime.size(), account);
            total = total.add(accountTotal);
            account.setTotalAmount(accountTotal);
            Context.require(!accountInfo.contains(account.getAddress()), "duplicated address");
            accountInfo.set(account.getAddress(), account);
        }
        totalAmount.set(total);

        scheduleType.set(type);
        vestingToken.set(_token);
        startTime.set(_startTime);
        endTime.set(_endTime);
        timeInterval.set(_timeInterval);

        if(_month != null)
            month.set(_month);
        if(_day != null)
            day.set(_day);
        if(_weekday != null)
            weekday.set(_weekday);
        if(_hour != null)
            hour.set(_hour);
    }

    @External
    public void addVestingAccounts(AccountInfo[] _accounts) {
        _onlyOwner();
        Context.require(vestingToken.get() != null, "vesting was not registered");
        Context.require(_accounts.length > 0, "no accounts");

        VestingScheduleType style = scheduleType.get();
        BigInteger sumAmount = totalAmount.get();

        for(AccountInfo account : _accounts) {
            Context.require(!accountInfo.contains(account.getAddress()), "duplicated address");

            BigInteger total = _totalAmountFrom(style, vestingTimes.size(), account);
            sumAmount = sumAmount.add(total);
            account.setTotalAmount(total);
            accountInfo.set(account.getAddress(), account);
        }
        totalAmount.set(sumAmount);
    }

    @External
    public void removeVestingAccounts(Address[] _accounts) {
        _onlyOwner();
        Context.require(vestingToken.get() != null, "vesting was not registered");
        Context.require(_accounts.length > 0, "no accounts");

        for(Address address : _accounts) {
            AccountInfo info = accountInfo.get(address);
            Context.require(info != null, "vesting entry is not found");

            BigInteger claimedAmount = accountClaimed.getOrDefault(address, BigInteger.ZERO);
            BigInteger total = totalAmount.get();

            BigInteger claimable_amount = info.getTotalAmount().subtract(claimedAmount);
            total = total.subtract(claimable_amount);

            accountInfo.remove(address);
            accountClaimed.set(address, null);
            totalAmount.set(total);
        }
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
            Context.require(style != VestingScheduleType.Onetime && style != VestingScheduleType.Linear, "cannot use each_amount");
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
    public void claim() {
        Address caller = Context.getCaller();
        long blockTime = Context.getBlockTimestamp();
        Address token = vestingToken.get();
        Context.require(token != null, "vesting was not registered");
        AccountInfo info = accountInfo.get(caller);
        Context.require(info != null, "vesting entry is not found");

        BigInteger claimed = accountClaimed.getOrDefault(caller, BigInteger.ZERO);
        Context.require(info.getTotalAmount().compareTo(claimed) >= 0, "no claimable amount");

        BigInteger vestedAmount = _vestedAmountFrom(scheduleType.get(), startTime.get(), endTime.get(), vestingTimes, blockTime, info);
        if(vestedAmount.compareTo(claimed) <= 0)
            return;

        BigInteger claimableAmount = vestedAmount.subtract(claimed);
        accountClaimed.set(caller, claimed.add(claimableAmount));
        totalClaimed.set(totalClaimed.getOrDefault(BigInteger.ZERO).add(claimableAmount));
        _transfer(token, caller, claimableAmount);

        Claimed(token, caller, claimableAmount);
    }

    @External(readonly = true)
    public List getAccounts() {
        List list = new ArrayList();
        for(int i=0; i<accountInfo.length(); i++) {
            list.add(accountInfo.getKey(i));
        }
        return list;
    }

    @External(readonly = true)
    public Map getAccountInfo(Address _address) {
        AccountInfo aInfo = accountInfo.get(_address);
        if(aInfo == null) return Map.of();

        BigInteger vested = _vestedAmountFrom(scheduleType.get(), startTime.get(), endTime.get(), vestingTimes, Context.getBlockTimestamp(), aInfo);
        BigInteger claimed = accountClaimed.get(_address);
        return Map.of(
                "total", aInfo.getTotalAmount(),
                "vested", vested,
                "claimed", claimed,
                "claimable", vested.subtract(claimed)
        );
    }

    @External(readonly = true)
    public Map info() {
        if(vestingToken.get() != null)
            return Map.of(
                    "type", scheduleType.get().name(),
                    "startTime", startTime.get(),
                    "endTime", endTime.get(),
                    "timeInterval", timeInterval.get(),
                    "month", _getSafeArray2String(month.get()),
                    "day", _getSafeArray2String(day.get()),
                    "weekday", _getSafeArray2String(weekday.get()),
                    "hour", _getSafeArray2String(hour.get()),
                    "totalAmount", totalAmount.getOrDefault(BigInteger.ZERO),
                    "totalClaimed", totalClaimed.getOrDefault(BigInteger.ZERO)
            );
        return Map.of();
    }

    @External(readonly = true)
    public BigInteger claimableAmount(Address _address) {
        Context.require(vestingToken.get() != null, "vesting was not registered");
        AccountInfo info = accountInfo.get(_address);
        Context.require(info != null, "vesting entry is not found");

        BigInteger claimed = accountClaimed.getOrDefault(_address, BigInteger.ZERO);
        if(info.getTotalAmount().compareTo(claimed) <= 0)
            return BigInteger.ZERO;

        BigInteger vestedAmount = _vestedAmountFrom(scheduleType.get(), startTime.get(), endTime.get(), vestingTimes, Context.getBlockTimestamp(), info);
        if(vestedAmount.compareTo(claimed) <= 0)
            return BigInteger.ZERO;

        return vestedAmount.subtract(claimed);
    }

    @External(readonly = true)
    public int accountCount() {
        return accountInfo.length();
    }

    @External(readonly = true)
    public List vestingTimes() {
        List list = new ArrayList();
        for(int i=0; i<vestingTimes.size(); i++) {
            list.add(vestingTimes.get(i));
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
