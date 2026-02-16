package org.sample.simpleenterprizeproj2.specification;

import org.sample.simpleenterprizeproj2.model.Employee;
import org.springframework.data.jpa.domain.Specification;

public class EmployeeSpecification {

    private EmployeeSpecification() {}

    public static Specification<Employee> build(String firstName, String lastName, String email, Long departmentId) {
        Specification<Employee> spec = Specification.where((Specification<Employee>) null);
        if (firstName != null && !firstName.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
        }
        if (lastName != null && !lastName.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("department").get("id"), departmentId));
        }
        return spec;
    }
}
