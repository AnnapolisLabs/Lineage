package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByProjectKey(String projectKey);
    boolean existsByProjectKey(String projectKey);

    @Query("SELECT p FROM Project p JOIN ProjectMember pm ON p.id = pm.project.id WHERE pm.user.id = :userId")
    List<Project> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.createdBy.id = :userId OR p.id IN " +
           "(SELECT t.projectId FROM Team t WHERE t.id IN " +
           "(SELECT tm.teamId FROM TeamMember tm WHERE tm.userId = :userId))")
    List<Project> findAllOwnedAndMemberProjectsByUserId(@Param("userId") UUID userId);
}
