package com.teamspace.teamspace.workcategory.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.teamspace.teamspace.workcategory.entity.WorkCategory;

public interface WorkCategoryRepository extends JpaRepository<WorkCategory, Long> {
    List<WorkCategory> findByProjectIdOrderByPositionAsc(Long projectId);
    Optional<WorkCategory> findByProjectIdAndNormalizedName(Long projectId, String normalizedName);
    boolean existsByProjectIdAndNormalizedNameAndIdNot(Long projectId, String normalizedName, Long id);
}
