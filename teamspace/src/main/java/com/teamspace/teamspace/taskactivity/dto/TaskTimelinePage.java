package com.teamspace.teamspace.taskactivity.dto;
import java.util.List; import lombok.Builder; import lombok.Getter;
@Getter @Builder public class TaskTimelinePage { private List<TaskTimelineEntry> content; private int page; private int size; private long totalElements; private int totalPages; }
