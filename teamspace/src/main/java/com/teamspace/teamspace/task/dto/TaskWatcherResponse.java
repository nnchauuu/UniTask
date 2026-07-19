package com.teamspace.teamspace.task.dto;
import java.time.LocalDateTime; import com.teamspace.teamspace.task.entity.TaskWatcher; import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary; import lombok.Builder; import lombok.Getter;
@Getter @Builder public class TaskWatcherResponse {private Long id;private WorkspaceUserSummary user;private LocalDateTime createdAt;public static TaskWatcherResponse from(TaskWatcher w){return builder().id(w.getId()).user(WorkspaceUserSummary.from(w.getUser())).createdAt(w.getCreatedAt()).build();}}
