package org.sample.simpleenterprizeproj2.repository;

import org.sample.simpleenterprizeproj2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
