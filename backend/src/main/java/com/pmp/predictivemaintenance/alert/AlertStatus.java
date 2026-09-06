package com.pmp.predictivemaintenance.alert;

/**
 * Lifecycle status of an alert.
 * Stored as a string in the database (EnumType.STRING).
 */
public enum AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED
}
