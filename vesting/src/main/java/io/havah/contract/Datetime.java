package io.havah.contract;

public class Datetime {
    static public final long GENESIS_YEAR = 2024L;
    static public final long GENESIS_TIMESTAMP = 1_704_067_200_000_000L;
    static public final long GENESIS_WEEK = 4L;
    static public final long HOUR = 3_600_000_000L;
    static public final long ONE_DAY = 86400_000_000L;
    static public final long NORMAL_YEAR = ONE_DAY * 365L;
    static public final long LEAF_YEAR = ONE_DAY * 366L;

    static public boolean isLeapYear(long year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    static public long[] getYearInfo(long time) {
        long year = GENESIS_YEAR;
        long accTime = GENESIS_TIMESTAMP;
        while (true) {
            long temp = accTime + (isLeapYear(year) ? LEAF_YEAR : NORMAL_YEAR);
            if(temp > time)
                break;
            year++;
            accTime = temp;
        }
        return new long[] { year, accTime};
    }

    static public int getWeek(long time) {
        return (int)(((time / ONE_DAY) + GENESIS_WEEK) % 7);
    }

    static public long getDateFromTime(long time) {
        return time / ONE_DAY;
    }

    static public long[] getMonthlyInfo(long time) {
        long[] info = getYearInfo(time);
        long year = info[0];
        long accTime = info[1];
        for(int i=1; i<=12; i++) {
            int day = getMonthDay(year, i);
            long tmp = accTime + (day * ONE_DAY);
            if(tmp > time)
                return new long[] {year, i, accTime};
            accTime = tmp;
        }
        return new long[]{};
    }

    static public int getMonthDay(long year, long month) {
        int day = 31;
        if(month < 8) {
            if (month == 2) {
                day = isLeapYear(year) ? 29 : 28;
            } else if (month % 2 == 0) {
                day = 30;
            }
        } else if (month % 2 == 0) {
            day = 31;
        } else
            day = 30;
        return day;
    }

    static public long getMonthAccTime(long year, long month) {
        long accTime = 0;
        for(int i=1; i<=month; i++) {
            int day = getMonthDay(year, i);
            accTime += (day * ONE_DAY);
        }
        return accTime;
    }
}
