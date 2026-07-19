package com.teamspace.teamspace.taskactivity.entity;

import java.time.LocalDateTime;

import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_activities")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_id")
    private User actor;
    @Column(nullable = false, length = 60)
    private String actionType;
    @Column(length = 80)
    private String fieldName;
    @Column(length = 1000)
    private String oldValue;
    @Column(length = 1000)
    private String newValue;
    @Column(length = 2000)
    private String metadata;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
}
