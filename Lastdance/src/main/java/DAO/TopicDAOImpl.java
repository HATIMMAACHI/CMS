package DAO;



import models.Topic;

import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TopicDAOImpl  {

    private static final String SQL_INSERT_TOPIC = "INSERT INTO topics (conference_id, name, parent_topic_id) VALUES (?, ?, ?)";
    private static final String SQL_SELECT_TOPIC_BY_ID = "SELECT * FROM topics WHERE topic_id = ?";
    private static final String SQL_SELECT_TOPICS_BY_CONF = "SELECT * FROM topics WHERE conference_id = ? ORDER BY name";
    private static final String SQL_SELECT_TOP_LEVEL_TOPICS_BY_CONF = "SELECT * FROM topics WHERE conference_id = ? AND parent_topic_id IS NULL ORDER BY name";
    private static final String SQL_SELECT_SUBTOPICS = "SELECT * FROM topics WHERE parent_topic_id = ? ORDER BY name";
    private static final String SQL_UPDATE_TOPIC = "UPDATE topics SET name = ?, parent_topic_id = ? WHERE topic_id = ?";
    private static final String SQL_DELETE_TOPIC = "DELETE FROM topics WHERE topic_id = ?";

     private Topic mapResultSetToTopic(ResultSet rs) throws SQLException {
        Topic topic = new Topic();
        topic.setTopicId(rs.getInt("topic_id"));
        topic.setConferenceId(rs.getInt("conference_id"));
        topic.setName(rs.getString("name"));
        // Gérer parent_topic_id qui peut être NULL
        topic.setParentTopicId(rs.getObject("parent_topic_id", Integer.class)); // getObject permet de récupérer NULL
        return topic;
    }


    public void createTopic(Topic topic) throws SQLException {
         try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_TOPIC, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, topic.getConferenceId());
            pstmt.setString(2, topic.getName());
            if (topic.getParentTopicId() != null && topic.getParentTopicId() > 0) {
                 pstmt.setInt(3, topic.getParentTopicId());
            } else {
                 pstmt.setNull(3, Types.INTEGER);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création du topic a échoué.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    topic.setTopicId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création du topic a échoué, aucun ID obtenu.");
                }
            }
        }
    }

    public Optional<Topic> findTopicById(int topicId) throws SQLException {
         Topic topic = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_TOPIC_BY_ID)) {
            pstmt.setInt(1, topicId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    topic = mapResultSetToTopic(rs);
                }
            }
        }
        return Optional.ofNullable(topic);
    }

 
    public List<Topic> findTopicsByConference(int conferenceId, boolean topLevelOnly) throws SQLException {
        List<Topic> topics = new ArrayList<>();
        String sql = topLevelOnly ? SQL_SELECT_TOP_LEVEL_TOPICS_BY_CONF : SQL_SELECT_TOPICS_BY_CONF;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, conferenceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapResultSetToTopic(rs));
                }
            }
        }
        return topics;
    }

    public List<Topic> findSubTopics(int parentTopicId) throws SQLException {
         List<Topic> topics = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_SUBTOPICS)) {
            pstmt.setInt(1, parentTopicId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapResultSetToTopic(rs));
                }
            }
        }
        return topics;
    }

    public boolean updateTopic(Topic topic) throws SQLException {
         if (topic.getTopicId() <= 0) {
             throw new IllegalArgumentException("Topic ID invalide pour la mise à jour.");
         }
        int affectedRows = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_TOPIC)) {
            pstmt.setString(1, topic.getName());
             if (topic.getParentTopicId() != null && topic.getParentTopicId() > 0) {
                 pstmt.setInt(2, topic.getParentTopicId());
            } else {
                 pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setInt(3, topic.getTopicId()); // WHERE clause
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }

    public boolean deleteTopic(int topicId) throws SQLException {
         if (topicId <= 0) {
             throw new IllegalArgumentException("Topic ID invalide pour la suppression.");
         }
        int affectedRows = 0;
        // Attention: La suppression d'un topic parent peut échouer à cause des FK
        // des sous-topics ou nécessiter une suppression en cascade (DB ou logique).
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_TOPIC)) {
            pstmt.setInt(1, topicId);
            affectedRows = pstmt.executeUpdate();
        }
        return affectedRows > 0;
    }
}