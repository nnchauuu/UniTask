package com.teamspace.teamspace.task.entity;

import com.teamspace.teamspace.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.teamspace.teamspace.task.enums.StatusGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "board_columns", uniqueConstraints = @UniqueConstraint(
        name = "uk_board_default_group", columnNames = { "project_id", "default_group_key" }
))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "column_key", nullable = false, length = 80)
    private String key;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 20)
    private String color;

    private Integer wipLimit;

    @Column(nullable = false)
    private boolean collapsed;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean systemColumn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusGroup statusGroup;

    @Column(name = "is_default_for_group", nullable = false)
    private boolean defaultForGroup;

    @Column(name = "default_group_key", length = 30)
    private String defaultGroupKey;

    @Version
    private Long version;

    public void configureGroup(StatusGroup group, boolean isDefault) {
        statusGroup = group;
        defaultForGroup = isDefault;
        defaultGroupKey = isDefault ? group.name() : null;
    }
}
