package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.RequirementLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementLinkRepository extends JpaRepository<RequirementLink, UUID> {
    List<RequirementLink> findByFromRequirementId(UUID fromRequirementId);
    List<RequirementLink> findByToRequirementId(UUID toRequirementId);

    @Query("SELECT rl FROM RequirementLink rl WHERE rl.fromRequirement.id = :reqId OR rl.toRequirement.id = :reqId")
    List<RequirementLink> findAllLinksForRequirement(@Param("reqId") UUID reqId);
}
