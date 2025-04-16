package servlets;

// Imports Java et Jakarta EE
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map; // Pour la map des reviews
import java.util.Optional;
import java.util.stream.Collectors; // Pour grouper les reviews

// Imports DAO
import DAO.ConferenceDAOImpl;
import DAO.ReviewDAOImpl;       // Pour récupérer les évaluations
import DAO.SubmissionAuthorDAOImpl; // Pour notifier les auteurs (plus tard)
import DAO.SubmissionDAOImpl;   // Pour récupérer/mettre à jour les soumissions
import DAO.UserConferenceRoleDAOImpl; // Pour vérifier le rôle SC
// import DAO.UserDAOImpl;          // Pour récupérer les emails auteurs (plus tard)

// Imports Models
import models.Conference;
import models.Review;
import models.Submission;
import models.Submission.SubmissionStatus; // Pour les statuts
import models.User;
import models.UserConferenceRole.Role;

/**
 * Servlet gérant les actions du Comité Scientifique (SC),
 * notamment la visualisation des évaluations et la prise de décision finale.
 */
@WebServlet(name = "SCServlet", urlPatterns = {
        "/sc/dashboard", // <-- Vérifiez ceci
        "/sc/makeDecision"
        
})
public class SCServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private SubmissionDAOImpl submissionDAO;
    private ReviewDAOImpl reviewDAO;
    private UserConferenceRoleDAOImpl userConferenceRoleDAO;
    private ConferenceDAOImpl conferenceDAO;
    private SubmissionAuthorDAOImpl submissionAuthorDAO; // Pour notification future
    // private UserDAOImpl userDAO; // Pour notification future

    @Override
    public void init() throws ServletException {
        super.init();
        this.submissionDAO = new SubmissionDAOImpl();
        this.reviewDAO = new ReviewDAOImpl();
        this.userConferenceRoleDAO = new UserConferenceRoleDAOImpl();
        this.conferenceDAO = new ConferenceDAOImpl();
        this.submissionAuthorDAO = new SubmissionAuthorDAOImpl();
        // this.userDAO = new UserDAOImpl();
    }

    // ======================== doGet Router ========================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendRedirect(request.getContextPath() + "/login?error=session_expired"); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();

        try {
            if ("/sc/dashboard".equals(actionPath)) {
                showSCDashboard(request, response, currentUser);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Action GET SC inconnue.");
            }
        } catch (InvalidIdException e) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) { handleGenericError(request, response, e, "Erreur DB (GET SC)");
        } catch (Exception e) { handleGenericError(request, response, e, "Erreur serveur (GET SC)"); }
    }

    // ======================== doPost Router =======================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non autorisé."); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();

        try {
            if ("/sc/makeDecision".equals(actionPath)) {
                processMakeDecision(request, response, currentUser);
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Action POST SC inconnue.");
            }
        } catch (InvalidIdException e) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            // Tenter de réafficher le dashboard SC avec une erreur
            int confIdForRetry = getIntParamFromRequestQuietly(request, "confId"); // ConfId doit être dans le form makeDecision
            request.setAttribute("scError", "Erreur base de données: " + e.getMessage());
            e.printStackTrace();
            try { showSCDashboard(request, response, currentUser); } // Tente de réafficher
            catch(Exception displayError) { handleGenericError(request, response, e, "Erreur DB (POST SC) + échec réaffichage"); }
        } catch (Exception e) { handleGenericError(request, response, e, "Erreur serveur (POST SC)"); }
    }

    // ======================== Méthodes Actions GET ========================

    /** Prépare les données et affiche la vue principale du SC pour une conférence. */
    private void showSCDashboard(HttpServletRequest request, HttpServletResponse response, User currentUser)
        throws ServletException, IOException, SQLException, InvalidIdException {

        int conferenceId = getIntParamFromRequest(request, "confId"); // Récupère l'ID depuis l'URL

        // Sécurité: Vérifier si l'utilisateur est SC Member
        if (!userConferenceRoleDAO.userHasRole(currentUser.getUserId(), conferenceId, Role.SC_MEMBER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès réservé aux membres du Comité Scientifique.");
            return;
        }

        // Récupérer infos conférence
        Optional<Conference> confOpt = conferenceDAO.findConferenceById(conferenceId);
        if (confOpt.isEmpty()) { throw new InvalidIdException("Conférence ID " + conferenceId + " non trouvée."); }

        // Récupérer les soumissions pertinentes (celles qui ont été évaluées ou en cours)
        // On prend celles qui sont UNDER_REVIEW (assignées, peut-être pas toutes évaluées)
        // et celles déjà ACCEPTED/REJECTED pour voir l'historique.
        List<Submission> submissions = submissionDAO.findSubmissionsByConference(conferenceId) // Prend toutes...
                                           .stream()
                                           // ...et filtre par statut pertinent pour SC
                                           .filter(s -> s.getStatus() == SubmissionStatus.UNDER_REVIEW ||
                                                        s.getStatus() == SubmissionStatus.ACCEPTED ||
                                                        s.getStatus() == SubmissionStatus.REJECTED)
                                           .collect(Collectors.toList());

        // Récupérer TOUTES les évaluations pour ces soumissions
        Map<Integer, List<Review>> reviewsMap = new HashMap<>();
        for (Submission sub : submissions) {
            List<Review> reviews = reviewDAO.findReviewsBySubmissionId(sub.getSubmissionId());
            reviewsMap.put(sub.getSubmissionId(), reviews);
        }

        // Transmettre les données à la JSP
        request.setAttribute("conference", confOpt.get());
        request.setAttribute("submissions", submissions);
        request.setAttribute("reviewsMap", reviewsMap); // Map<submissionId, List<Review>>
        request.setAttribute("confId", conferenceId);

        // Afficher la JSP
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/sc/dashboard_sc.jsp");
        dispatcher.forward(request, response);
    }


    // ======================== Méthodes Actions POST =======================

    /** Traite l'enregistrement de la décision finale pour un article. */
    private void processMakeDecision(HttpServletRequest request, HttpServletResponse response, User currentUser)
        throws ServletException, IOException, SQLException, InvalidIdException {

        int conferenceId = getIntParamFromRequest(request, "confId"); // Champ caché dans le formulaire
        int submissionId = getIntParamFromRequest(request, "submissionId"); // Champ caché
        String decisionStr = request.getParameter("decision"); // Valeur du bouton radio/select ("ACCEPT" ou "REJECT")

        // Sécurité: Vérifier rôle SC pour cette conférence
        if (!userConferenceRoleDAO.userHasRole(currentUser.getUserId(), conferenceId, Role.SC_MEMBER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Action réservée aux membres du Comité Scientifique.");
            return;
        }

        // Validation de la décision
        SubmissionStatus newStatus = null;
        if ("ACCEPT".equals(decisionStr)) {
            newStatus = SubmissionStatus.ACCEPTED;
        } else if ("REJECT".equals(decisionStr)) {
            newStatus = SubmissionStatus.REJECTED;
        } else {
            request.setAttribute("scError", "Décision invalide sélectionnée pour l'article ID " + submissionId + ".");
            showSCDashboard(request, response, currentUser); // Réafficher avec erreur
            return;
        }

        // Vérifier si la soumission existe et appartient à la bonne conférence (sécurité supplémentaire)
        Optional<Submission> subOpt = submissionDAO.findSubmissionById(submissionId);
        if (subOpt.isEmpty() || subOpt.get().getConferenceId() != conferenceId) {
            throw new InvalidIdException("Soumission ID " + submissionId + " invalide ou n'appartient pas à la conférence " + conferenceId);
        }

        // Mettre à jour le statut dans la base de données
        boolean updated = submissionDAO.updateSubmissionStatus(submissionId, newStatus);

        if (updated) {
             System.out.println("Décision " + newStatus + " enregistrée pour submission " + submissionId + " par SC " + currentUser.getUserId());
             request.getSession().setAttribute("successMessage", "Décision '" + newStatus + "' enregistrée pour l'article ID " + submissionId + ".");

             // --- TODO: Déclencher l'envoi de l'email de notification ---
             // try {
             //     notifyAuthor(submissionId, newStatus); // Appeler une méthode pour envoyer l'email
             // } catch (Exception mailEx) {
             //     System.err.println("Erreur lors de l'envoi de l'email de notification pour submission " + submissionId);
             //     mailEx.printStackTrace();
             //     request.getSession().setAttribute("warningMessage", "Décision enregistrée, mais l'email de notification n'a pas pu être envoyé.");
             // }
             // ---------------------------------------------------------

        } else {
            request.setAttribute("scError", "La mise à jour du statut a échoué pour l'article ID " + submissionId + ".");
        }

        // Réafficher le dashboard SC pour voir le statut mis à jour
        // Plutôt que d'appeler showSCDashboard directement (qui referait les requêtes DB), on redirige
        response.sendRedirect(request.getContextPath() + "/sc/dashboard_sc?confId=" + conferenceId);
        // showSCDashboard(request, response, currentUser); // Alternative si on veut éviter une redirection
    }


    // ======================== Méthodes Utilitaires =======================
    private int getIntParamFromRequest(HttpServletRequest request, String paramName) throws InvalidIdException { /* ... identique ... */
         String paramValue = request.getParameter(paramName);
        if (paramValue == null || paramValue.trim().isEmpty()) { throw new InvalidIdException("Paramètre '" + paramName + "' manquant."); }
        try { int id = Integer.parseInt(paramValue); if (id <= 0) { throw new InvalidIdException("ID invalide pour '" + paramName + "'."); } return id; }
        catch (NumberFormatException e) { throw new InvalidIdException("Format invalide pour '" + paramName + "'."); }
    }
    private int getIntParamFromRequestQuietly(HttpServletRequest request, String paramName) { /* ... identique ... */
         String paramValue = request.getParameter(paramName); if (paramValue != null) { try { int id = Integer.parseInt(paramValue); if (id > 0) return id; } catch (NumberFormatException e) {} } return 0;
     }
    private void handleGenericError(HttpServletRequest req, HttpServletResponse res, Exception e, String ctx) throws IOException, ServletException { /* ... identique ... */
         System.err.println("ERREUR SERVEUR DANS SC SERVLET - Contexte: " + ctx); e.printStackTrace();
         res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne serveur.");
     }
    private static class InvalidIdException extends Exception { public InvalidIdException(String msg) { super(msg); } }

}