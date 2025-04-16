package DAO;

import models.Submission;
import models.Submission.SubmissionStatus;

import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubmissionDAOImpl  {

    private static final String SQL_INSERT_SUBMISSION = "INSERT INTO submissions (conference_id, title, abstract, keywords, file_path, submission_date, status, unique_paper_id) VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)";
    private static final String SQL_SELECT_SUBMISSION_BY_ID = "SELECT * FROM submissions WHERE submission_id = ?";
    private static final String SQL_SELECT_SUBMISSION_BY_UNIQUE_ID = "SELECT * FROM submissions WHERE unique_paper_id = ?";
    private static final String SQL_SELECT_SUBMISSIONS_BY_CONF = "SELECT * FROM submissions WHERE conference_id = ? ORDER BY submission_date DESC";
    private static final String SQL_SELECT_SUBMISSIONS_BY_AUTHOR = "SELECT s.* FROM submissions s JOIN submission_authors sa ON s.submission_id = sa.submission_id WHERE s.conference_id = ? AND sa.user_id = ? ORDER BY s.submission_date DESC";
    private static final String SQL_UPDATE_SUBMISSION_DETAILS = "UPDATE submissions SET title = ?, abstract = ?, keywords = ?, file_path = ?, last_updated = NOW() WHERE submission_id = ?";
    private static final String SQL_UPDATE_SUBMISSION_STATUS = "UPDATE submissions SET status = ?, last_updated = NOW() WHERE submission_id = ?";
    private static final String SQL_DELETE_SUBMISSION = "DELETE FROM submissions WHERE submission_id = ?";
    private static final String SQL_SELECT_SUBMISSIONS_BY_CONF_STATUS = "SELECT * FROM submissions WHERE conference_id = ? AND status = ? ORDER BY submission_date DESC";


     private Submission mapResultSetToSubmission(ResultSet rs) throws SQLException {
        Submission sub = new Submission();
        sub.setSubmissionId(rs.getInt("submission_id"));
        sub.setConferenceId(rs.getInt("conference_id"));
        sub.setTitle(rs.getString("title"));
        sub.setAbstractText(rs.getString("abstract")); // Nom de la colonne "abstract"
        sub.setKeywords(rs.getString("keywords"));
        sub.setFilePath(rs.getString("file_path"));
        sub.setSubmissionDate(rs.getTimestamp("submission_date"));
        sub.setLastUpdated(rs.getTimestamp("last_updated"));
        try {
            sub.setStatus(SubmissionStatus.valueOf(rs.getString("status")));
        } catch (Exception e) {
             System.err.println("Statut de soumission invalide dans la DB: " + rs.getString("status"));
        }
        sub.setUniquePaperId(rs.getString("unique_paper_id"));
        return sub;
    }

 
    public void createSubmission(Submission submission) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_SUBMISSION, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, submission.getConferenceId());
            pstmt.setString(2, submission.getTitle());
            pstmt.setString(3, submission.getAbstractText());
            pstmt.setString(4, submission.getKeywords());
            pstmt.setString(5, submission.getFilePath());
            pstmt.setString(6, submission.getStatus() != null ? submission.getStatus().name() : SubmissionStatus.SUBMITTED.name()); // Statut par défaut
            pstmt.setString(7, submission.getUniquePaperId()); // UUID doit être généré avant

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la soumission a échoué.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    submission.setSubmissionId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de la soumission a échoué, aucun ID obtenu.");
                }
            }
        }
    }


    public Optional<Submission> findSubmissionById(int submissionId) throws SQLException {
        Submission sub = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_SUBMISSION_BY_ID)) {
            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    sub = mapResultSetToSubmission(rs);
                }
            }
        }
        return Optional.ofNullable(sub);
    }


    public Optional<Submission> findSubmissionByUniqueId(String uniquePaperId) throws SQLException {
        Submission sub = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_SUBMISSION_BY_UNIQUE_ID)) {
            pstmt.setString(1, uniquePaperId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    sub = mapResultSetToSubmission(rs);
                }
            }
        }
        return Optional.ofNullable(sub);
    }



    public List<Submission> findSubmissionsByConference(int conferenceId) throws SQLException {
        List<Submission> submissions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_SUBMISSIONS_BY_CONF)) {
            pstmt.setInt(1, conferenceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapResultSetToSubmission(rs));
                }
            }
        }
        return submissions;
    }


    public List<Submission> findSubmissionsByAuthor(int conferenceId, int authorId) throws SQLException {
        List<Submission> submissions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_SUBMISSIONS_BY_AUTHOR)) {
            pstmt.setInt(1, conferenceId);
            pstmt.setInt(2, authorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapResultSetToSubmission(rs));
                }
            }
        }
        return submissions;
    }


    public boolean updateSubmissionDetails(Submission submission) throws SQLException {
         if (submission.getSubmissionId() <= 0) {
             throw new IllegalArgumentException("Submission ID invalide pour la mise à jour.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_SUBMISSION_DETAILS)) {
            pstmt.setString(1, submission.getTitle());
            pstmt.setString(2, submission.getAbstractText());
            pstmt.setString(3, submission.getKeywords());
            pstmt.setString(4, submission.getFilePath());
            pstmt.setInt(5, submission.getSubmissionId()); // WHERE clause
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

  
    public boolean updateSubmissionStatus(int submissionId, SubmissionStatus newStatus) throws SQLException {
         if (submissionId <= 0) {
             throw new IllegalArgumentException("Submission ID invalide pour la mise à jour du statut.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_SUBMISSION_STATUS)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, submissionId); // WHERE clause
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }


    public boolean deleteSubmission(int submissionId) throws SQLException {
         if (submissionId <= 0) {
             throw new IllegalArgumentException("Submission ID invalide pour la suppression.");
         }
        int affectedRows = 0;
        // !! Important : Supprimer d'abord les dépendances (auteurs, assignations, reviews) !!
        // ou configurer ON DELETE CASCADE dans la DB.
        // Ici, on suppose que c'est géré par CASCADE ou logiquement avant cet appel.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_SUBMISSION)) {
            pstmt.setInt(1, submissionId);
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

    public List<Submission> findSubmissionsByConferenceAndStatus(int conferenceId, SubmissionStatus status) throws SQLException {
        List<Submission> submissions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_SUBMISSIONS_BY_CONF_STATUS)) {
            pstmt.setInt(1, conferenceId);
            pstmt.setString(2, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapResultSetToSubmission(rs));
                }
            }
        }
        return submissions;
    }
}