package models;



import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

public class Conference {

    public enum ConferenceType { PRESENTIAL, VIRTUAL, HYBRID }

    private int conferenceId;
    private String name;
    private String acronym;
    private String website;
    private ConferenceType type;
    private Date startDate;
    private Date endDate;
    private String location;
    private String description;
    private String logoPath;
    private Timestamp submissionDeadline;
    private Timestamp reviewDeadline;
    private Timestamp notificationDate;
    private Timestamp cameraReadyDeadline;
    private int createdByUserId; // ID de l'utilisateur (Président) qui a créé
    private Timestamp createdAt;

    public Conference() {}

    // --- Getters and Setters pour tous les champs ---
    // (Générez-les avec votre IDE ou écrivez-les manuellement)
    // Exemple :
    public int getConferenceId() { return conferenceId; }
    public void setConferenceId(int conferenceId) { this.conferenceId = conferenceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... etc pour tous les autres champs ...

    public ConferenceType getType() { return type; }
    public void setType(ConferenceType type) { this.type = type; }
    // ... continuez pour tous les getters/setters ...


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conference that = (Conference) o;
        return conferenceId == that.conferenceId || Objects.equals(acronym, that.acronym);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conferenceId, acronym);
    }

    @Override
    public String toString() {
        return "Conference{" +
               "conferenceId=" + conferenceId +
               ", name='" + name + '\'' +
               ", acronym='" + acronym + '\'' +
               '}';
    }
     // --- GETTERS ET SETTERS COMPLETS ---
    public String getAcronym() { return acronym; }
    public void setAcronym(String acronym) { this.acronym = acronym; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public Timestamp getSubmissionDeadline() { return submissionDeadline; }
    public void setSubmissionDeadline(Timestamp submissionDeadline) { this.submissionDeadline = submissionDeadline; }
    public Timestamp getReviewDeadline() { return reviewDeadline; }
    public void setReviewDeadline(Timestamp reviewDeadline) { this.reviewDeadline = reviewDeadline; }
    public Timestamp getNotificationDate() { return notificationDate; }
    public void setNotificationDate(Timestamp notificationDate) { this.notificationDate = notificationDate; }
    public Timestamp getCameraReadyDeadline() { return cameraReadyDeadline; }
    public void setCameraReadyDeadline(Timestamp cameraReadyDeadline) { this.cameraReadyDeadline = cameraReadyDeadline; }
    public int getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(int createdByUserId) { this.createdByUserId = createdByUserId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
