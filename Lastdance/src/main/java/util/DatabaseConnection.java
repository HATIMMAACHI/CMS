package util;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // !! NE JAMAIS HARDCODER LES IDENTIFIANTS EN PRODUCTION !!
    // Utilisez des variables d'environnement, des fichiers de config, ou JNDI.
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/conference_db?useSSL=false&serverTimezone=UTC";
    private static final String JDBC_USER = "root"; // Remplacez
    private static final String JDBC_PASSWORD = "Hatim@2003"; // Remplacez
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver"; // Pour MySQL Connector/J 8+

    // Charger le driver une seule fois
    static {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur critique: Driver JDBC MySQL introuvable.");
            // Dans une vraie app, lancer une exception plus spécifique ou logger
            throw new RuntimeException("Driver JDBC MySQL introuvable", e);
        }
    }

    /**
     * Obtient une nouvelle connexion à la base de données.
     * IMPORTANT: L'appelant est responsable de fermer cette connexion.
     * @return Une connexion SQL.
     * @throws SQLException Si la connexion échoue.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    // Méthode utilitaire pour fermer la connexion (peut être enrichie pour fermer aussi Statement/ResultSet)
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
                // Logger l'erreur
            }
        }
    }

     // Méthode utilitaire pour fermer proprement Connection, Statement, ResultSet
    public static void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    // Logguer l'erreur mais ne pas la propager (typiquement)
                    System.err.println("Erreur lors de la fermeture de la ressource " + resource.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }
}