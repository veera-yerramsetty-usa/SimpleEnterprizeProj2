package org.sample.simpleenterprizeproj2.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SoftDeletePurgeTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(SoftDeletePurgeTasklet.class);

    private final JdbcTemplate jdbcTemplate;

    public SoftDeletePurgeTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int employees = jdbcTemplate.update("DELETE FROM employees WHERE deleted = true");
        int users = jdbcTemplate.update("DELETE FROM users WHERE deleted = true");
        int departments = jdbcTemplate.update("DELETE FROM departments WHERE deleted = true");

        log.info("Soft-delete purge completed — employees: {}, users: {}, departments: {}",
                employees, users, departments);

        return RepeatStatus.FINISHED;
    }
}
