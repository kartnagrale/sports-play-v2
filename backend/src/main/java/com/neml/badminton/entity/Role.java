package com.neml.badminton.entity;

public enum Role {
    SUPER_ADMIN,
    USER,
    /** Legacy values retained only so existing rows can be migrated safely. */
    @Deprecated ADMIN,
    @Deprecated TEAM_OWNER,
    @Deprecated VIEWER
}
