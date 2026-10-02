package com.automateit.backend.enums;

public enum UserRole {
    ADMIN,
    MANAGER,
    TESTER,
    VIEWER;

    public boolean canWriteTests() {
        return this == ADMIN || this == MANAGER || this == TESTER;
    }
}
