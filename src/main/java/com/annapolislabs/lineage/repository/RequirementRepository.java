package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    List<Requirement> findByProjectId(UUID projectId);
    List<Requirement> findByProjectIdAndParentIsNull(UUID projectId);
    List<Requirement> findByParentId(UUID parentId);
    Optional<Requirement> findByReqId(String reqId);
    boolean existsByReqId(String reqId);

    @Query(value = "SELECT * FROM requirements WHERE project_id = :projectId AND " +
            "to_tsvector('english', title || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :searchQuery)",
            nativeQuery = true)
    List<Requirement> searchByText(@Param("projectId") UUID projectId, @Param("searchQuery") String searchQuery);

    @Query("SELECT r FROM Requirement r WHERE r.project.id = :projectId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:priority IS NULL OR r.priority = :priority)")
    List<Requirement> findByFilters(@Param("projectId") UUID projectId,
                                   @Param("status") String status,
                                   @Param("priority") String priority);
}
