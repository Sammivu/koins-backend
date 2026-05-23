package com.middleware.entity.enums;

public enum WalletStatus {
    INACTIVE(0), ACTIVE(1), SUSPENDED(2);

    private final int value;
    WalletStatus(int value) { this.value = value; }
    public int getValue() { return value; }
}
