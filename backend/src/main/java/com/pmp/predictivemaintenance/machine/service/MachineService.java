package com.pmp.predictivemaintenance.machine.service;

import com.pmp.predictivemaintenance.common.exception.DuplicateResourceException;
import com.pmp.predictivemaintenance.common.exception.ResourceNotFoundException;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.MachineStatus;
import com.pmp.predictivemaintenance.machine.dto.CreateMachineRequest;
import com.pmp.predictivemaintenance.machine.dto.MachineResponse;
import com.pmp.predictivemaintenance.machine.dto.UpdateMachineRequest;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MachineService {

    private final MachineRepository machineRepository;

    public MachineService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    @Transactional
    public MachineResponse createMachine(CreateMachineRequest request) {
        if (machineRepository.existsByMachineCode(request.machineCode())) {
            throw new DuplicateResourceException("Machine code already exists: " + request.machineCode());
        }

        Machine machine = new Machine(
                request.machineCode(),
                request.name(),
                request.machineType(),
                request.location()
        );

        machine = machineRepository.save(machine);
        return mapToResponse(machine);
    }

    @Transactional(readOnly = true)
    public MachineResponse getMachineById(UUID id) {
        Machine machine = getMachineEntity(id);
        return mapToResponse(machine);
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> getAllMachines() {
        return machineRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MachineResponse updateMachine(UUID id, UpdateMachineRequest request) {
        Machine machine = getMachineEntity(id);

        machine.setName(request.name());
        machine.setMachineType(request.machineType());
        machine.setLocation(request.location());
        machine.setStatus(request.status());

        machine = machineRepository.save(machine);
        return mapToResponse(machine);
    }

    @Transactional
    public void decommissionMachine(UUID id) {
        Machine machine = getMachineEntity(id);
        machine.setStatus(MachineStatus.DECOMMISSIONED);
        machineRepository.save(machine);
    }

    protected Machine getMachineEntity(UUID id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id: " + id));
    }

    private MachineResponse mapToResponse(Machine machine) {
        return new MachineResponse(
                machine.getId(),
                machine.getMachineCode(),
                machine.getName(),
                machine.getMachineType(),
                machine.getLocation(),
                machine.getStatus(),
                machine.getCreatedAt(),
                machine.getUpdatedAt()
        );
    }
}
