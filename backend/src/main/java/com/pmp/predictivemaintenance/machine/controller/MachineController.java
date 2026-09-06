package com.pmp.predictivemaintenance.machine.controller;

import com.pmp.predictivemaintenance.machine.dto.CreateMachineRequest;
import com.pmp.predictivemaintenance.machine.dto.MachineResponse;
import com.pmp.predictivemaintenance.machine.dto.UpdateMachineRequest;
import com.pmp.predictivemaintenance.machine.service.MachineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MachineResponse createMachine(@Valid @RequestBody CreateMachineRequest request) {
        return machineService.createMachine(request);
    }

    @GetMapping("/{id}")
    public MachineResponse getMachineById(@PathVariable UUID id) {
        return machineService.getMachineById(id);
    }

    @GetMapping
    public List<MachineResponse> getAllMachines() {
        return machineService.getAllMachines();
    }

    @PutMapping("/{id}")
    public MachineResponse updateMachine(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMachineRequest request) {
        return machineService.updateMachine(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decommissionMachine(@PathVariable UUID id) {
        machineService.decommissionMachine(id);
    }
}
