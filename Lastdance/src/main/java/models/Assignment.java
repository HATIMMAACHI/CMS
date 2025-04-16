package models;



import java.sql.Timestamp;
import java.util.Objects;

public class Assignment {

    public enum AssignmentStatus { PENDING, ACCEPTED, DECLINED, COMPLETED }

    private int assignmentId;
    private int submissionId;
    private int pcMemberId; // User ID du PC Member
    private int assignedById; // User ID du SC Member (peut être 0 si non tracké ou système)
    private Timestamp assignmentDate;
    private AssignmentStatus status;

    public Assignment() {}

    // --- Getters and Setters ---
    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }
    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }
    public int getPcMemberId() { return pcMemberId; }
    public void setPcMemberId(int pcMemberId) { this.pcMemberId = pcMemberId; }
    public int getAssignedById() { return assignedById; }
    public void setAssignedById(int assignedById) { this.assignedById = assignedById; }
    public Timestamp getAssignmentDate() { return assignmentDate; }
    public void setAssignmentDate(Timestamp assignmentDate) { this.assignmentDate = assignmentDate; }
    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return assignmentId == that.assignmentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }
}
