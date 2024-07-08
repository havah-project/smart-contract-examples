package io.havah.contract;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

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
