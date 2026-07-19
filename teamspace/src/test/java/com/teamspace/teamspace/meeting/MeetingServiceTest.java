package com.teamspace.teamspace.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.meeting.dto.CreateMeetingTaskRequest;
import com.teamspace.teamspace.meeting.entity.Meeting;
import com.teamspace.teamspace.meeting.repository.MeetingNoteRepository;
import com.teamspace.teamspace.meeting.repository.MeetingParticipantRepository;
import com.teamspace.teamspace.meeting.repository.MeetingRepository;
import com.teamspace.teamspace.meeting.service.MeetingService;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.service.TaskService;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {
    @Mock MeetingRepository meetingRepository;
    @Mock MeetingNoteRepository meetingNoteRepository;
    @Mock MeetingParticipantRepository meetingParticipantRepository;
    @Mock ProjectRepository projectRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock ActivityLogService activityLogService;
    @Mock TaskService taskService;
    @Mock Authentication authentication;
    @InjectMocks MeetingService service;

    @Test
    void delegatesCompleteTaskFormPayloadAfterManagerCheck() {
        User leader = User.builder().id(1L).email("leader@example.com").fullName("Leader").password("x").build();
        Workspace workspace = Workspace.builder().id(10L).build();
        Project project = Project.builder().id(20L).workspace(workspace).createdBy(leader).build();
        Meeting meeting = Meeting.builder().id(30L).project(project).title("Planning").createdBy(leader).build();
        CreateMeetingTaskRequest request = new CreateMeetingTaskRequest();
        request.setTitle("Task from meeting");
        request.setDescription("Full payload");
        request.setAssignedToUserId(2L);
        request.setBoardColumnId(40L);
        request.setParentTaskId(50L);
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.HIGH);
        request.setType("Backend");
        request.setWorkCategoryId(60L);
        request.setReviewRequired(false);
        request.setStartDate(LocalDate.of(2026, 7, 16));
        request.setDueDate(LocalDate.of(2026, 7, 20));
        request.setLabels("meeting, backend");
        request.setEstimatedEffort(new BigDecimal("5.5"));
        request.setActualEffort(new BigDecimal("1.5"));
        TaskResponse expected = TaskResponse.builder().id(70L).title(request.getTitle()).build();

        when(authentication.getName()).thenReturn(leader.getEmail());
        when(userRepository.findByEmail(leader.getEmail())).thenReturn(Optional.of(leader));
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.of(meeting));
        when(memberRepository.findByWorkspaceIdAndUserId(workspace.getId(), leader.getId())).thenReturn(Optional.of(
                WorkspaceMember.builder().workspace(workspace).user(leader).role(WorkspaceRole.LEADER).build()
        ));
        when(taskService.createTask(project.getId(), request, authentication)).thenReturn(expected);

        TaskResponse response = service.createTaskFromMeeting(meeting.getId(), request, authentication);

        assertThat(response).isSameAs(expected);
        verify(taskService).createTask(project.getId(), request, authentication);
    }
}
