package com.teamspace.teamspace.meeting.dto;

import com.teamspace.teamspace.task.dto.CreateTaskRequest;

/**
 * Uses the complete task creation contract so fields collected by TaskForm are
 * not silently discarded by the specialized meeting endpoint.
 */
public class CreateMeetingTaskRequest extends CreateTaskRequest {
}
