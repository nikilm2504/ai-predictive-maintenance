package com.pmp.predictivemaintenance.machine.repository;

import com.pmp.predictivemaintenance.machine.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MachineRepository extends JpaRepository<Machine, UUID> {
    boolean existsByMachineCode(String machineCode);
    java.util.Optional<Machine> findByMachineCode(String machineCode);
}
