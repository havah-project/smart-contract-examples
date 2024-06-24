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
    int[] month;
    int[] day;
    int[] weekday;
    int[] hour;

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

    public int[] getMonth() {
        return month;
    }

    public void setMonth(int[] month) {
        this.month = month.clone();
    }

    public int[] getDay() {
        return day;
    }

    public void setDay(int[] day) {
        this.day = day.clone();
    }

    public int[] getWeekday() {
        return weekday;
    }

    public void setWeekday(int[] weekday) {
        this.weekday = weekday.clone();
    }

    public int[] getHour() {
        return hour;
    }

    public void setHour(int[] hour) {
        this.hour = hour.clone();
    }

    public static void writeObject(ObjectWriter w, VestingSchedule v) {
        int size = 6;
        if(v.month != null) size += v.month.length;
        if(v.day != null) size += v.day.length;
        if(v.weekday != null) size += v.weekday.length;
        if(v.hour != null) size += v.hour.length;
        w.beginList(size);
        w.write(v.type);
        w.write(v.token);
        w.write(v.startTime);
        w.write(v.endTime);
        w.write(v.timeInterval);

        if(v.month != null) {
            w.write(v.month.length);
            for (int i = 0; i < v.month.length; i++)
                w.write(v.month[i]);
        } else w.write(0);

        if(v.day != null) {
            w.write(v.day.length);
            for (int i = 0; i < v.day.length; i++)
                w.write(v.day[i]);
        } else w.write(0);

        if(v.weekday != null) {
            w.write(v.weekday.length);
            for(int i=0; i<v.weekday.length; i++)
                w.write(v.weekday[i]);
        } else w.write(0);

        if(v.hour != null) {
            w.write(v.hour.length);
            for (int i = 0; i < v.hour.length; i++)
                w.write(v.hour[i]);
        } else w.write(0);

        w.end();
    }

    public static VestingSchedule readObject(ObjectReader r) {
        r.beginList();
        VestingSchedule v = new VestingSchedule();
//        v.setStyle(VestingScheduleStyle.valueOf(r.readString()));
        v.setType(r.read(VestingScheduleType.class));
        v.setToken(r.readAddress());
        v.setStartTime(r.readLong());
        v.setEndTime(r.readLong());
        v.setTimeInterval(r.readLong());
        int count = r.readInt();
        if(count > 0) {
            int[] month = new int[count];
            for (int i = 0; i < count; i++) {
                month[i] = r.readInt();
            }
            v.setMonth(month);
        }

        count = r.readInt();
        if(count > 0) {
            int[] day = new int[count];
            for (int i = 0; i < count; i++) {
                day[i] = r.readInt();
            }
            v.setDay(day);
        }

        count = r.readInt();
        if(count > 0) {
            int[] weekday = new int[count];
            for (int i = 0; i < count; i++) {
                weekday[i] = r.readInt();
            }
            v.setWeekday(weekday);
        }

        count = r.readInt();
        if(count > 0) {
            int[] hour = new int[count];
            for (int i = 0; i < count; i++) {
                hour[i] = r.readInt();
            }
            v.setHour(hour);
        }

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
                    for(int h : hour) {
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
                    int day = Datetime.getWeek(dayTime);
                    if(binarySearch(weekday, 0, weekday.length, day) > -1) {
                        for (int h : hour) {
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
                    for(int d : day) {
                        if(d <= 0) {
                            if (StrictMath.abs(d) > monthday) continue;
                            // 말일 기준 -값 만큼 앞의 날
                            d = (int) (monthday - (d * -1));
                        } else if (d > monthday)
                            continue;
                        d -= 1;
                        long dayTime = monthTime + (d * Datetime.ONE_DAY);
                        for (int h : hour) {
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
                    for(int m : month) {
                        long monthday = Datetime.getMonthDay(year, m);
                        long monthTime = yearTime + Datetime.getMonthAccTime(year, m - 1);
                        for (int d : day) {
                            if (d <= 0) {
                                if (StrictMath.abs(d) > monthday) continue;
                                // 말일 기준 -값 만큼 앞의 날
                                d = (int) (monthday - (d * -1));
                            } else if (d > monthday)
                                continue;
                            d -= 1;
                            long dayTime = monthTime + (d * Datetime.ONE_DAY);
                            for (int h : hour) {
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

    public Map toMap() {
        return Map.of(
                "type", type.name(),
                "startTime", startTime,
                "endTime", endTime,
                "timeInterval", timeInterval,
                "month", _getSafeArray2String(month),
                "day", _getSafeArray2String(day),
                "weekday", _getSafeArray2String(weekday),
                "hour", _getSafeArray2String(hour)
        );
    }
}
