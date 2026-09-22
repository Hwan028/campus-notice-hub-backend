package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

}
