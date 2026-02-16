package org.sample.simpleenterprizeproj2.specification;

import org.sample.simpleenterprizeproj2.model.Department;
import org.springframework.data.jpa.domain.Specification;

public class DepartmentSpecification {

    private DepartmentSpecification() {}

    public static Specification<Department> build(String name) {
        Specification<Department> spec = Specification.where((Specification<Department>) null);
        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        return spec;
    }
}
