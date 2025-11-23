package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.PermissionDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PermissionDefinition entities
 */
@Repository
public interface PermissionDefinitionRepository extends JpaRepository<PermissionDefinition, UUID> {

    /**
     * Find permission definition by permission key
     */
    Optional<PermissionDefinition> findByPermissionKey(String permissionKey);
    
    /**
     * Check if permission key exists
     */
    boolean existsByPermissionKey(String permissionKey);
    
    /**
     * Find permissions by resource
     */
    List<PermissionDefinition> findByResource(String resource);
    
    /**
     * Find permissions by category
     */
    List<PermissionDefinition> findByCategory(String category);
    
    /**
     * Find permissions by risk level
     */
    List<PermissionDefinition> findByRiskLevel(PermissionDefinition.RiskLevel riskLevel);
    
    /**
     * Find system permissions
     */
    List<PermissionDefinition> findBySystemTrue();
    
    /**
     * Find permissions that require confirmation
     */
    List<PermissionDefinition> findByRequiresConfirmationTrue();
    
    /**
     * Find high-risk permissions
     */
    @Query("SELECT pd FROM PermissionDefinition pd WHERE pd.riskLevel IN ('HIGH', 'CRITICAL')")
    List<PermissionDefinition> findHighRiskPermissions();
    
    /**
     * Find permissions by resource and action
     */
    @Query("SELECT pd FROM PermissionDefinition pd WHERE pd.resource = :resource AND pd.action = :action")
    List<PermissionDefinition> findByResourceAndAction(@Param("resource") String resource, @Param("action") String action);
    
    /**
     * Search permissions by text in key, resource, or action
     */
    @Query("SELECT pd FROM PermissionDefinition pd WHERE " +
           "LOWER(pd.permissionKey) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pd.resource) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pd.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pd.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<PermissionDefinition> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    /**
     * Find permissions with filtering
     */
    @Query("SELECT pd FROM PermissionDefinition pd WHERE " +
           "(:resource IS NULL OR pd.resource = :resource) AND " +
           "(:category IS NULL OR pd.category = :category) AND " +
           "(:riskLevel IS NULL OR pd.riskLevel = :riskLevel) AND " +
           "(:systemOnly IS NULL OR pd.system = :systemOnly)")
    Page<PermissionDefinition> findWithFilters(@Param("resource") String resource,
                                              @Param("category") String category,
                                              @Param("riskLevel") PermissionDefinition.RiskLevel riskLevel,
                                              @Param("systemOnly") Boolean systemOnly,
                                              Pageable pageable);
    
    /**
     * Get statistics about permission definitions
     */
    @Query("SELECT pd.resource, COUNT(pd) FROM PermissionDefinition pd GROUP BY pd.resource")
    List<Object[]> getPermissionCountsByResource();
    
    @Query("SELECT pd.category, COUNT(pd) FROM PermissionDefinition pd GROUP BY pd.category")
    List<Object[]> getPermissionCountsByCategory();
    
    @Query("SELECT pd.riskLevel, COUNT(pd) FROM PermissionDefinition pd GROUP BY pd.riskLevel")
    List<Object[]> getPermissionCountsByRiskLevel();
}