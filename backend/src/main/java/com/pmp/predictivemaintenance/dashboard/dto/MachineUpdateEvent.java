package com.pmp.predictivemaintenance.dashboard.dto;

import org.springframework.context.ApplicationEvent;

public class MachineUpdateEvent extends ApplicationEvent {
    
    private final DashboardUpdateDto updateData;

    public MachineUpdateEvent(Object source, DashboardUpdateDto updateData) {
        super(source);
        this.updateData = updateData;
    }

    public DashboardUpdateDto getUpdateData() {
        return updateData;
    }
}
