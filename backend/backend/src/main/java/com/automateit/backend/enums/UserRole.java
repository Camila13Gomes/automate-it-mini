package com.automateit.backend.enums;

public enum UserRole {
    ADMIN,
    MANAGER,
    TESTER,
    VIEWER;

    public boolean canManageTests() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canExecuteTests() {
        return this == ADMIN || this == MANAGER || this == TESTER;
    }
}
