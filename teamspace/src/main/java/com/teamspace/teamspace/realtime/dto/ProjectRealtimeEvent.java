package com.teamspace.teamspace.realtime.dto;
import java.time.LocalDateTime; import java.util.Map; import java.util.UUID; import lombok.Builder; import lombok.Getter;
@Getter @Builder public class ProjectRealtimeEvent {private UUID eventId;private Long projectId;private Long taskId;private String eventType;private Long version;private Map<String,Object> data;private LocalDateTime occurredAt;}
