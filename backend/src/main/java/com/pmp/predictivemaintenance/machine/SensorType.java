package com.pmp.predictivemaintenance.machine;

/**
 * Type of a physical or simulated sensor attached to a machine.
 * Stored as a string in the database (EnumType.STRING).
 */
public enum SensorType {
    VIBRATION,
    TEMPERATURE,
    CURRENT,
    RPM
}
