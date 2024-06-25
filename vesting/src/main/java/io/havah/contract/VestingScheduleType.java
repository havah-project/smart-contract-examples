package io.havah.contract;

import score.ObjectReader;
import score.ObjectWriter;

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
}
