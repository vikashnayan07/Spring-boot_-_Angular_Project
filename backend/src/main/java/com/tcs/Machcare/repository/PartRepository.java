package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.Part;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
	List<Part> findByMachineIdIn(Collection<String> machineIds);
}