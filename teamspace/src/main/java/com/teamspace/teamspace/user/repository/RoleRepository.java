package com.teamspace.teamspace.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.user.entity.Role;
import com.teamspace.teamspace.user.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
