package com.pmp.predictivemaintenance.maintenance;

/**
 * Lifecycle status of a maintenance work order.
 * Stored as a string in the database (EnumType.STRING).
 */
public enum WorkOrderStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
