package com.pmp.predictivemaintenance.machine;

/**
 * Operational status of an industrial machine.
 * Stored as a string in the database (EnumType.STRING).
 */
public enum MachineStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE,
    DECOMMISSIONED
}
