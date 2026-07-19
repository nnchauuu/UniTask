package com.teamspace.teamspace.workcategory.dto;
import java.time.LocalDateTime;
import com.teamspace.teamspace.workcategory.entity.WorkCategory;
import lombok.Builder; import lombok.Getter;
@Getter @Builder
public class WorkCategoryResponse {
 private Long id; private Long projectId; private String name; private String color; private String icon; private int position; private boolean active; private Long version; private LocalDateTime createdAt; private LocalDateTime updatedAt;
 public static WorkCategoryResponse from(WorkCategory c){return builder().id(c.getId()).projectId(c.getProject().getId()).name(c.getName()).color(c.getColor()).icon(c.getIcon()).position(c.getPosition()).active(c.isActive()).version(c.getVersion()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();}
}
