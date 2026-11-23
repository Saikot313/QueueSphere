package com.qflow.common.security;

import com.qflow.common.exception.ApiException;

public final class Roles {
    private Roles() {
    }

    public static final String ADMIN = "ADMIN";
    public static final String STAFF = "STAFF";
    public static final String CITIZEN = "CITIZEN";

    public static void require(String role, String... allowed) {
        for (String a : allowed) {
            if (a.equals(role))
                return;
        }
        throw ApiException.forbidden("Access denied for role: " + role);
    }
}