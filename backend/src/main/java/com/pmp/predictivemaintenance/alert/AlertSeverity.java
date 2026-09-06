package com.pmp.predictivemaintenance.alert;

/**
 * Severity of an alert generated from an ML prediction.
 * Stored as a string in the database (EnumType.STRING).
 */
public enum AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
