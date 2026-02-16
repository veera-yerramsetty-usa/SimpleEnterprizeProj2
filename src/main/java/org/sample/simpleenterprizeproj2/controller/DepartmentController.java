package org.sample.simpleenterprizeproj2.controller;

import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> getAll() {
        return departmentService.findAll();
    }

    @GetMapping("/{id}")
    public Department getById(@PathVariable Long id) {
        return departmentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Department> create(@RequestBody Department department) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(department));
    }

    @PutMapping("/{id}")
    public Department update(@PathVariable Long id, @RequestBody Department department) {
        return departmentService.update(id, department);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
