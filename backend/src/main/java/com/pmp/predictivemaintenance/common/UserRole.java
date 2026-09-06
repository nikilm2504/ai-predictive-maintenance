package com.pmp.predictivemaintenance.common;

/**
 * Role of a platform user.
 * Stored as a string in the database (EnumType.STRING).
 */
public enum UserRole {
    ADMIN,
    OPERATOR,
    MAINTENANCE_ENGINEER
}
