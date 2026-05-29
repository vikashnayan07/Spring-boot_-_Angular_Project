package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.RaiseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaiseRequestRepository extends JpaRepository<RaiseRequest, Long> {
	List<RaiseRequest> findByDescriptionStartingWith(String prefix);
}
