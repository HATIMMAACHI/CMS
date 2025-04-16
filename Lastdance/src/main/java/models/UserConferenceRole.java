package models;

import java.util.Objects;

public class UserConferenceRole {

    // Doit correspondre aux valeurs ENUM dans la DB
    public enum Role { AUTHOR, PC_MEMBER, SC_MEMBER, CHAIR, STEERING_COMMITTEE }

    private int userId;
    private int conferenceId;
    private Role role;

    public UserConferenceRole() {}

    public UserConferenceRole(int userId, int conferenceId, Role role) {
        this.userId = userId;
        this.conferenceId = conferenceId;
        this.role = role;
    }

    // --- Getters and Setters ---
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getConferenceId() { return conferenceId; }
    public void setConferenceId(int conferenceId) { this.conferenceId = conferenceId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }


    // La clé primaire est composite (userId, conferenceId, role)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserConferenceRole that = (UserConferenceRole) o;
        return userId == that.userId &&
               conferenceId == that.conferenceId &&
               role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, conferenceId, role);
    }

    @Override
    public String toString() {
        return "UserConferenceRole{" +
               "userId=" + userId +
               ", conferenceId=" + conferenceId +
               ", role=" + role +
               '}';
    }
}