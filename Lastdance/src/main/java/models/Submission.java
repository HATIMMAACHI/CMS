package models;

import java.sql.Timestamp;
import java.util.Objects;

public class Submission {

    public enum SubmissionStatus { SUBMITTED, UNDER_REVIEW, REJECTED, ACCEPTED, PENDING_REVISION }

    private int submissionId;
    private int conferenceId;
    private String title;
    private String abstractText; // 'abstract' est un mot clé Java
    private String keywords;
    private String filePath;
    private Timestamp submissionDate;
    private Timestamp lastUpdated;
    private SubmissionStatus status;
    private String uniquePaperId; // UUID

    public Submission() {}

    // --- Getters and Setters ---
    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }
    public int getConferenceId() { return conferenceId; }
    public void setConferenceId(int conferenceId) { this.conferenceId = conferenceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Timestamp getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(Timestamp submissionDate) { this.submissionDate = submissionDate; }
    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public String getUniquePaperId() { return uniquePaperId; }
    public void setUniquePaperId(String uniquePaperId) { this.uniquePaperId = uniquePaperId; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Submission that = (Submission) o;
        return submissionId == that.submissionId || Objects.equals(uniquePaperId, that.uniquePaperId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submissionId, uniquePaperId);
    }

    @Override
    public String toString() {
        return "Submission{" +
               "submissionId=" + submissionId +
               ", title='" + title + '\'' +
               ", status=" + status +
               '}';
    }
}