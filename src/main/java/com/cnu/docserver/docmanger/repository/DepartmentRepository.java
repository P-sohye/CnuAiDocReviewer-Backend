package com.cnu.docserver.docmanger.repository;

import com.cnu.docserver.docmanger.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}
