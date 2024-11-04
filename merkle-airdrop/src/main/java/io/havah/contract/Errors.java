package io.havah.contract;

public enum Errors {
    ERR_NOT_ADMIN("Only administrator can call this method"), // 0x21
    ERR_NOT_CONTRACT_ADDRESS("Not contract address"),
    ERR_EMPTY_HASH("Empty hash"),
    ERR_INVALID_TIME("Invalid time"),
    ERR_INVALID_AMOUNT("Invalid amount"),
    ERR_ALREADY_STARTED("Already started airdrop"),
    ERR_ALREADY_OPTION_SELECTED("Already option selected"), // 0x27
    ERR_NOT_A_VALID_OPTION("Already option selected"),
    ERR_NOT_SELECT_VESTING_OPTION("Not select vesting option"),
    ERR_INVALID_PROOF("Invalid proof");

    private final String msg;

    Errors(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return this.msg;
    }
}
