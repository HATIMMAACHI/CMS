package DAO;

import models.User;

import util.DatabaseConnection; // Notre classe utilitaire

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl  {

    // Requêtes SQL pré-compilées pour la performance et la sécurité
    private static final String SQL_INSERT_USER = "INSERT INTO users (email, password_hash, first_name, last_name, affiliation, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
    private static final String SQL_SELECT_USER_BY_ID = "SELECT user_id, email, password_hash, first_name, last_name, affiliation, created_at FROM users WHERE user_id = ?";
    private static final String SQL_SELECT_USER_BY_EMAIL = "SELECT user_id, email, password_hash, first_name, last_name, affiliation, created_at FROM users WHERE email = ?";
    private static final String SQL_SELECT_ALL_USERS = "SELECT user_id, email, password_hash, first_name, last_name, affiliation, created_at FROM users ORDER BY last_name, first_name";
    private static final String SQL_UPDATE_USER_PROFILE = "UPDATE users SET first_name = ?, last_name = ?, affiliation = ? WHERE user_id = ?";
    private static final String SQL_UPDATE_USER_PASSWORD = "UPDATE users SET password_hash = ? WHERE user_id = ?";
    private static final String SQL_DELETE_USER = "DELETE FROM users WHERE user_id = ?";
    private static final String SQL_SEARCH_USERS = "SELECT user_id, email, password_hash, first_name, last_name, affiliation, created_at FROM users WHERE LOWER(email) LIKE LOWER(?) OR LOWER(first_name) LIKE LOWER(?) OR LOWER(last_name) LIKE LOWER(?) ORDER BY last_name, first_name LIMIT 50"; // Ajout de LOWER et LIMIT

    /**
     * Mappe une ligne de ResultSet vers un objet User.
     * @param rs Le ResultSet positionné sur la ligne à mapper.
     * @return Un objet User peuplé.
     * @throws SQLException Si une colonne n'est pas trouvée.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setAffiliation(rs.getString("affiliation"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }

    
    public void createUser(User user) throws SQLException {
        // Utilisation de try-with-resources pour garantir la fermeture des ressources
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPasswordHash()); // Assurez-vous que le hash est déjà généré
            pstmt.setString(3, user.getFirstName());
            pstmt.setString(4, user.getLastName());
            pstmt.setString(5, user.getAffiliation());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("La création de l'utilisateur a échoué, aucune ligne affectée.");
            }

            // Récupérer l'ID généré par la base de données
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1)); // Met à jour l'ID de l'objet User
                } else {
                    throw new SQLException("La création de l'utilisateur a échoué, aucun ID obtenu.");
                }
            }
        }
        // La connexion et le preparedStatement sont automatiquement fermés ici
    }

 
    public Optional<User> findUserById(int userId) throws SQLException {
        User user = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_USER_BY_ID)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = mapResultSetToUser(rs);
                }
            }
        }
        return Optional.ofNullable(user); // Retourne un Optional vide si user est null
    }

    
    public Optional<User> findUserByEmail(String email) throws SQLException {
        User user = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_USER_BY_EMAIL)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = mapResultSetToUser(rs);
                }
            }
        }
        return Optional.ofNullable(user);
    }

   
    public List<User> findAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ALL_USERS);
             ResultSet rs = pstmt.executeQuery()) { // Pas de paramètres '?' ici

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }


    public boolean updateUserProfile(User user) throws SQLException {
        if (user.getUserId() <= 0) {
             throw new IllegalArgumentException("User ID invalide pour la mise à jour.");
        }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_USER_PROFILE)) {

            pstmt.setString(1, user.getFirstName());
            pstmt.setString(2, user.getLastName());
            pstmt.setString(3, user.getAffiliation());
            pstmt.setInt(4, user.getUserId()); // Clause WHERE

            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0; // Retourne true si au moins une ligne a été modifiée
    }

 
    public boolean updateUserPassword(int userId, String newPasswordHash) throws SQLException {
         if (userId <= 0) {
             throw new IllegalArgumentException("User ID invalide pour la mise à jour du mot de passe.");
        }
         if (newPasswordHash == null || newPasswordHash.isEmpty()) {
              throw new IllegalArgumentException("Le nouveau hash de mot de passe ne peut pas être vide.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_USER_PASSWORD)) {

            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId); // Clause WHERE

            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }


    public boolean deleteUser(int userId) throws SQLException {
         if (userId <= 0) {
             throw new IllegalArgumentException("User ID invalide pour la suppression.");
        }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_USER)) {

            pstmt.setInt(1, userId); // Clause WHERE
            affectedRows = pstmt.executeUpdate();
        }
        // Attention : les contraintes de clé étrangère pourraient empêcher la suppression.
        // Il faudrait gérer les SQLException spécifiques (ex: ConstraintViolation).
        return affectedRows > 0;
    }


    public List<User> searchUsers(String query) throws SQLException {
        List<User> users = new ArrayList<>();
        String searchQuery = "%" + query + "%"; // Ajoute les wildcards pour LIKE

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SEARCH_USERS)) {

            pstmt.setString(1, searchQuery); // Pour email
            pstmt.setString(2, searchQuery); // Pour first_name
            pstmt.setString(3, searchQuery); // Pour last_name

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        }
        return users;
    }
}