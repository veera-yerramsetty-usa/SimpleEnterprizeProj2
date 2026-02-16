package org.sample.simpleenterprizeproj2.mapper;

import org.sample.simpleenterprizeproj2.dto.DepartmentPatchRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentResponse;
import org.sample.simpleenterprizeproj2.model.Department;
import org.sample.simpleenterprizeproj2.util.SanitizationUtils;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public Department toEntity(DepartmentRequest request) {
        Department department = new Department();
        department.setName(SanitizationUtils.sanitize(request.getName()));
        department.setDescription(SanitizationUtils.sanitize(request.getDescription()));
        return department;
    }

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription()
        );
    }

    public void updateEntity(Department department, DepartmentRequest request) {
        department.setName(SanitizationUtils.sanitize(request.getName()));
        department.setDescription(SanitizationUtils.sanitize(request.getDescription()));
    }

    public void patchEntity(Department department, DepartmentPatchRequest request) {
        if (request.getName() != null) {
            department.setName(SanitizationUtils.sanitize(request.getName()));
        }
        if (request.getDescription() != null) {
            department.setDescription(SanitizationUtils.sanitize(request.getDescription()));
        }
    }
}
