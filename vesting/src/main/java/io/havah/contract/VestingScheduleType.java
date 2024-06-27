package io.havah.contract;

import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public enum VestingScheduleType {
    Onetime,
    Linear,
    Periodic,
    Daily,
    Weekly,
    Monthly,
    Yearly;

    public static void writeObject(ObjectWriter w, VestingScheduleType vs) {
        w.beginList(1);
        w.write(vs.name());
        w.end();
    }

    public static VestingScheduleType readObject(ObjectReader r) {
        r.beginList();
        String name = r.readString();
        r.end();
        return VestingScheduleType.valueOf(name);
    }

    public static List calculateVestingTime(VestingSchedule schedule) {
        List list = new ArrayList();
        switch (schedule.type) {
            case Onetime:
                list.add(schedule.startTime);
                break;
            case Linear:
                // do nothing..
                break;
            case Periodic:
            {
                long cur = schedule.startTime;
                while (cur < schedule.endTime) {
                    list.add(cur);
                    cur += schedule.timeInterval;
                }
            }
            break;
            case Daily:
            {
                long startDt = Datetime.getDateFromTime(schedule.startTime);
                long endDt = Datetime.getDateFromTime(schedule.endTime);
                long curDt = startDt;
                while (true) {
                    long dayTime = curDt * Datetime.ONE_DAY;
                    long cur = dayTime + (schedule.hour * Datetime.HOUR);
                    if(cur >= schedule.startTime) {
                        if (cur > schedule.endTime) break;
                        list.add(cur);
                    }
                    curDt++;
                    if(curDt > endDt) break;
                }
            }
            break;
            case Weekly:
            {
                long startDt = Datetime.getDateFromTime(schedule.startTime);
                long endDt = Datetime.getDateFromTime(schedule.endTime);
                long curDt = startDt;
                while (true) {
                    long dayTime = curDt * Datetime.ONE_DAY;
                    int day = Datetime.getWeek(dayTime);
                    if(schedule.weekday == day) {
                        long cur = dayTime + (schedule.hour * Datetime.HOUR);
                        if (cur >= schedule.startTime) {
                            if (cur > schedule.endTime) break;
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
                long[] info = Datetime.getMonthlyInfo(schedule.startTime);
                long year = info[0];
                long month = info[1];
                long monthTime = info[2];

                info = Datetime.getMonthlyInfo(schedule.endTime);
                long endYear = info[0];
                long endMonth = info[1];
                while (true) {
                    long monthday = Datetime.getMonthDay(year, month);
                    long dayTime = 0;
                    long cur = 0;
                    if (schedule.day == 0) {
                        dayTime = monthTime + ((monthday - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (schedule.hour * Datetime.HOUR);
                    } else if (schedule.day > monthday) {
                        dayTime = monthTime + (monthday * Datetime.ONE_DAY);
                        cur = dayTime;
                    } else {
                        dayTime = monthTime + ((schedule.day - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (schedule.hour * Datetime.HOUR);
                    }

                    if (cur >= schedule.startTime) {
                        if (cur > schedule.endTime) break;
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
                long[] info = Datetime.getYearInfo(schedule.startTime);
                long year = info[0];
                long yearTime = info[1];

                info = Datetime.getYearInfo(schedule.endTime);
                long endYear = info[0];
                while (true) {
                    long monthday = Datetime.getMonthDay(year, schedule.month);
                    long monthTime = yearTime + Datetime.getMonthAccTime(year, schedule.month - 1);
                    long dayTime;
                    long cur = 0;
                    if (schedule.day == 0) {
                        dayTime = monthTime + ((monthday - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (schedule.hour * Datetime.HOUR);
                    } else if (schedule.day > monthday) {
                        dayTime = monthTime + (monthday * Datetime.ONE_DAY);
                        cur = dayTime;
                    } else {
                        dayTime = monthTime + ((schedule.day - 1) * Datetime.ONE_DAY);
                        cur = dayTime + (schedule.hour * Datetime.HOUR);
                    }

                    if (cur >= schedule.startTime) {
                        if (cur > schedule.endTime) break;
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
}
