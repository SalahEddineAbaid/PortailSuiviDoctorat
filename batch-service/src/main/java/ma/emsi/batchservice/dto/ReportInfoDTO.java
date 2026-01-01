package ma.emsi.batchservice.dto;

import java.time.LocalDateTime;

/**
 * DTO for monthly report information.
 * Contains metadata about generated reports including download links.
 */
public class ReportInfoDTO {
    private String fileName;
    private LocalDateTime generatedDate;
    private Long fileSizeBytes;
    private String downloadUrl;

    public ReportInfoDTO() {
    }

    public ReportInfoDTO(String fileName, LocalDateTime generatedDate, Long fileSizeBytes, String downloadUrl) {
        this.fileName = fileName;
        this.generatedDate = generatedDate;
        this.fileSizeBytes = fileSizeBytes;
        this.downloadUrl = downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDateTime generatedDate) {
        this.generatedDate = generatedDate;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
