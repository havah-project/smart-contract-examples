package io.havah.contract;

import score.ObjectReader;
import score.ObjectWriter;

public enum VestingScheduleType {
    Onetime("onetime"),
    Linear("linear"),
    Periodic("periodic"),
    Daily("daily_condition"),
    Weekly("weekly_condition"),
    Monthly("monthly_condition"),
    Yearly("yearly_condition");

    private final String msg;
    VestingScheduleType(String _msg) {
        this.msg = _msg;
    }
    public String getMessage() {
        return msg;
    }

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
}
