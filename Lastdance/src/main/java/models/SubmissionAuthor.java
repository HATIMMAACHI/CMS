package models;



import java.util.Objects;

public class SubmissionAuthor {

    private int submissionId;
    private int userId;
    private boolean isCorresponding;
    private int authorOrder;

    public SubmissionAuthor() {}

    public SubmissionAuthor(int submissionId, int userId, boolean isCorresponding, int authorOrder) {
        this.submissionId = submissionId;
        this.userId = userId;
        this.isCorresponding = isCorresponding;
        this.authorOrder = authorOrder;
    }

    // --- Getters and Setters ---
    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public boolean isCorresponding() { return isCorresponding; }
    public void setCorresponding(boolean corresponding) { isCorresponding = corresponding; }
    public int getAuthorOrder() { return authorOrder; }
    public void setAuthorOrder(int authorOrder) { this.authorOrder = authorOrder; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubmissionAuthor that = (SubmissionAuthor) o;
        return submissionId == that.submissionId && userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(submissionId, userId);
    }
}