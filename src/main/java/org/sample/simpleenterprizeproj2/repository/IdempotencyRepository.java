package org.sample.simpleenterprizeproj2.repository;

import java.time.Instant;
import java.util.Optional;

import org.sample.simpleenterprizeproj2.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    void deleteByCreatedAtBefore(Instant cutoff);
}
