package models;

import java.sql.Timestamp;
import java.util.Objects;

// Utilisation de l'API standard Java pour la date/heure
import java.time.LocalDateTime;


public class User {

    private int userId;
    private String email;
    private String passwordHash; // Ne stockez JAMAIS le mot de passe en clair
    private String firstName;
    private String lastName;
    private String affiliation;
    private Timestamp createdAt; // Utilisation de Timestamp pour compatibilité JDBC directe

    // Constructeur vide (souvent utile)
    public User() {}

    // Constructeur avec champs (sans ID et createdAt pour la création)
    public User(String email, String passwordHash, String firstName, String lastName, String affiliation) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.affiliation = affiliation;
    }

    // Getters et Setters pour tous les champs
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Optionnel: equals() et hashCode() basés sur l'ID ou l'email
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId || Objects.equals(email, user.email); // Utiliser l'email si l'ID n'est pas encore défini
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }

    // Optionnel: toString() pour le débogage
    @Override
    public String toString() {
        return "User{" +
               "userId=" + userId +
               ", email='" + email + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               '}';
    }
}