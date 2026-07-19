package com.teamspace.teamspace.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.planning.enums.PlanningState;
import java.time.LocalDate;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    @EntityGraph(attributePaths = {"assignedTo", "workCategory", "boardColumn", "parentTask", "createdBy", "project", "project.workspace", "subtasks", "checklistItems"})
    List<Task> findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(Long projectId, PlanningState planningState);

    @EntityGraph(attributePaths = {"assignedTo", "workCategory", "boardColumn", "parentTask", "createdBy", "project", "project.workspace", "subtasks", "checklistItems"})
    List<Task> findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(Long weeklyPlanId);

    List<Task> findByAssignedToIdOrderByDueDateAscCreatedAtDesc(Long assignedToId);

    List<Task> findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(Long boardColumnId);

    List<Task> findByParentTaskIdOrderByCreatedAtAsc(Long parentTaskId);

    long countByParentTaskId(Long parentTaskId);

    boolean existsByWorkCategoryId(Long workCategoryId);
    List<Task> findByDueDateAndPlanningState(LocalDate dueDate, PlanningState planningState);
    List<Task> findByDueDateBeforeAndPlanningState(LocalDate dueDate, PlanningState planningState);
}
