package com.middleware.entity.enums;

public enum LoanStatus {
    PENDING(0), APPROVED(1), DISBURSED(2), REPAID(3), DEFAULTED(4);

    private final int value;
    LoanStatus(int value) { this.value = value; }
    public int getValue() { return value; }
}
