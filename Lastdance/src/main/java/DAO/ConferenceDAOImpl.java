package DAO;

import models.Conference;
import models.Conference.ConferenceType;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConferenceDAOImpl {

    private static final String SQL_INSERT_CONFERENCE = "INSERT INTO conferences (name, acronym, website, type, start_date, end_date, location, description, logo_path, submission_deadline, review_deadline, notification_date, camera_ready_deadline, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
    private static final String SQL_SELECT_CONFERENCE_BY_ID = "SELECT * FROM conferences WHERE conference_id = ?";
    private static final String SQL_SELECT_CONFERENCE_BY_ACRONYM = "SELECT * FROM conferences WHERE acronym = ?";
    private static final String SQL_SELECT_ALL_CONFERENCES = "SELECT * FROM conferences ORDER BY start_date DESC, name";
    private static final String SQL_UPDATE_CONFERENCE = "UPDATE conferences SET name = ?, acronym = ?, website = ?, type = ?, start_date = ?, end_date = ?, location = ?, description = ? WHERE conference_id = ?";
    private static final String SQL_UPDATE_DEADLINES = "UPDATE conferences SET submission_deadline = ?, review_deadline = ?, notification_date = ?, camera_ready_deadline = ? WHERE conference_id = ?";
    private static final String SQL_UPDATE_LOGO_PATH = "UPDATE conferences SET logo_path = ? WHERE conference_id = ?";
    private static final String SQL_DELETE_CONFERENCE = "DELETE FROM conferences WHERE conference_id = ?";
    private static final String SQL_SELECT_CONFERENCES_BY_USER_ID = "SELECT c.* FROM conferences c JOIN user_conference_roles ucr ON c.conference_id = ucr.conference_id WHERE ucr.user_id = ? GROUP BY c.conference_id ORDER BY c.start_date DESC";
    private static final String SQL_SELECT_USER_CONFERENCES_AND_ROLES = "SELECT c.*, GROUP_CONCAT(ucr.role SEPARATOR ',') as roles FROM conferences c JOIN user_conference_roles ucr ON c.conference_id = ucr.conference_id WHERE ucr.user_id = ? GROUP BY c.conference_id ORDER BY c.start_date DESC";


    private Conference mapResultSetToConference(ResultSet rs) throws SQLException {
        Conference conf = new Conference();
        conf.setConferenceId(rs.getInt("conference_id"));
        conf.setName(rs.getString("name"));
        conf.setAcronym(rs.getString("acronym"));
        conf.setWebsite(rs.getString("website"));
        // Gérer l'ENUM Type
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            try {
                conf.setType(ConferenceType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                System.err.println("Type de conférence invalide dans la DB: " + typeStr);
                // Gérer l'erreur - peut-être définir un type par défaut ou laisser null?
            }
        }
        conf.setStartDate(rs.getDate("start_date"));
        conf.setEndDate(rs.getDate("end_date"));
        conf.setLocation(rs.getString("location"));
        conf.setDescription(rs.getString("description"));
        conf.setLogoPath(rs.getString("logo_path"));
        conf.setSubmissionDeadline(rs.getTimestamp("submission_deadline"));
        conf.setReviewDeadline(rs.getTimestamp("review_deadline"));
        conf.setNotificationDate(rs.getTimestamp("notification_date"));
        conf.setCameraReadyDeadline(rs.getTimestamp("camera_ready_deadline"));
        conf.setCreatedByUserId(rs.getInt("created_by"));
        conf.setCreatedAt(rs.getTimestamp("created_at"));
        return conf;
    }


    public void createConference(Conference conference) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_CONFERENCE, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, conference.getName());
            pstmt.setString(2, conference.getAcronym());
            pstmt.setString(3, conference.getWebsite());
            pstmt.setString(4, conference.getType() != null ? conference.getType().name() : null);
            pstmt.setDate(5, conference.getStartDate());
            pstmt.setDate(6, conference.getEndDate());
            pstmt.setString(7, conference.getLocation());
            pstmt.setString(8, conference.getDescription());
            pstmt.setString(9, conference.getLogoPath());
            pstmt.setTimestamp(10, conference.getSubmissionDeadline());
            pstmt.setTimestamp(11, conference.getReviewDeadline());
            pstmt.setTimestamp(12, conference.getNotificationDate());
            pstmt.setTimestamp(13, conference.getCameraReadyDeadline());
            // Handle potential null createdByUserId if needed, depends on logic
             if (conference.getCreatedByUserId() > 0) {
                 pstmt.setInt(14, conference.getCreatedByUserId());
             } else {
                 pstmt.setNull(14, Types.INTEGER); // Ou une valeur par défaut si la colonne l'accepte
             }


            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la conférence a échoué.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    conference.setConferenceId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de la conférence a échoué, aucun ID obtenu.");
                }
            }
        }
    }

    public Optional<Conference> findConferenceById(int conferenceId) throws SQLException {
        Conference conf = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_CONFERENCE_BY_ID)) {
            pstmt.setInt(1, conferenceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    conf = mapResultSetToConference(rs);
                }
            }
        }
        return Optional.ofNullable(conf);
    }

     public Optional<Conference> findConferenceByAcronym(String acronym) throws SQLException {
        Conference conf = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_CONFERENCE_BY_ACRONYM)) {
            pstmt.setString(1, acronym);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    conf = mapResultSetToConference(rs);
                }
            }
        }
        return Optional.ofNullable(conf);
    }


    public List<Conference> findAllConferences() throws SQLException {
        List<Conference> conferences = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ALL_CONFERENCES);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                conferences.add(mapResultSetToConference(rs));
            }
        }
        return conferences;
    }

    public boolean updateConference(Conference conference) throws SQLException {
         if (conference.getConferenceId() <= 0) {
             throw new IllegalArgumentException("Conference ID invalide pour la mise à jour.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_CONFERENCE)) {
            pstmt.setString(1, conference.getName());
            pstmt.setString(2, conference.getAcronym());
            pstmt.setString(3, conference.getWebsite());
            pstmt.setString(4, conference.getType() != null ? conference.getType().name() : null);
            pstmt.setDate(5, conference.getStartDate());
            pstmt.setDate(6, conference.getEndDate());
            pstmt.setString(7, conference.getLocation());
            pstmt.setString(8, conference.getDescription());
            pstmt.setInt(9, conference.getConferenceId()); // WHERE clause

            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

    public boolean updateConferenceDeadlines(int conferenceId, Timestamp submissionDeadline, Timestamp reviewDeadline, Timestamp notificationDate, Timestamp cameraReadyDeadline) throws SQLException {
         if (conferenceId <= 0) {
             throw new IllegalArgumentException("Conference ID invalide pour la mise à jour des deadlines.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_DEADLINES)) {
            pstmt.setTimestamp(1, submissionDeadline);
            pstmt.setTimestamp(2, reviewDeadline);
            pstmt.setTimestamp(3, notificationDate);
            pstmt.setTimestamp(4, cameraReadyDeadline);
            pstmt.setInt(5, conferenceId); // WHERE clause

            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

     public boolean updateConferenceLogoPath(int conferenceId, String logoPath) throws SQLException {
         if (conferenceId <= 0) {
             throw new IllegalArgumentException("Conference ID invalide pour la mise à jour du logo.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_LOGO_PATH)) {
            pstmt.setString(1, logoPath); // Peut être null si on supprime le logo
            pstmt.setInt(2, conferenceId); // WHERE clause

            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }


    public boolean deleteConference(int conferenceId) throws SQLException {
         if (conferenceId <= 0) {
             throw new IllegalArgumentException("Conference ID invalide pour la suppression.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_CONFERENCE)) {
            pstmt.setInt(1, conferenceId);
            affectedRows = pstmt.executeUpdate();
        }
        // Gérer les exceptions de contrainte si nécessaire
        return affectedRows > 0;
    }

    public List<Conference> findConferencesByUserId(int userId) throws SQLException {
        List<Conference> conferences = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_CONFERENCES_BY_USER_ID)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    conferences.add(mapResultSetToConference(rs));
                }
            }
        }
        return conferences;
    }

    public Map<Conference, List<String>> findUserConferencesAndRoles(int userId) throws SQLException {
        Map<Conference, List<String>> confRolesMap = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_USER_CONFERENCES_AND_ROLES)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Conference conf = mapResultSetToConference(rs);
                    String rolesString = rs.getString("roles"); // Récupère "AUTHOR,PC_MEMBER"
                    List<String> rolesList = new ArrayList<>();
                    if (rolesString != null && !rolesString.isEmpty()) {
                        rolesList = List.of(rolesString.split(",")); // Convertit en liste
                    }
                    confRolesMap.put(conf, rolesList);
                }
            }
        }
        return confRolesMap;
    }
}