package org.sample.simpleenterprizeproj2.specification;

import org.sample.simpleenterprizeproj2.model.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> build(String username, String email, String role) {
        Specification<User> spec = Specification.where((Specification<User>) null);
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (role != null && !role.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role"), role));
        }
        return spec;
    }
}
