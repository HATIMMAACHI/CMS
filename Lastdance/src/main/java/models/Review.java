package models;

import java.sql.Timestamp;
import java.util.Objects;

public class Review {

    public enum Recommendation { STRONG_ACCEPT, ACCEPT, WEAK_ACCEPT, BORDERLINE, WEAK_REJECT, REJECT, STRONG_REJECT }
    public enum Confidence { EXPERT, HIGH, MEDIUM, LOW, NONE }

    private int reviewId;
    private int assignmentId;
    private int submissionId;
    private int reviewerId; // User ID du PC Member
    private String commentsToAuthor;
    private String commentsToSc;
    private Recommendation recommendation;
    private Confidence confidence;
    private Timestamp reviewDate;

    public Review() {}

    // --- Getters and Setters ---
    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }
    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }
    public int getReviewerId() { return reviewerId; }
    public void setReviewerId(int reviewerId) { this.reviewerId = reviewerId; }
    public String getCommentsToAuthor() { return commentsToAuthor; }
    public void setCommentsToAuthor(String commentsToAuthor) { this.commentsToAuthor = commentsToAuthor; }
    public String getCommentsToSc() { return commentsToSc; }
    public void setCommentsToSc(String commentsToSc) { this.commentsToSc = commentsToSc; }
    public Recommendation getRecommendation() { return recommendation; }
    public void setRecommendation(Recommendation recommendation) { this.recommendation = recommendation; }
    public Confidence getConfidence() { return confidence; }
    public void setConfidence(Confidence confidence) { this.confidence = confidence; }
    public Timestamp getReviewDate() { return reviewDate; }
    public void setReviewDate(Timestamp reviewDate) { this.reviewDate = reviewDate; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return reviewId == review.reviewId || assignmentId == review.assignmentId; // assignmentId est aussi unique
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewId, assignmentId);
    }
}