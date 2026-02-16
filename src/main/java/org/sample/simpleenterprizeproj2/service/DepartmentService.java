package org.sample.simpleenterprizeproj2.service;

import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id " + id));
    }

    public Department create(Department department) {
        return departmentRepository.save(department);
    }

    public Department update(Long id, Department updated) {
        Department department = findById(id);
        department.setName(updated.getName());
        department.setDescription(updated.getDescription());
        return departmentRepository.save(department);
    }

    public void delete(Long id) {
        Department department = findById(id);
        departmentRepository.delete(department);
    }
}
