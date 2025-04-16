package DAO;


import models.User;
import models.UserConferenceRole;
import models.UserConferenceRole.Role;

import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserConferenceRoleDAOImpl {

    private static final String SQL_INSERT_ROLE = "INSERT IGNORE INTO user_conference_roles (user_id, conference_id, role) VALUES (?, ?, ?)"; // INSERT IGNORE pour éviter l'erreur si le rôle existe déjà
    private static final String SQL_DELETE_ROLE = "DELETE FROM user_conference_roles WHERE user_id = ? AND conference_id = ? AND role = ?";
    private static final String SQL_DELETE_ALL_ROLES_FOR_USER_CONF = "DELETE FROM user_conference_roles WHERE user_id = ? AND conference_id = ?";
    private static final String SQL_SELECT_ROLES_FOR_USER_CONF = "SELECT role FROM user_conference_roles WHERE user_id = ? AND conference_id = ?";
    private static final String SQL_CHECK_USER_HAS_ROLE = "SELECT 1 FROM user_conference_roles WHERE user_id = ? AND conference_id = ? AND role = ? LIMIT 1";
    private static final String SQL_SELECT_USERS_BY_CONF_ROLE = "SELECT u.* FROM users u JOIN user_conference_roles ucr ON u.user_id = ucr.user_id WHERE ucr.conference_id = ? AND ucr.role = ?";
    private static final String SQL_SELECT_ALL_USER_ROLES_ACROSS_CONFS = "SELECT conference_id, role FROM user_conference_roles WHERE user_id = ?";

    // Réutilisation du mapping de UserDAOImpl (idéalement via un UserMapper partagé, mais ici on le copie pour simplifier)
     private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash")); // Normalement pas nécessaire ici, mais inclus car dans le SELECT *
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setAffiliation(rs.getString("affiliation"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }


    public void assignRoleToUser(int userId, int conferenceId, Role role) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_ROLE)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, conferenceId);
            pstmt.setString(3, role.name()); // Convertit l'enum en String
            pstmt.executeUpdate();
            // INSERT IGNORE ne lance pas d'erreur si la clé existe déjà, et affectedRows peut être 0.
            // Si on voulait savoir si l'insertion a eu lieu, il faudrait faire un SELECT avant ou gérer l'exception sans IGNORE.
        }
    }

 
    public boolean removeRoleFromUser(int userId, int conferenceId, Role role) throws SQLException {
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_ROLE)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, conferenceId);
            pstmt.setString(3, role.name());
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }


    public int removeAllRolesFromUserForConference(int userId, int conferenceId) throws SQLException {
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_ALL_ROLES_FOR_USER_CONF)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, conferenceId);
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows;
    }


    public List<Role> findUserRolesForConference(int userId, int conferenceId) throws SQLException {
        List<Role> roles = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ROLES_FOR_USER_CONF)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, conferenceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        roles.add(Role.valueOf(rs.getString("role")));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Rôle invalide trouvé dans la DB: " + rs.getString("role"));
                        // Ignorer ou logger
                    }
                }
            }
        }
        return roles;
    }


    public boolean userHasRole(int userId, int conferenceId, Role role) throws SQLException {
        boolean hasRole = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CHECK_USER_HAS_ROLE)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, conferenceId);
            pstmt.setString(3, role.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) { // Si une ligne est retournée, le rôle existe
                    hasRole = true;
                }
            }
        }
        return hasRole;
    }


    public List<User> findUsersByConferenceAndRole(int conferenceId, Role role) throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_USERS_BY_CONF_ROLE)) {
            pstmt.setInt(1, conferenceId);
            pstmt.setString(2, role.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs)); // Utilise le mapper
                }
            }
        }
        return users;
    }


    public Map<Integer, List<Role>> findAllUserRolesAcrossConferences(int userId) throws SQLException {
        Map<Integer, List<Role>> rolesMap = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ALL_USER_ROLES_ACROSS_CONFS)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int confId = rs.getInt("conference_id");
                    String roleStr = rs.getString("role");
                    try {
                        Role roleEnum = Role.valueOf(roleStr);
                        // Ajoute la conférence à la map si elle n'y est pas encore
                        rolesMap.computeIfAbsent(confId, k -> new ArrayList<>()).add(roleEnum);
                    } catch (IllegalArgumentException e) {
                         System.err.println("Rôle invalide trouvé dans la DB pour user " + userId + ": " + roleStr);
                    }
                }
            }
        }
        return rolesMap;
    }
}
