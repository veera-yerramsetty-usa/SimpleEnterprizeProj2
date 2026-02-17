package org.sample.simpleenterprizeproj2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sample.simpleenterprizeproj2.dto.JobExecutionResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/batch/jobs")
@Tag(name = "Batch Jobs", description = "Spring Batch job management operations")
public class BatchJobController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job softDeletePurgeJob;
    private final Job employeeExportJob;
    private final String exportDirectory;

    public BatchJobController(JobLauncher jobLauncher,
                              JobExplorer jobExplorer,
                              @Qualifier("softDeletePurgeJob") Job softDeletePurgeJob,
                              @Qualifier("employeeExportJob") Job employeeExportJob,
                              @Value("${batch.export.directory:./batch-output}") String exportDirectory) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.softDeletePurgeJob = softDeletePurgeJob;
        this.employeeExportJob = employeeExportJob;
        this.exportDirectory = exportDirectory;
    }

    @PostMapping("/soft-delete-purge")
    @Operation(summary = "Run soft-delete purge job",
            description = "Launches a batch job that permanently deletes all soft-deleted records")
    @ApiResponse(responseCode = "200", description = "Job completed")
    @ApiResponse(responseCode = "500", description = "Job failed",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<JobExecutionResponse> runPurgeJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("timestamp", Instant.now().toString())
                .toJobParameters();

        log.info("Launching softDeletePurgeJob");
        JobExecution execution = jobLauncher.run(softDeletePurgeJob, params);

        return ResponseEntity.ok(toResponse(execution));
    }

    @PostMapping("/employee-export")
    @Operation(summary = "Run employee CSV export job",
            description = "Launches a batch job that exports all active employees to a CSV file")
    @ApiResponse(responseCode = "200", description = "Job completed")
    @ApiResponse(responseCode = "500", description = "Job failed",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<JobExecutionResponse> runExportJob() throws Exception {
        Path exportDir = Path.of(exportDirectory);
        Files.createDirectories(exportDir);

        String timestamp = Instant.now().toString();
        String outputPath = exportDir.resolve("employees-" + timestamp.replace(":", "-") + ".csv").toString();

        JobParameters params = new JobParametersBuilder()
                .addString("outputPath", outputPath)
                .addString("timestamp", timestamp)
                .toJobParameters();

        log.info("Launching employeeExportJob with outputPath={}", outputPath);
        JobExecution execution = jobLauncher.run(employeeExportJob, params);

        return ResponseEntity.ok(toResponse(execution));
    }

    @GetMapping("/executions/{id}")
    @Operation(summary = "Get job execution status",
            description = "Retrieve the status of a batch job execution by its ID")
    @ApiResponse(responseCode = "200", description = "Execution found")
    @ApiResponse(responseCode = "404", description = "Execution not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<JobExecutionResponse> getExecution(
            @Parameter(description = "Job execution ID") @PathVariable Long id) {
        JobExecution execution = jobExplorer.getJobExecution(id);
        if (execution == null) {
            throw new ResourceNotFoundException("error.not.found.job.execution", id);
        }
        return ResponseEntity.ok(toResponse(execution));
    }

    private JobExecutionResponse toResponse(JobExecution execution) {
        return new JobExecutionResponse(
                execution.getId(),
                execution.getJobInstance().getJobName(),
                execution.getStatus().toString(),
                toInstant(execution.getStartTime()),
                toInstant(execution.getEndTime()),
                execution.getExitStatus().getExitDescription()
        );
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : null;
    }
}
