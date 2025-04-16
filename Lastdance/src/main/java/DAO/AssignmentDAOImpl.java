package DAO;


import models.Assignment;

import models.Assignment.AssignmentStatus;

import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentDAOImpl  {

    private static final String SQL_INSERT_ASSIGNMENT = "INSERT INTO assignments (submission_id, pc_member_id, assigned_by, assignment_date, status) VALUES (?, ?, ?, NOW(), ?)";
    private static final String SQL_SELECT_ASSIGNMENT_BY_ID = "SELECT * FROM assignments WHERE assignment_id = ?";
    private static final String SQL_SELECT_ASSIGNMENTS_BY_SUBMISSION = "SELECT * FROM assignments WHERE submission_id = ?";
    // Jointure pour récupérer les assignations d'un PC member dans une conférence spécifique
    private static final String SQL_SELECT_ASSIGNMENTS_BY_PC_MEMBER = "SELECT a.* FROM assignments a JOIN submissions s ON a.submission_id = s.submission_id WHERE s.conference_id = ? AND a.pc_member_id = ? ORDER BY a.assignment_date DESC";
    private static final String SQL_UPDATE_ASSIGNMENT_STATUS = "UPDATE assignments SET status = ? WHERE assignment_id = ?";
    private static final String SQL_DELETE_ASSIGNMENT = "DELETE FROM assignments WHERE assignment_id = ?";
    private static final String SQL_COUNT_ASSIGNMENTS_FOR_SUBMISSION = "SELECT COUNT(*) FROM assignments WHERE submission_id = ?";

     private Assignment mapResultSetToAssignment(ResultSet rs) throws SQLException {
        Assignment assign = new Assignment();
        assign.setAssignmentId(rs.getInt("assignment_id"));
        assign.setSubmissionId(rs.getInt("submission_id"));
        assign.setPcMemberId(rs.getInt("pc_member_id"));
        // Gérer assigned_by qui peut être NULL si la colonne l'autorise
        assign.setAssignedById(rs.getInt("assigned_by"));
        if (rs.wasNull()) {
            // Optionnel: mettre une valeur spéciale comme 0 ou -1 si on veut distinguer du vrai ID 0
             // assign.setAssignedById(0); // Ou laisser tel quel si getInt retourne 0 pour NULL
        }
        assign.setAssignmentDate(rs.getTimestamp("assignment_date"));
        try {
            assign.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
        } catch (Exception e) {
             System.err.println("Statut d'assignation invalide dans la DB: " + rs.getString("status"));
        }
        return assign;
    }

    public void createAssignment(Assignment assignment) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_ASSIGNMENT, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, assignment.getSubmissionId());
            pstmt.setInt(2, assignment.getPcMemberId());
             if (assignment.getAssignedById() > 0) {
                 pstmt.setInt(3, assignment.getAssignedById());
             } else {
                 pstmt.setNull(3, Types.INTEGER); // Permettre NULL si l'assignation est automatique
             }
            pstmt.setString(4, assignment.getStatus() != null ? assignment.getStatus().name() : AssignmentStatus.PENDING.name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de l'assignation a échoué.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    assignment.setAssignmentId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de l'assignation a échoué, aucun ID obtenu.");
                }
            }
        }
    }

    public Optional<Assignment> findAssignmentById(int assignmentId) throws SQLException {
        Assignment assign = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ASSIGNMENT_BY_ID)) {
            pstmt.setInt(1, assignmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    assign = mapResultSetToAssignment(rs);
                }
            }
        }
        return Optional.ofNullable(assign);
    }

    public List<Assignment> findAssignmentsBySubmissionId(int submissionId) throws SQLException {
        List<Assignment> assignments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ASSIGNMENTS_BY_SUBMISSION)) {
            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(mapResultSetToAssignment(rs));
                }
            }
        }
        return assignments;
    }

    public List<Assignment> findAssignmentsByPCMember(int conferenceId, int pcMemberId) throws SQLException {
         List<Assignment> assignments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ASSIGNMENTS_BY_PC_MEMBER)) {
            pstmt.setInt(1, conferenceId);
            pstmt.setInt(2, pcMemberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(mapResultSetToAssignment(rs));
                }
            }
        }
        return assignments;
    }

    public boolean updateAssignmentStatus(int assignmentId, AssignmentStatus newStatus) throws SQLException {
         if (assignmentId <= 0) {
             throw new IllegalArgumentException("Assignment ID invalide pour la mise à jour.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_ASSIGNMENT_STATUS)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, assignmentId); // WHERE clause
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

    public boolean deleteAssignment(int assignmentId) throws SQLException {
         if (assignmentId <= 0) {
             throw new IllegalArgumentException("Assignment ID invalide pour la suppression.");
         }
        int affectedRows = 0;
         // Attention: Supprimer une assignation peut nécessiter de supprimer l'évaluation associée (Review)
         // si la contrainte FK n'est pas en CASCADE. Gérer cela logiquement ou via la DB.
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_ASSIGNMENT)) {
            pstmt.setInt(1, assignmentId);
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

     public int countAssignmentsForSubmission(int submissionId) throws SQLException {
        int count = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_COUNT_ASSIGNMENTS_FOR_SUBMISSION)) {
            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1); // Récupère la première colonne (le COUNT)
                }
            }
        }
        return count;
    }
}