package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectId(UUID projectId);
    List<ProjectMember> findByUserId(UUID userId);
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);
    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("SELECT COUNT(*) > 0 FROM Project p WHERE p.id = :projectId AND " +
           "(p.createdBy.id = :userId OR p.id IN " +
           "(SELECT t.projectId FROM Team t WHERE t.id IN " +
           "(SELECT tm.teamId FROM TeamMember tm WHERE tm.userId = :userId)))")
    boolean hasProjectAccess(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
