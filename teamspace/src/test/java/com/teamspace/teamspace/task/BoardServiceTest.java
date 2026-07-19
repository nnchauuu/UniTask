package com.teamspace.teamspace.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.realtime.service.ProjectRealtimeService;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;
import com.teamspace.teamspace.task.entity.BoardColumn;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.repository.BoardColumnRepository;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.service.BoardService;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {
    @Mock BoardColumnRepository boardColumnRepository;
    @Mock TaskRepository taskRepository;
    @Mock ProjectRealtimeService realtimeService;
    @Mock TaskChangeCoordinator taskChanges;
    @Mock ProjectRepository projectRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;
    @Mock UserRepository userRepository;

    private BoardService service;

    @BeforeEach
    void setUp() {
        service = new BoardService(boardColumnRepository, taskRepository, realtimeService, taskChanges,
                projectRepository, workspaceMemberRepository, userRepository);
    }

    @Test
    void usesFirstColumnInGroupWhenProjectHasNoExplicitDefault() {
        Project project = Project.builder().id(20L).name("Project").build();
        BoardColumn todo = BoardColumn.builder()
                .id(30L)
                .project(project)
                .key("CUSTOM_TODO")
                .label("Can lam")
                .position(0)
                .statusGroup(StatusGroup.TODO)
                .defaultForGroup(false)
                .build();

        when(boardColumnRepository.findByProjectIdOrderByPositionAsc(20L)).thenReturn(List.of(todo));
        when(taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(30L)).thenReturn(List.of());
        when(taskRepository.findByProjectIdOrderByCreatedAtDesc(20L)).thenReturn(List.of());
        when(boardColumnRepository.findByProjectIdAndStatusGroupAndDefaultForGroupTrue(20L, StatusGroup.TODO))
                .thenReturn(Optional.empty());

        assertThat(service.getDefaultColumn(project, StatusGroup.TODO)).isSameAs(todo);
    }
}
