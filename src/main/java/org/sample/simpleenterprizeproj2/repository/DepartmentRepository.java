package org.sample.simpleenterprizeproj2.repository;

import org.sample.simpleenterprizeproj2.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
