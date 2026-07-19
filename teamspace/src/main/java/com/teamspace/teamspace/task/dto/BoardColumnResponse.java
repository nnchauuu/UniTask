package com.teamspace.teamspace.task.dto;

import com.teamspace.teamspace.task.entity.BoardColumn;
import lombok.Builder;
import lombok.Getter;
import com.teamspace.teamspace.task.enums.StatusGroup;

@Getter
@Builder
public class BoardColumnResponse {
    private Long id;
    private String key;
    private String label;
    private String color;
    private Integer limit;
    private boolean collapsed;
    private int position;
    private boolean system;
    private StatusGroup statusGroup;
    private boolean defaultForGroup;
    private Long version;

    public static BoardColumnResponse from(BoardColumn column) {
        return BoardColumnResponse.builder()
                .id(column.getId())
                .key(column.getKey())
                .label(column.getLabel())
                .color(column.getColor())
                .limit(column.getWipLimit())
                .collapsed(column.isCollapsed())
                .position(column.getPosition())
                .system(column.isSystemColumn())
                .statusGroup(column.getStatusGroup())
                .defaultForGroup(column.isDefaultForGroup())
                .version(column.getVersion())
                .build();
    }
}
