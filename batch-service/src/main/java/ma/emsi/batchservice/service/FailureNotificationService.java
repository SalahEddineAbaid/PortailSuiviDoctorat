package ma.emsi.batchservice.service;

import ma.emsi.batchservice.dto.event.JobFailureEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for handling batch job failure notifications.
 * 
 * Responsibilities:
 * - Publish Kafka events on job failure
 * - Send email alerts to technical team
 * - Include detailed error information
 * - Log all failure notifications
 * 
 * Notification channels:
 * 1. Kafka event to notifications topic (for system-wide alerting)
 * 2. Email to technical admin team (for immediate attention)
 * 3. Application logs (for debugging and audit trail)
 * 
 * Requirements: 8.8, 8.9
 */
@Service
public class FailureNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FailureNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JavaMailSender mailSender;

    @Value("${batch.notifications.kafka.topic:notifications}")
    private String notificationsTopic;

    @Value("${batch.notifications.email.from:batch-service@emsi.ma}")
    private String fromEmail;

    @Value("${batch.notifications.email.technical-team}")
    private String[] technicalTeamEmails;

    @Value("${batch.notifications.email.enabled:true}")
    private boolean emailNotificationsEnabled;

    @Value("${batch.notifications.kafka.enabled:true}")
    private boolean kafkaNotificationsEnabled;

    public FailureNotificationService(
            KafkaTemplate<String, Object> kafkaTemplate,
            JavaMailSender mailSender) {
        this.kafkaTemplate = kafkaTemplate;
        this.mailSender = mailSender;
    }

    /**
     * Notifies about a job failure through all configured channels.
     * 
     * @param jobName     Name of the failed job
     * @param executionId Job execution ID
     * @param failureTime Time of failure
     * @param exception   Exception that caused the failure
     * @param exitMessage Exit message from the job
     */
    public void notifyJobFailure(
            String jobName,
            Long executionId,
            LocalDateTime failureTime,
            Throwable exception,
            String exitMessage) {

        logger.error("Job failure detected: {} (execution ID: {})", jobName, executionId, exception);

        // Build failure event DTO
        JobFailureEventDTO failureEvent = buildFailureEvent(
                jobName, executionId, failureTime, exception, exitMessage);

        // Publish Kafka event
        if (kafkaNotificationsEnabled) {
            publishKafkaEvent(failureEvent);
        }

        // Send email alert
        if (emailNotificationsEnabled) {
            sendEmailAlert(failureEvent);
        }
    }

    /**
     * Builds a JobFailureEventDTO from failure details.
     */
    private JobFailureEventDTO buildFailureEvent(
            String jobName,
            Long executionId,
            LocalDateTime failureTime,
            Throwable exception,
            String exitMessage) {

        JobFailureEventDTO event = new JobFailureEventDTO();
        event.setJobName(jobName);
        event.setExecutionId(executionId);
        event.setFailureTime(failureTime);
        event.setExceptionType(exception != null ? exception.getClass().getSimpleName() : "Unknown");
        event.setExceptionMessage(exception != null ? exception.getMessage() : "No exception message");
        event.setStackTrace(getStackTraceAsString(exception));
        event.setExitMessage(exitMessage);
        event.setPriority("URGENT");
        event.setNotificationType("JOB_FAILURE");

        return event;
    }

    /**
     * Publishes a failure event to Kafka notifications topic.
     */
    private void publishKafkaEvent(JobFailureEventDTO failureEvent) {
        try {
            kafkaTemplate.send(notificationsTopic, failureEvent.getJobName(), failureEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to publish Kafka failure event for job {}: {}",
                                    failureEvent.getJobName(), ex.getMessage());
                        } else {
                            logger.info("Published Kafka failure event for job {} to topic {}",
                                    failureEvent.getJobName(), notificationsTopic);
                        }
                    });
        } catch (Exception e) {
            logger.error("Exception while publishing Kafka failure event: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends an email alert to the technical team.
     */
    private void sendEmailAlert(JobFailureEventDTO failureEvent) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(technicalTeamEmails);
            message.setSubject(String.format("[URGENT] Batch Job Failure: %s", failureEvent.getJobName()));
            message.setText(buildEmailBody(failureEvent));

            mailSender.send(message);
            logger.info("Sent failure email alert for job {} to technical team", failureEvent.getJobName());
        } catch (Exception e) {
            logger.error("Failed to send failure email alert for job {}: {}",
                    failureEvent.getJobName(), e.getMessage(), e);
        }
    }

    /**
     * Builds the email body with detailed failure information.
     */
    private String buildEmailBody(JobFailureEventDTO failureEvent) {
        StringBuilder body = new StringBuilder();
        body.append("BATCH JOB FAILURE ALERT\n");
        body.append("======================\n\n");
        body.append("Job Name: ").append(failureEvent.getJobName()).append("\n");
        body.append("Execution ID: ").append(failureEvent.getExecutionId()).append("\n");
        body.append("Failure Time: ").append(failureEvent.getFailureTime().format(FORMATTER)).append("\n");
        body.append("Priority: ").append(failureEvent.getPriority()).append("\n\n");

        body.append("ERROR DETAILS\n");
        body.append("-------------\n");
        body.append("Exception Type: ").append(failureEvent.getExceptionType()).append("\n");
        body.append("Exception Message: ").append(failureEvent.getExceptionMessage()).append("\n\n");

        if (failureEvent.getExitMessage() != null && !failureEvent.getExitMessage().isEmpty()) {
            body.append("Exit Message: ").append(failureEvent.getExitMessage()).append("\n\n");
        }

        body.append("STACK TRACE\n");
        body.append("-----------\n");
        body.append(failureEvent.getStackTrace()).append("\n\n");

        body.append("ACTION REQUIRED\n");
        body.append("---------------\n");
        body.append("Please investigate this failure immediately and take corrective action.\n");
        body.append("Check the batch-service logs for more details.\n");
        body.append("You can manually retry the job via the REST API if needed.\n\n");

        body.append("This is an automated alert from the Batch Service.\n");

        return body.toString();
    }

    /**
     * Converts exception stack trace to string.
     */
    private String getStackTraceAsString(Throwable exception) {
        if (exception == null) {
            return "No stack trace available";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        // Limit stack trace to first 50 lines to avoid overly long emails
        String fullStackTrace = sw.toString();
        String[] lines = fullStackTrace.split("\n");
        if (lines.length > 50) {
            StringBuilder truncated = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                truncated.append(lines[i]).append("\n");
            }
            truncated.append("... (truncated, see logs for full stack trace)");
            return truncated.toString();
        }

        return fullStackTrace;
    }

    /**
     * Notifies about a step failure within a job.
     * 
     * @param jobName     Name of the job
     * @param stepName    Name of the failed step
     * @param executionId Job execution ID
     * @param exception   Exception that caused the failure
     */
    public void notifyStepFailure(
            String jobName,
            String stepName,
            Long executionId,
            Throwable exception) {

        logger.error("Step failure detected: {} in job {} (execution ID: {})",
                stepName, jobName, executionId, exception);

        String exitMessage = String.format("Step '%s' failed in job '%s'", stepName, jobName);
        notifyJobFailure(jobName, executionId, LocalDateTime.now(), exception, exitMessage);
    }
}
