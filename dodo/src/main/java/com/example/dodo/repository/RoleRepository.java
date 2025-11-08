package com.example.dodo.repository;

import com.example.dodo.entities.Role;
import com.example.dodo.entities.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Long> {
    Roles getByName(Role roleName);
    Roles findRolesById(Long id);
}
