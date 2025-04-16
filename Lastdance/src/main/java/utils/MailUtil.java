package utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import models.Conference;
import models.Submission;
import models.Submission.SubmissionStatus;
import models.User;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class MailUtil {

    // --- CONFIGURATION SMTP ---

    // Exemple : Mailtrap
    private static final String SMTP_HOST = "live.smtp.mailtrap.io";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USER = "smtp@mailtrap.io";
    private static final String SMTP_PASSWORD = "a49a151c0cada2e80772505b542f796f";
    private static final boolean SMTP_AUTH = true;
    private static final boolean SMTP_STARTTLS = true;

    private static final String FROM_EMAIL = "no-reply@conferencems.com";
    private static final String FROM_NAME = "ConferenceMS Platform";

    public static boolean sendEmail(String to, String subject, String body) {
        System.out.println("Tentative d'envoi d'email à: " + to + " | Sujet: " + subject);

        Properties properties = new Properties();
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.auth", String.valueOf(SMTP_AUTH));
        properties.put("mail.smtp.starttls.enable", String.valueOf(SMTP_STARTTLS));

        Session session;
        if (SMTP_AUTH) {
            session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
                }
            });
        } else {
            session = Session.getInstance(properties);
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Email envoyé avec succès !");
            return true;

        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("Erreur lors de l'envoi de l'email à " + to);
            e.printStackTrace();
            return false;
        }
    }

    public static String generateSubmissionConfirmationBody(User author, Submission submission, Conference conference) {
        return "<html><body style='font-family: sans-serif;'>"
             + "<h2>Confirmation de Soumission d'Article</h2>"
             + "<p>Cher " + fn(author.getFirstName()) + " " + fn(author.getLastName()) + ",</p>"
             + "<p>Votre article intitulé \"<b>" + fn(submission.getTitle()) + "</b>\" "
             + "(ID: " + submission.getUniquePaperId() + ") a bien été soumis à la conférence "
             + "<b>" + fn(conference.getName()) + " (" + fn(conference.getAcronym()) + ")</b>.</p>"
             + "<p>Vous pouvez suivre son statut depuis votre tableau de bord.</p>"
             + "<p>Cordialement,<br/>L'équipe d'organisation</p>"
             + "</body></html>";
    }

    public static String generateDecisionNotificationBody(User author, Submission submission, Conference conference) {
        String decisionText;
        String decisionColor;
        if (submission.getStatus() == SubmissionStatus.ACCEPTED) {
            decisionText = "Accepté";
            decisionColor = "green";
        } else if (submission.getStatus() == SubmissionStatus.REJECTED) {
            decisionText = "Rejeté";
            decisionColor = "red";
        } else {
            decisionText = "Statut Indéterminé (" + submission.getStatus() + ")";
            decisionColor = "black";
        }

        return "<html><body style='font-family: sans-serif;'>"
             + "<h2>Décision Concernant Votre Soumission</h2>"
             + "<p>Cher " + fn(author.getFirstName()) + " " + fn(author.getLastName()) + ",</p>"
             + "<p>Nous vous informons de la décision concernant votre article \"<b>" + fn(submission.getTitle()) + "</b>\" "
             + "(ID: " + submission.getUniquePaperId() + ") soumis à la conférence "
             + "<b>" + fn(conference.getName()) + " (" + fn(conference.getAcronym()) + ")</b>.</p>"
             + "<p style='font-size: 1.2em;'>Décision Finale : <strong style='color:" + decisionColor + ";'>" + decisionText + "</strong></p>"
             + "<p><i>Commentaires des évaluateurs (si disponibles) :</i></p>"
             + "<div style='border-left: 3px solid #ccc; padding-left: 10px; margin-left: 5px; font-style: italic;'>"
             + "[Commentaires des évaluateurs à insérer ici - TODO]"
             + "</div>"
             + "<p>Vous pouvez consulter les détails depuis votre tableau de bord.</p>"
             + "<p>Cordialement,<br/>Le Comité Scientifique</p>"
             + "</body></html>";
    }

    private static String fn(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}