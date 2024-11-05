package io.havah.contract;

public enum Errors {
    ERR_NOT_ADMIN("Only administrator can call this method"), // 0x21
    ERR_NOT_REWARD_MANAGER("Only reward manager can call this method"), // 0x21
    ERR_INVALID_START_TIME("the start_time must be after 2024.01.01 00:00(UTC)"),
    ERR_INVALID_TIME("the start_time must be less than the end_time"),
    ERR_INVALID_HOUR("invalid hour"),
    ERR_INVALID_WEEKDAY("invalid weekday"),
    ERR_INVALID_MONTH("invalid month"),
    ERR_INVALID_DAY("invalid day"),
    ERR_EMPTY_VESTING_TIMES("empty vesting times"),
    ERR_DUPLICATED_ADDRESS("duplicated address"),
    ERR_VESTING_NOT_REGISTERED("vesting was not registered"),
    ERR_NO_ACCOUNTS("no accounts"),
    ERR_VESTING_ENTRY_NOT_FOUND("vesting entry is not found"),
    ERR_NO_CLAIMABLE_AMOUNT("no claimable amount");

    private final String msg;

    Errors(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return this.msg;
    }
}
