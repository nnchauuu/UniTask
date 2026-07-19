package com.teamspace.teamspace.contribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ContributionResponse {

    private Long userId;
    private String fullName;
    private String email;
    private int score;
    private long completedTasks;
    private long lateCompletedTasks;
    private long overdueTasks;
    private long commentsCount;
    private long uploadedFilesCount;
    private long joinedMeetingsCount;
}
