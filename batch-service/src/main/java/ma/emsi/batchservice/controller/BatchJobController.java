package ma.emsi.batchservice.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.emsi.batchservice.dto.*;
import ma.emsi.batchservice.service.BatchJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for manual batch job execution and monitoring.
 * All endpoints require ROLE_ADMIN authorization.
 * Job trigger endpoint is rate-limited to prevent abuse.
 */
@RestController
@RequestMapping("/api/batch/jobs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Batch Job Management", description = "APIs for manual batch job execution, monitoring, and statistics")
@SecurityRequirement(name = "Bearer Authentication")
public class BatchJobController {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobController.class);

    private final BatchJobService batchJobService;

    public BatchJobController(BatchJobService batchJobService) {
        this.batchJobService = batchJobService;
    }

    /**
     * Manually trigger a batch job execution.
     * Rate-limited to prevent abuse.
     *
     * @param jobName Name of the job to trigger
     * @return Job execution details including ID, status, and start time
     */
    @Operation(summary = "Trigger a batch job manually", description = """
            Manually triggers execution of a specified batch job. This endpoint is rate-limited
            to 10 requests per minute per user to prevent abuse.

            **Available Jobs:**
            - `tokenCleanupJob` - Remove expired tokens
            - `dureeDoctoratAlertJob` - Check doctoral duration thresholds
            - `monthlyReportJob` - Generate monthly statistical report
            - `archiveJob` - Archive old records
            - `dataConsistencyJob` - Verify data consistency
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job triggered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JobTriggerResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "executionId": 12345,
                      "jobName": "tokenCleanupJob",
                      "status": "STARTED",
                      "startTime": "2024-01-15T02:00:00",
                      "message": "Job started successfully"
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid job name", content = @Content),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded (max 10 requests per minute)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during job execution", content = @Content)
    })
    @PostMapping("/{jobName}/run")
    @RateLimiter(name = "jobTrigger", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<JobTriggerResponseDTO> triggerJob(
            @Parameter(description = "Name of the batch job to trigger", required = true, example = "tokenCleanupJob") @PathVariable String jobName) {
        logger.info("Received request to trigger job: {}", jobName);

        try {
            JobTriggerResponseDTO response = batchJobService.triggerJob(jobName);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid job name: {}", jobName);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to trigger job {}: {}", jobName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * List all available batch jobs with metadata.
     *
     * @return List of jobs with name, description, CRON expression, last execution,
     *         and status
     */
    @Operation(summary = "List all batch jobs", description = "Retrieves a list of all available batch jobs with their metadata including name, description, CRON schedule, last execution time, and current status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved job list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JobInfoDTO.class), examples = @ExampleObject(value = """
                    [
                      {
                        "name": "tokenCleanupJob",
                        "description": "Remove expired authentication and password reset tokens",
                        "cronExpression": "0 0 2 * * ?",
                        "lastExecution": "2024-01-15T02:00:00",
                        "lastStatus": "COMPLETED",
                        "isRunning": false
                      }
                    ]
                    """))),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<JobInfoDTO>> listJobs() {
        logger.debug("Received request to list all jobs");

        try {
            List<JobInfoDTO> jobs = batchJobService.listJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Failed to list jobs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get paginated execution history for a specific job.
     *
     * @param jobName Job name
     * @param page    Page number (default: 0)
     * @param size    Page size (default: 20)
     * @return Paginated list of job executions
     */
    @Operation(summary = "Get job execution history", description = "Retrieves paginated execution history for a specific batch job, including execution details, status, duration, and items processed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved execution history", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JobExecutionDTO.class), examples = @ExampleObject(value = """
                    [
                      {
                        "executionId": 12345,
                        "jobName": "tokenCleanupJob",
                        "status": "COMPLETED",
                        "startTime": "2024-01-15T02:00:00",
                        "endTime": "2024-01-15T02:05:23",
                        "duration": "5m 23s",
                        "itemsProcessed": 1523,
                        "itemsFailed": 0,
                        "exitMessage": "Job completed successfully"
                      }
                    ]
                    """))),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/{jobName}/executions")
    public ResponseEntity<List<JobExecutionDTO>> getJobExecutions(
            @Parameter(description = "Name of the batch job", required = true, example = "tokenCleanupJob") @PathVariable String jobName,
            @Parameter(description = "Page number (zero-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        logger.debug("Received request for execution history: job={}, page={}, size={}", jobName, page, size);

        try {
            List<JobExecutionDTO> executions = batchJobService.getJobExecutionHistory(jobName, page, size);
            return ResponseEntity.ok(executions);
        } catch (Exception e) {
            logger.error("Failed to retrieve execution history for job {}: {}", jobName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get detailed execution information for a specific execution ID.
     *
     * @param executionId Execution ID
     * @return Detailed execution information
     */
    @Operation(summary = "Get execution details", description = "Retrieves detailed information for a specific job execution by execution ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved execution details", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JobExecutionDTO.class))),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Execution not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<JobExecutionDTO> getExecutionDetails(
            @Parameter(description = "Job execution ID", required = true, example = "12345") @PathVariable Long executionId) {
        logger.debug("Received request for execution details: executionId={}", executionId);

        try {
            JobExecutionDTO execution = batchJobService.getExecutionDetails(executionId);
            return ResponseEntity.ok(execution);
        } catch (RuntimeException e) {
            logger.error("Execution not found: {}", executionId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to retrieve execution details for {}: {}", executionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stop a running job execution.
     *
     * @param executionId Execution ID to stop
     * @return Confirmation message
     */
    @Operation(summary = "Stop a running job execution", description = "Stops a currently running batch job execution. The job will complete its current chunk and then stop gracefully.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job execution stopped successfully", content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Job execution 12345 stopped successfully"))),
            @ApiResponse(responseCode = "400", description = "Execution is not running or cannot be stopped", content = @Content),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Execution not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/executions/{executionId}/stop")
    public ResponseEntity<String> stopJobExecution(
            @Parameter(description = "Job execution ID to stop", required = true, example = "12345") @PathVariable Long executionId) {
        logger.info("Received request to stop execution: {}", executionId);

        try {
            String message = batchJobService.stopJobExecution(executionId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            logger.error("Failed to stop execution {}: {}", executionId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error stopping execution {}: {}", executionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get global statistics across all batch jobs.
     *
     * @return Global statistics including total executions, success rate, average
     *         duration, etc.
     */
    @Operation(summary = "Get global batch job statistics", description = "Retrieves global statistics across all batch jobs including total executions, success rate, average duration, and last failure details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved global statistics", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GlobalStatsDTO.class), examples = @ExampleObject(value = """
                    {
                      "totalExecutions": 1523,
                      "successfulExecutions": 1498,
                      "failedExecutions": 25,
                      "successRate": 98.36,
                      "averageDuration": "4m 32s",
                      "lastFailure": {
                        "jobName": "archiveJob",
                        "executionId": 12340,
                        "failureTime": "2024-01-14T03:00:00",
                        "errorMessage": "Disk space exhausted"
                      }
                    }
                    """))),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("/stats")
    public ResponseEntity<GlobalStatsDTO> getGlobalStatistics() {
        logger.debug("Received request for global statistics");

        try {
            GlobalStatsDTO stats = batchJobService.getGlobalStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to calculate global statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Fallback method for rate limiting.
     * Returns 429 Too Many Requests when rate limit is exceeded.
     */
    public ResponseEntity<JobTriggerResponseDTO> rateLimitFallback(String jobName, Exception e) {
        logger.warn("Rate limit exceeded for job trigger: {}", jobName);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
