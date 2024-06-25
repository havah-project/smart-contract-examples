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
}
