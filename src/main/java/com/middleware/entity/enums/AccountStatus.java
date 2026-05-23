package com.middleware.entity.enums;

public enum AccountStatus {
    PENDING(0), ACTIVE(1), SUSPENDED(2);

    private final int value;
    AccountStatus(int value) { this.value = value; }
    public int getValue() { return value; }
}
