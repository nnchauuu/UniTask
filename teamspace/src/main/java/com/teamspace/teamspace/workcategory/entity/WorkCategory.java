package com.teamspace.teamspace.workcategory.entity;

import java.time.LocalDateTime;

import com.teamspace.teamspace.project.entity.Project;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_categories", uniqueConstraints = @UniqueConstraint(name = "uk_work_category_name", columnNames = {"project_id", "normalized_name"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;
    @Column(nullable = false, length = 20)
    private String color;
    @Column(nullable = false, length = 50)
    private String icon;
    @Column(nullable = false)
    private int position;
    @Column(nullable = false)
    private boolean active;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Version private Long version;
    @PrePersist void prePersist() { var now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void preUpdate() { updatedAt=LocalDateTime.now(); }
}
