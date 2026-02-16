package org.sample.simpleenterprizeproj2.specification;

import org.sample.simpleenterprizeproj2.model.Employee;
import org.sample.simpleenterprizeproj2.util.SanitizationUtils;
import org.springframework.data.jpa.domain.Specification;

public class EmployeeSpecification {

    private EmployeeSpecification() {}

    public static Specification<Employee> build(String firstName, String lastName, String email, Long departmentId) {
        Specification<Employee> spec = Specification.where((Specification<Employee>) null);
        if (firstName != null && !firstName.isBlank()) {
            String escaped = SanitizationUtils.escapeWildcards(firstName.toLowerCase());
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("firstName")), "%" + escaped + "%"));
        }
        if (lastName != null && !lastName.isBlank()) {
            String escaped = SanitizationUtils.escapeWildcards(lastName.toLowerCase());
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("lastName")), "%" + escaped + "%"));
        }
        if (email != null && !email.isBlank()) {
            String escaped = SanitizationUtils.escapeWildcards(email.toLowerCase());
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + escaped + "%"));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("department").get("id"), departmentId));
        }
        return spec;
    }
}
