package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.RequirementHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementHistoryRepository extends JpaRepository<RequirementHistory, UUID> {
    List<RequirementHistory> findByRequirementIdOrderByChangedAtDesc(UUID requirementId);
}
