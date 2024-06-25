package io.havah.contract;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;
import java.util.Map;

public class VestingSchedule {
    VestingScheduleType type;
    Address token;
    long startTime;
    long endTime;
    long timeInterval;
    int month;
    int day;
    int weekday;
    int hour;

    public VestingScheduleType getType() {
        return type;
    }

    public void setType(VestingScheduleType type) {
        this.type = type;
    }

    public Address getToken() {
        return token;
    }

    public void setToken(Address token) {
        this.token = token;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(long timeInterval) {
        this.timeInterval = timeInterval;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public static void writeObject(ObjectWriter w, VestingSchedule v) {
        w.beginList(9);
        w.write(v.type);
        w.write(v.token);
        w.write(v.startTime);
        w.write(v.endTime);
        w.write(v.timeInterval);
        w.write(v.month);
        w.write(v.day);
        w.write(v.weekday);
        w.write(v.hour);
        w.end();
    }

    public static VestingSchedule readObject(ObjectReader r) {
        r.beginList();
        VestingSchedule v = new VestingSchedule();
        v.setType(r.read(VestingScheduleType.class));
        v.setToken(r.readAddress());
        v.setStartTime(r.readLong());
        v.setEndTime(r.readLong());
        v.setTimeInterval(r.readLong());
        v.setMonth(r.readInt());
        v.setDay(r.readInt());
        v.setWeekday(r.readInt());
        v.setHour(r.readInt());
        r.end();
        return v;
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

    public List calculateVestingTime() {
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
                    long cur = dayTime + (hour * Datetime.HOUR);
                    if(cur >= startTime) {
                        if (cur > endTime) break;
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
                    int day = Datetime.getWeek(dayTime);
                    if(weekday == day) {
                        long cur = dayTime + (hour * Datetime.HOUR);
                        if (cur >= startTime) {
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
                    long dayTime = 0;
                    long cur = 0;
                    if (day == 0) {
                        dayTime = monthTime + ((monthday - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (hour * Datetime.HOUR);
                    } else if (day > monthday) {
                        dayTime = monthTime + (monthday * Datetime.ONE_DAY);
                        cur = dayTime;
                    } else if (day <= monthday) {
                        dayTime = monthTime + ((day - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (hour * Datetime.HOUR);
                    }

                    if (cur >= startTime) {
                        if (cur > endTime) break;
                        list.add(cur);
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
                    long monthday = Datetime.getMonthDay(year, month);
                    long monthTime = yearTime + Datetime.getMonthAccTime(year, month - 1);
                    long dayTime;
                    long cur = 0;
                    if (day == 0) {
                        dayTime = monthTime + ((monthday - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (hour * Datetime.HOUR);
                    } else if (day > monthday) {
                        dayTime = monthTime + (monthday * Datetime.ONE_DAY);
                        cur = dayTime;
                    } else if (day <= monthday) {
                        dayTime = monthTime + ((day - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (hour * Datetime.HOUR);
                    }

                    if (cur >= startTime) {
                        if (cur > endTime) break;
                        list.add(cur);
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

    public Map toMap() {
        return Map.of(
                "type", type.name(),
                "startTime", startTime,
                "endTime", endTime,
                "timeInterval", timeInterval,
                "month", month,
                "day", day,
                "weekday", weekday,
                "hour", hour
        );
    }
}
