package com.qflow.common.model;

public enum Priority {
    EMERGENCY(0),
    SENIOR_CITIZEN(1),
    PREGNANT(1),
    DISABLED(1),
    NORMAL(2);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}