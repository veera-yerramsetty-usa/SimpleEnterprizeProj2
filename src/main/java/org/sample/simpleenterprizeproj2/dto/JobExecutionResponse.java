package org.sample.simpleenterprizeproj2.dto;

import java.time.Instant;

public class JobExecutionResponse {

    private Long executionId;
    private String jobName;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private String exitDescription;

    public JobExecutionResponse() {}

    public JobExecutionResponse(Long executionId, String jobName, String status,
                                Instant startTime, Instant endTime, String exitDescription) {
        this.executionId = executionId;
        this.jobName = jobName;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.exitDescription = exitDescription;
    }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getExitDescription() { return exitDescription; }
    public void setExitDescription(String exitDescription) { this.exitDescription = exitDescription; }
}
