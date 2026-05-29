package com.tcs.Machcare.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.Machcare.entity.Machine;

public interface MachineRepository
        extends JpaRepository<Machine, String> {

}