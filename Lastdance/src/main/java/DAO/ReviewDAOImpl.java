package DAO;


import models.Review;
import models.Review.Confidence;
import models.Review.Recommendation; 
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReviewDAOImpl {

    private static final String SQL_INSERT_REVIEW = "INSERT INTO reviews (assignment_id, submission_id, reviewer_id, comments_to_author, comments_to_sc, recommendation, confidence, review_date) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
    private static final String SQL_SELECT_REVIEW_BY_ID = "SELECT * FROM reviews WHERE review_id = ?";
    private static final String SQL_SELECT_REVIEW_BY_ASSIGNMENT_ID = "SELECT * FROM reviews WHERE assignment_id = ?";
    private static final String SQL_SELECT_REVIEWS_BY_SUBMISSION = "SELECT * FROM reviews WHERE submission_id = ?";
    // Jointure pour récupérer les reviews d'un reviewer dans une conférence
    private static final String SQL_SELECT_REVIEWS_BY_REVIEWER = "SELECT r.* FROM reviews r JOIN submissions s ON r.submission_id = s.submission_id WHERE s.conference_id = ? AND r.reviewer_id = ? ORDER BY r.review_date DESC";
    private static final String SQL_UPDATE_REVIEW = "UPDATE reviews SET comments_to_author = ?, comments_to_sc = ?, recommendation = ?, confidence = ?, review_date = NOW() WHERE review_id = ?"; // Mise à jour de la date aussi
    private static final String SQL_DELETE_REVIEW = "DELETE FROM reviews WHERE review_id = ?";

     private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setAssignmentId(rs.getInt("assignment_id"));
        review.setSubmissionId(rs.getInt("submission_id"));
        review.setReviewerId(rs.getInt("reviewer_id"));
        review.setCommentsToAuthor(rs.getString("comments_to_author"));
        review.setCommentsToSc(rs.getString("comments_to_sc"));
        try {
            review.setRecommendation(Recommendation.valueOf(rs.getString("recommendation")));
        } catch (Exception e) {
             System.err.println("Recommandation invalide dans la DB: " + rs.getString("recommendation"));
        }
        try {
             review.setConfidence(Confidence.valueOf(rs.getString("confidence")));
        } catch (Exception e) {
             System.err.println("Confiance invalide dans la DB: " + rs.getString("confidence"));
        }
        review.setReviewDate(rs.getTimestamp("review_date"));
        return review;
    }


  
    public void createReview(Review review) throws SQLException {
        // La contrainte UNIQUE sur assignment_id lèvera une SQLException si on essaie d'insérer une 2e review pour la même assignation.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_REVIEW, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, review.getAssignmentId());
            pstmt.setInt(2, review.getSubmissionId());
            pstmt.setInt(3, review.getReviewerId());
            pstmt.setString(4, review.getCommentsToAuthor());
            pstmt.setString(5, review.getCommentsToSc());
            pstmt.setString(6, review.getRecommendation() != null ? review.getRecommendation().name() : null);
            pstmt.setString(7, review.getConfidence() != null ? review.getConfidence().name() : null);


            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de l'évaluation a échoué.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    review.setReviewId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de l'évaluation a échoué, aucun ID obtenu.");
                }
            }
        }
    }

    public Optional<Review> findReviewById(int reviewId) throws SQLException {
        Review review = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_REVIEW_BY_ID)) {
            pstmt.setInt(1, reviewId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    review = mapResultSetToReview(rs);
                }
            }
        }
        return Optional.ofNullable(review);
    }

 
    public Optional<Review> findReviewByAssignmentId(int assignmentId) throws SQLException {
         Review review = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_REVIEW_BY_ASSIGNMENT_ID)) {
            pstmt.setInt(1, assignmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    review = mapResultSetToReview(rs);
                }
            }
        }
        return Optional.ofNullable(review);
    }

   
    public List<Review> findReviewsBySubmissionId(int submissionId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_REVIEWS_BY_SUBMISSION)) {
            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        return reviews;
    }

    public List<Review> findReviewsByReviewer(int conferenceId, int reviewerId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_REVIEWS_BY_REVIEWER)) {
            pstmt.setInt(1, conferenceId);
            pstmt.setInt(2, reviewerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        return reviews;
    }


    public boolean updateReview(Review review) throws SQLException {
         if (review.getReviewId() <= 0) {
             throw new IllegalArgumentException("Review ID invalide pour la mise à jour.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_REVIEW)) {
            pstmt.setString(1, review.getCommentsToAuthor());
            pstmt.setString(2, review.getCommentsToSc());
            pstmt.setString(3, review.getRecommendation() != null ? review.getRecommendation().name() : null);
            pstmt.setString(4, review.getConfidence() != null ? review.getConfidence().name() : null);
            pstmt.setInt(5, review.getReviewId()); // WHERE clause
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

  
    public boolean deleteReview(int reviewId) throws SQLException {
         if (reviewId <= 0) {
             throw new IllegalArgumentException("Review ID invalide pour la suppression.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_REVIEW)) {
            pstmt.setInt(1, reviewId);
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }
}