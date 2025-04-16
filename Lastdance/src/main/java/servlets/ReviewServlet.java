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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap; // Import manquant corrigé
import java.util.List;
import java.util.Map;
import java.util.Optional;
// Pas besoin de stream/Collectors ici finalement

// Imports DAO (Adapter le nom du package si besoin)
import DAO.AssignmentDAOImpl;
import DAO.ConferenceDAOImpl;
import DAO.ReviewDAOImpl;
import DAO.SubmissionDAOImpl;
import DAO.UserConferenceRoleDAOImpl;

// Imports Models (Adapter le nom du package si besoin)
import models.Assignment;
import models.Assignment.AssignmentStatus;
import models.Conference;
import models.Review;
import models.Review.Confidence;
import models.Review.Recommendation;
import models.Submission;
import models.User;
import models.UserConferenceRole.Role;

/**
 * Servlet gérant le processus d'évaluation par les membres du Comité de Programme (PC).
 */
@WebServlet(name = "ReviewServlet", urlPatterns = {
        "/review/assigned", // <-- Est-ce bien présent ?
        "/review/form",
        "/review/submit"
})

public class ReviewServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    // Déclaration des DAOs
    private AssignmentDAOImpl assignmentDAO;
    private SubmissionDAOImpl submissionDAO;
    private ReviewDAOImpl reviewDAO;
    private UserConferenceRoleDAOImpl userConferenceRoleDAO;
    private ConferenceDAOImpl conferenceDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        // Initialisation des DAOs
        this.assignmentDAO = new AssignmentDAOImpl();
        this.submissionDAO = new SubmissionDAOImpl();
        this.reviewDAO = new ReviewDAOImpl();
        this.userConferenceRoleDAO = new UserConferenceRoleDAOImpl();
        this.conferenceDAO = new ConferenceDAOImpl();
    }

    // ======================== doGet Router ========================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendRedirect(request.getContextPath() + "/login?error=session_expired"); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();

        try {
             // Récupérer les IDs nécessaires en fonction de l'action
             int conferenceId = 0;
             int assignmentId = 0;

             if ("/review/assigned".equals(actionPath)) {
                conferenceId = getConferenceIdFromRequest(request); // Requis pour lister
             } else if ("/review/form".equals(actionPath)) {
                 assignmentId = getIntParamFromRequest(request, "assignId"); // Requis pour afficher le formulaire
             }

            // Router vers la méthode appropriée
            switch (actionPath) {
                case "/review/assigned":
                    showAssignedReviews(request, response, currentUser, conferenceId);
                    break;
                case "/review/form":
                    showReviewForm(request, response, currentUser, assignmentId);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Action GET inconnue pour l'évaluation.");
            }
        } catch (InvalidIdException e) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); // Erreur 400 si ID invalide/manquant
        } catch (SQLException e) { handleGenericError(request, response, e, "Erreur DB (GET Review)");
        } catch (Exception e) { handleGenericError(request, response, e, "Erreur serveur (GET Review)"); }
    }

    // ======================== doPost Router =======================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non autorisé."); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();

        try {
            if ("/review/submit".equals(actionPath)) {
                processReviewSubmission(request, response, currentUser); // Appelle la méthode pour traiter la soumission
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Action POST inconnue pour l'évaluation.");
            }
        } catch (InvalidIdException e) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); // Erreur 400 si ID invalide/manquant
        } catch (SQLException e) {
             // Tenter de réafficher le formulaire si l'erreur SQL survient lors du submit
             int assignIdForRetry = getIntParamFromRequestQuietly(request, "assignmentId");
             request.setAttribute("reviewError", "Erreur base de données lors de l'enregistrement: " + e.getMessage());
             e.printStackTrace();
             try {
                 // On doit quand même passer les infos nécessaires à showReviewForm
                 showReviewForm(request, response, currentUser, assignIdForRetry);
             }
             catch (Exception displayError) { handleGenericError(request, response, e, "Erreur DB (POST Review) + échec réaffichage"); }
        } catch (Exception e) { handleGenericError(request, response, e, "Erreur serveur (POST Review)"); }
    }

    // ======================== Méthodes Actions GET ========================

    /** Affiche la liste des articles assignés au PC member connecté pour une conférence. */
    private void showAssignedReviews(HttpServletRequest request, HttpServletResponse response, User pcMember, int conferenceId)
        throws ServletException, IOException, SQLException, InvalidIdException {

         // Sécurité: Vérifier que l'utilisateur est bien PC Member pour cette conf
         if (!userConferenceRoleDAO.userHasRole(pcMember.getUserId(), conferenceId, Role.PC_MEMBER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès réservé aux membres du Comité de Programme.");
            return;
         }

        // Récupérer les infos de la conférence
        Optional<Conference> confOpt = conferenceDAO.findConferenceById(conferenceId);
        if (confOpt.isEmpty()) { throw new InvalidIdException("Conférence ID " + conferenceId + " non trouvée."); } // Ou 404

        // Récupérer les assignations
        List<Assignment> assignments = assignmentDAO.findAssignmentsByPCMember(conferenceId, pcMember.getUserId());

        // Récupérer les détails des soumissions associées
        Map<Integer, Submission> submissionDetails = new HashMap<>();
        for (Assignment assign : assignments) {
            if (!submissionDetails.containsKey(assign.getSubmissionId())) {
                 submissionDAO.findSubmissionById(assign.getSubmissionId()).ifPresent(sub -> submissionDetails.put(sub.getSubmissionId(), sub));
            }
        }

        // Vérifier si une évaluation existe déjà pour chaque assignation
         Map<Integer, Boolean> hasReview = new HashMap<>();
         for(Assignment assign : assignments) {
             hasReview.put(assign.getAssignmentId(), reviewDAO.findReviewByAssignmentId(assign.getAssignmentId()).isPresent());
         }

        // Transmettre les données à la JSP
        request.setAttribute("conference", confOpt.get());
        request.setAttribute("assignments", assignments);
        request.setAttribute("submissionDetails", submissionDetails);
        request.setAttribute("hasReview", hasReview);
        request.setAttribute("confId", conferenceId); // Utile pour les liens retour

        // Afficher la JSP
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/review/review_assigned.jsp");
        dispatcher.forward(request, response);
    }

    /** Affiche le formulaire d'évaluation pour une assignation spécifique. */
    private void showReviewForm(HttpServletRequest request, HttpServletResponse response, User pcMember, int assignmentId)
        throws ServletException, IOException, SQLException, InvalidIdException {

        if (assignmentId <= 0) { throw new InvalidIdException("ID d'assignation requis pour afficher le formulaire."); }

        // Récupérer l'assignation et vérifier les droits
        Optional<Assignment> assignOpt = assignmentDAO.findAssignmentById(assignmentId);
        if (assignOpt.isEmpty()) { throw new InvalidIdException("Assignation ID " + assignmentId + " non trouvée."); }
        Assignment assignment = assignOpt.get();
        if (assignment.getPcMemberId() != pcMember.getUserId()) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Assignation non autorisée."); return; }

        // Récupérer la soumission
        Optional<Submission> subOpt = submissionDAO.findSubmissionById(assignment.getSubmissionId());
         if (subOpt.isEmpty()) { throw new ServletException("Article associé (ID " + assignment.getSubmissionId() + ") non trouvé pour assignation " + assignmentId); } // Erreur interne si l'article n'existe plus

         // Récupérer la conférence
         Optional<Conference> confOpt = conferenceDAO.findConferenceById(subOpt.get().getConferenceId());

         // Pré-remplir si évaluation existante (sauf si on vient d'une erreur POST)
         if (request.getAttribute("reviewData") == null) { // Ne pas écraser les données POST erronées
            Optional<Review> existingReviewOpt = reviewDAO.findReviewByAssignmentId(assignmentId);
            if (existingReviewOpt.isPresent()) {
               request.setAttribute("reviewData", existingReviewOpt.get());
                request.setAttribute("isEditMode", true);
            } else {
                 request.setAttribute("isEditMode", false);
            }
         } // Si reviewData existe déjà (erreur POST), isEditMode doit aussi être mis par processReviewSubmission

        // Transmettre les données à la JSP
        request.setAttribute("assignment", assignment);
        request.setAttribute("submission", subOpt.get());
        request.setAttribute("conference", confOpt.orElse(null)); // La conf pourrait ne plus exister, mais peu probable
        request.setAttribute("recommendations", Recommendation.values());
        request.setAttribute("confidences", Confidence.values());

        // Afficher la JSP
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/review/review_form.jsp");
        dispatcher.forward(request, response);
    }

    // ======================== Méthodes Actions POST =======================

    /** Traite la soumission du formulaire d'évaluation. */
    private void processReviewSubmission(HttpServletRequest request, HttpServletResponse response, User pcMember)
        throws ServletException, IOException, SQLException, InvalidIdException {

        // 1. Récupérer les paramètres
        int assignmentId = getIntParamFromRequest(request, "assignmentId");
        String commentsAuthor = request.getParameter("commentsAuthor");
        String commentsSC = request.getParameter("commentsSC");
        String recommendationStr = request.getParameter("recommendation");
        String confidenceStr = request.getParameter("confidence");

        // 2. Valider l'assignation et les droits
        Optional<Assignment> assignOpt = assignmentDAO.findAssignmentById(assignmentId);
        if (assignOpt.isEmpty()) { throw new InvalidIdException("Assignation ID " + assignmentId + " non trouvée."); }
        Assignment assignment = assignOpt.get();
        if (assignment.getPcMemberId() != pcMember.getUserId()) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Non autorisé."); return; }

        // 3. Validation des champs
        StringBuilder errors = new StringBuilder();
        Recommendation recommendation = null; Confidence confidence = null;
        if (commentsAuthor == null || commentsAuthor.trim().isEmpty()) { errors.append("Commentaires pour l'auteur requis.<br>"); }
        if (recommendationStr == null || recommendationStr.isEmpty()) { errors.append("Recommandation requise.<br>"); } else { try { recommendation = Recommendation.valueOf(recommendationStr); } catch (IllegalArgumentException e) { errors.append("Recommandation invalide.<br>"); } }
        if (confidenceStr == null || confidenceStr.isEmpty()) { errors.append("Niveau de confiance requis.<br>"); } else { try { confidence = Confidence.valueOf(confidenceStr); } catch (IllegalArgumentException e) { errors.append("Confiance invalide.<br>"); } }

         // 4. Si Erreurs -> Réafficher formulaire
        if (errors.length() > 0) {
            request.setAttribute("reviewError", errors.toString());
            // Pré-remplir avec données saisies
            Review reviewData = new Review(); reviewData.setCommentsToAuthor(commentsAuthor); reviewData.setCommentsToSc(commentsSC);
            reviewData.setRecommendation(recommendation); reviewData.setConfidence(confidence);
            request.setAttribute("reviewData", reviewData);
            // Déterminer si on était en mode édition avant l'erreur
            boolean wasEditMode = reviewDAO.findReviewByAssignmentId(assignmentId).isPresent();
             request.setAttribute("isEditMode", wasEditMode);
            showReviewForm(request, response, pcMember, assignmentId); return;
        }

        // 5. Créer ou Mettre à jour l'objet Review
        Review review = reviewDAO.findReviewByAssignmentId(assignmentId).orElse(new Review());
        review.setAssignmentId(assignmentId); review.setSubmissionId(assignment.getSubmissionId()); review.setReviewerId(pcMember.getUserId());
        review.setCommentsToAuthor(commentsAuthor.trim()); review.setCommentsToSc(commentsSC != null ? commentsSC.trim() : null);
        review.setRecommendation(recommendation); review.setConfidence(confidence); review.setReviewDate(Timestamp.valueOf(LocalDateTime.now()));

        // 6. Sauvegarde DB (Création ou MàJ)
        try {
             if (review.getReviewId() > 0) { reviewDAO.updateReview(review); }
             else { reviewDAO.createReview(review); }

            // 7. Mettre à jour statut assignation
            assignmentDAO.updateAssignmentStatus(assignmentId, AssignmentStatus.COMPLETED);

            // 8. Redirection Succès
            int conferenceId = submissionDAO.findSubmissionById(assignment.getSubmissionId()).map(Submission::getConferenceId).orElse(0);
            request.getSession().setAttribute("successMessage", "Évaluation enregistrée (Assignation ID: " + assignmentId + ").");
            response.sendRedirect(request.getContextPath() + "/review/assigned?confId=" + conferenceId);

        } catch (SQLException e) {
             request.setAttribute("reviewError", "Erreur base de données lors de l'enregistrement: " + e.getMessage());
             e.printStackTrace();
             request.setAttribute("reviewData", review); // Repasser l'objet pour pré-remplissage
             request.setAttribute("isEditMode", review.getReviewId() > 0);
             showReviewForm(request, response, pcMember, assignmentId); // Réaffiche le formulaire
        }
    }

    // ======================== Méthodes Utilitaires (Implémentées) =======================

    /** Récupère et valide l'ID ('confId') depuis la requête. */
    private int getConferenceIdFromRequest(HttpServletRequest request) throws InvalidIdException {
        String confIdParam = request.getParameter("confId");
        if (confIdParam == null || confIdParam.trim().isEmpty()) { throw new InvalidIdException("Paramètre 'confId' manquant."); }
        try { int confId = Integer.parseInt(confIdParam); if (confId <= 0) { throw new InvalidIdException("ID conf invalide: " + confIdParam); } return confId; }
        catch (NumberFormatException e) { throw new InvalidIdException("Format ID conf invalide: " + confIdParam); }
    }

    /** Récupère et valide un paramètre entier positif depuis la requête. */
    private int getIntParamFromRequest(HttpServletRequest request, String paramName) throws InvalidIdException {
        String paramValue = request.getParameter(paramName);
        if (paramValue == null || paramValue.trim().isEmpty()) { throw new InvalidIdException("Paramètre '" + paramName + "' manquant."); }
        try { int id = Integer.parseInt(paramValue); if (id <= 0) { throw new InvalidIdException("ID invalide pour '" + paramName + "': " + paramValue); } return id; }
        catch (NumberFormatException e) { throw new InvalidIdException("Format invalide pour '" + paramName + "': " + paramValue); }
    }

    /** Récupère un paramètre entier sans lancer d'exception (retourne 0 si invalide/manquant). */
    private int getIntParamFromRequestQuietly(HttpServletRequest request, String paramName) {
        String paramValue = request.getParameter(paramName);
        if (paramValue != null) { try { int id = Integer.parseInt(paramValue); if (id > 0) return id; } catch (NumberFormatException e) {} } return 0;
    }

    /** Gère les erreurs génériques. */
    private void handleGenericError(HttpServletRequest req, HttpServletResponse res, Exception e, String ctx) throws IOException, ServletException {
        System.err.println("ERREUR SERVEUR DANS REVIEW SERVLET - Contexte: " + ctx); e.printStackTrace();
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne du serveur. Consultez les logs.");
    }

    /** Exception interne pour les ID invalides */
    private static class InvalidIdException extends Exception { public InvalidIdException(String msg) { super(msg); } }

} // Fin Servlet