package DAO;

import models.SubmissionAuthor;
import models.User; // Nécessaire pour findAuthorsBySubmissionId

import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubmissionAuthorDAOImpl  {

    private static final String SQL_INSERT_AUTHOR = "INSERT INTO submission_authors (submission_id, user_id, is_corresponding, author_order) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE is_corresponding=VALUES(is_corresponding), author_order=VALUES(author_order)"; // Gère l'insertion ou la mise à jour simple
    private static final String SQL_DELETE_AUTHOR = "DELETE FROM submission_authors WHERE submission_id = ? AND user_id = ?";
    // Jointure pour récupérer les détails de l'utilisateur
    private static final String SQL_SELECT_AUTHORS_BY_SUBMISSION = "SELECT u.*, sa.is_corresponding, sa.author_order FROM users u JOIN submission_authors sa ON u.user_id = sa.user_id WHERE sa.submission_id = ? ORDER BY sa.author_order ASC";
    private static final String SQL_SELECT_LINKS_BY_SUBMISSION = "SELECT submission_id, user_id, is_corresponding, author_order FROM submission_authors WHERE submission_id = ? ORDER BY author_order ASC";
    private static final String SQL_UPDATE_AUTHOR = "UPDATE submission_authors SET is_corresponding = ?, author_order = ? WHERE submission_id = ? AND user_id = ?";
    private static final String SQL_DELETE_ALL_AUTHORS_FROM_SUBMISSION = "DELETE FROM submission_authors WHERE submission_id = ?";

    // Réutilisation du mapping de UserDAOImpl (ou mapper partagé)
     private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        // Ne pas mapper le hash ici
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setAffiliation(rs.getString("affiliation"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }

     private SubmissionAuthor mapResultSetToSubmissionAuthor(ResultSet rs) throws SQLException {
        SubmissionAuthor sa = new SubmissionAuthor();
        sa.setSubmissionId(rs.getInt("submission_id"));
        sa.setUserId(rs.getInt("user_id"));
        sa.setCorresponding(rs.getBoolean("is_corresponding"));
        sa.setAuthorOrder(rs.getInt("author_order"));
        return sa;
    }


    public void addAuthorToSubmission(SubmissionAuthor author) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_AUTHOR)) {
            pstmt.setInt(1, author.getSubmissionId());
            pstmt.setInt(2, author.getUserId());
            pstmt.setBoolean(3, author.isCorresponding());
            pstmt.setInt(4, author.getAuthorOrder());
            pstmt.executeUpdate();
        }
    }

    public boolean removeAuthorFromSubmission(int submissionId, int userId) throws SQLException {
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_AUTHOR)) {
            pstmt.setInt(1, submissionId);
            pstmt.setInt(2, userId);
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

      public List<User> findAuthorsBySubmissionId(int submissionId) throws SQLException {
        List<User> authors = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_AUTHORS_BY_SUBMISSION)) {
            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // On pourrait vouloir stocker l'ordre et is_corresponding quelque part,
                    // mais l'interface demande List<User>. On pourrait créer une classe wrapper AuthorDetails.
                    authors.add(mapResultSetToUser(rs));
                }
            }
        }
        return authors;
    }


    public List<SubmissionAuthor> findSubmissionAuthorLinks(int submissionId) throws SQLException {
        List<SubmissionAuthor> links = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_LINKS_BY_SUBMISSION)) {
            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    links.add(mapResultSetToSubmissionAuthor(rs));
                }
            }
        }
        return links;
    }



    public boolean updateSubmissionAuthor(SubmissionAuthor author) throws SQLException {
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_AUTHOR)) {
            pstmt.setBoolean(1, author.isCorresponding());
            pstmt.setInt(2, author.getAuthorOrder());
            pstmt.setInt(3, author.getSubmissionId()); // WHERE clause
            pstmt.setInt(4, author.getUserId());       // WHERE clause
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }


    public int removeAllAuthorsFromSubmission(int submissionId) throws SQLException {
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_ALL_AUTHORS_FROM_SUBMISSION)) {
            pstmt.setInt(1, submissionId);
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows;
    }
}