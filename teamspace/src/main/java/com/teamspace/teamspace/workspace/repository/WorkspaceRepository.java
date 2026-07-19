package com.teamspace.teamspace.workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.workspace.entity.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
