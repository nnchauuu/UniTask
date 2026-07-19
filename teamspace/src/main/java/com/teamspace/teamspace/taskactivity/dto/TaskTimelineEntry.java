package com.teamspace.teamspace.taskactivity.dto;
import java.time.LocalDateTime; import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary; import lombok.Builder; import lombok.Getter;
@Getter @Builder public class TaskTimelineEntry { private Long id; private String source; private String actionType; private String fieldName; private String oldValue; private String newValue; private String description; private WorkspaceUserSummary actor; private LocalDateTime createdAt; }
