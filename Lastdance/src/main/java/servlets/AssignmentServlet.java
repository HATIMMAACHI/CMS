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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Imports DAO (Adapter le nom du package si besoin)
import DAO.AssignmentDAOImpl;
import DAO.ConferenceDAOImpl;
import DAO.SubmissionDAOImpl;
import DAO.UserConferenceRoleDAOImpl;
// import DAO.UserDAOImpl; // Pas forcément requis ici directement

// Imports Models (Adapter le nom du package si besoin)
import models.Assignment;
import models.Conference;
import models.Submission;
import models.Submission.SubmissionStatus;
import models.User;
import models.UserConferenceRole.Role;

/**
 * Servlet gérant l'assignation des articles soumis aux membres du Comité de Programme (PC).
 * Accès réservé aux membres du Comité Scientifique (SC).
 */
@WebServlet(name = "AssignmentServlet", urlPatterns = {
        "/assignment/list",
        "/assignment/assign"// GET: Affiche la liste des papiers à assigner
          // POST: Traite l'assignation d'un papier à un ou plusieurs PC members
})
public class AssignmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private SubmissionDAOImpl submissionDAO;
    private UserConferenceRoleDAOImpl userConferenceRoleDAO;
    private AssignmentDAOImpl assignmentDAO;
    private ConferenceDAOImpl conferenceDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        // Initialisation des DAOs nécessaires
        this.submissionDAO = new SubmissionDAOImpl();
        this.userConferenceRoleDAO = new UserConferenceRoleDAOImpl();
        this.assignmentDAO = new AssignmentDAOImpl();
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
            if ("/assignment/list".equals(actionPath)) {
                showAssignmentList(request, response, currentUser); // Appelle la méthode pour afficher la liste
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Action GET inconnue pour l'assignation.");
            }
        } catch (InvalidConferenceIdException e) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) { handleGenericError(request, response, e, "Erreur DB (GET Assignment)");
        } catch (Exception e) { handleGenericError(request, response, e, "Erreur serveur (GET Assignment)"); }
    }

    // ======================== doPost Router =======================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non autorisé."); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();

        try {
             if ("/assignment/assign".equals(actionPath)) {
                processAssignment(request, response, currentUser); // Appelle la méthode pour traiter l'assignation
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Action POST inconnue pour l'assignation.");
            }
        } catch (InvalidConferenceIdException | InvalidSubmissionOrPcIdException e) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
             // Gérer l'erreur SQL en tentant de réafficher la liste avec un message
             int confIdForRetry = getConferenceIdFromRequestQuietly(request);
             request.setAttribute("assignmentError", "Erreur base de données lors de l'assignation: " + e.getMessage());
             e.printStackTrace();
             try { showAssignmentList(request, response, currentUser); } // Tente de réafficher la liste
             catch (Exception displayError) { handleGenericError(request, response, e, "Erreur DB (POST Assign) + échec réaffichage"); }
        } catch (Exception e) { handleGenericError(request, response, e, "Erreur serveur (POST Assign)"); }
    }

    // ======================== Méthodes Actions =======================

    /** Prépare les données et affiche la page de liste d'assignation. */
    private void showAssignmentList(HttpServletRequest request, HttpServletResponse response, User currentUser)
        throws ServletException, IOException, SQLException, InvalidConferenceIdException {

        int conferenceId = getConferenceIdFromRequest(request); // Récupère et valide confId

        // Sécurité: Vérifier si l'utilisateur est SC Member pour cette conférence
        if (!userConferenceRoleDAO.userHasRole(currentUser.getUserId(), conferenceId, Role.SC_MEMBER)) {
             System.err.println("ALERTE SECURITE: User " + currentUser.getUserId() + " (non SC) a tenté d'accéder à /assignment/list pour conf " + conferenceId);
             response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès réservé aux membres du Comité Scientifique.");
             return;
        }

        // Récupérer les infos de la conférence
         Optional<Conference> confOpt = conferenceDAO.findConferenceById(conferenceId);
         if (confOpt.isEmpty()) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Conférence ID " + conferenceId + " non trouvée."); return; }

        // Récupérer les soumissions à assigner (Statut SUBMITTED)
        List<Submission> submissionsToAssign = submissionDAO.findSubmissionsByConferenceAndStatus(conferenceId, SubmissionStatus.SUBMITTED);

        // Récupérer la liste des PC Members disponibles
        List<User> pcMembers = userConferenceRoleDAO.findUsersByConferenceAndRole(conferenceId, Role.PC_MEMBER);

        // Récupérer les assignations actuelles pour chaque soumission
        Map<Integer, Integer> assignmentCounts = new HashMap<>();
        Map<Integer, List<Integer>> currentAssignments = new HashMap<>();
        for (Submission sub : submissionsToAssign) {
             List<Assignment> assignments = assignmentDAO.findAssignmentsBySubmissionId(sub.getSubmissionId());
             assignmentCounts.put(sub.getSubmissionId(), assignments.size());
             currentAssignments.put(sub.getSubmissionId(), assignments.stream().map(Assignment::getPcMemberId).collect(Collectors.toList()));
        }

        // Transmettre les données à la JSP
        request.setAttribute("conference", confOpt.get());
        request.setAttribute("submissions", submissionsToAssign);
        request.setAttribute("pcMembers", pcMembers);
        request.setAttribute("assignmentCounts", assignmentCounts);
        request.setAttribute("currentAssignments", currentAssignments);
        request.setAttribute("confId", conferenceId);

        // Afficher la JSP
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/assignment/assign_papers.jsp");
        dispatcher.forward(request, response);
    }

    /** Traite l'assignation d'un article à un ou plusieurs PC Members. */
    private void processAssignment(HttpServletRequest request, HttpServletResponse response, User currentUser)
        throws ServletException, IOException, SQLException, InvalidConferenceIdException, InvalidSubmissionOrPcIdException {

         int conferenceId = getConferenceIdFromRequest(request); // Valide confId

         // Sécurité: Vérifier si l'utilisateur est SC Member
        if (!userConferenceRoleDAO.userHasRole(currentUser.getUserId(), conferenceId, Role.SC_MEMBER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Action réservée aux membres du Comité Scientifique.");
            return;
        }

        // Récupérer les IDs de la requête
        int submissionId = 0;
        String submissionIdParam = request.getParameter("submissionId");
        if(submissionIdParam != null) { try { submissionId = Integer.parseInt(submissionIdParam); if(submissionId <= 0) throw new NumberFormatException();} catch(NumberFormatException e) { throw new InvalidSubmissionOrPcIdException("ID de soumission invalide.");} }
        else { throw new InvalidSubmissionOrPcIdException("ID de soumission manquant."); }

        String[] pcMemberIdsParam = request.getParameterValues("pcMemberIds_" + submissionId); // Param spécifique
        List<Integer> pcMemberIdsToAssign = new ArrayList<>();
        if (pcMemberIdsParam != null) {
            try { for (String id : pcMemberIdsParam) { pcMemberIdsToAssign.add(Integer.parseInt(id.trim())); } }
            catch (NumberFormatException e) { throw new InvalidSubmissionOrPcIdException("Format d'ID PC Member invalide."); }
        }

        System.out.println("Assignation pour submission " + submissionId + " -> PC Members: " + pcMemberIdsToAssign + " par SC " + currentUser.getUserId());

        int assignmentsCreated = 0; // Compter seulement les NOUVELLES assignations créées
        int alreadyAssignedCount = 0; // Compter ceux déjà assignés
        StringBuilder assignmentErrors = new StringBuilder();

        // Vérifier la validité des PC members et les assignations existantes
        List<Integer> currentlyAssignedPcIds = assignmentDAO.findAssignmentsBySubmissionId(submissionId)
                                                    .stream().map(Assignment::getPcMemberId).collect(Collectors.toList());
        List<Integer> validPcMemberIdsForConf = userConferenceRoleDAO.findUsersByConferenceAndRole(conferenceId, Role.PC_MEMBER)
                                                    .stream().map(User::getUserId).collect(Collectors.toList());

        for (int pcMemberId : pcMemberIdsToAssign) {
            if (!validPcMemberIdsForConf.contains(pcMemberId)) {
                 assignmentErrors.append("ID PC Member ").append(pcMemberId).append(" invalide.<br>"); continue;
            }
            if (currentlyAssignedPcIds.contains(pcMemberId)) {
                 alreadyAssignedCount++; continue; // Déjà assigné
            }
            // Créer la nouvelle assignation
            Assignment newAssignment = new Assignment();
            newAssignment.setSubmissionId(submissionId);
            newAssignment.setPcMemberId(pcMemberId);
            newAssignment.setAssignedById(currentUser.getUserId());
            newAssignment.setStatus(Assignment.AssignmentStatus.PENDING);
            try {
                assignmentDAO.createAssignment(newAssignment);
                assignmentsCreated++;
            } catch (SQLException e) {
                 assignmentErrors.append("Erreur DB assignation PC ID ").append(pcMemberId).append(": ").append(e.getMessage()).append("<br>"); e.printStackTrace();
            }
        }

         // Mettre à jour le statut de la soumission si nécessaire
         // (si au moins une nouvelle assignation OU si des PC étaient sélectionnés même s'ils étaient déjà assignés)
         if (assignmentsCreated > 0 || (alreadyAssignedCount > 0 && pcMemberIdsToAssign.size() == alreadyAssignedCount)) {
             try {
                // Vérifier si le statut est encore SUBMITTED avant de le changer
                 Optional<Submission> subOpt = submissionDAO.findSubmissionById(submissionId);
                 if (subOpt.isPresent() && subOpt.get().getStatus() == SubmissionStatus.SUBMITTED) {
                    submissionDAO.updateSubmissionStatus(submissionId, SubmissionStatus.UNDER_REVIEW);
                    System.out.println("Statut de submission " + submissionId + " mis à UNDER_REVIEW.");
                 }
             } catch (SQLException e) {
                 assignmentErrors.append("Erreur màj statut soumission: ").append(e.getMessage()).append("<br>"); e.printStackTrace();
             }
         }

        // Préparer le message pour la JSP
        if (assignmentErrors.length() > 0) { request.setAttribute("assignmentError", "Erreurs lors de l'assignation :<br>" + assignmentErrors.toString()); }
        else if (assignmentsCreated > 0) { request.setAttribute("assignmentSuccess", assignmentsCreated + " nouvelle(s) assignation(s) enregistrée(s) pour l'article ID " + submissionId + "."); }
        else if (alreadyAssignedCount > 0 && pcMemberIdsToAssign.size() > 0) { request.setAttribute("assignmentInfo", "Les PC Members sélectionnés étaient déjà assignés à l'article ID " + submissionId + "."); }
        else { request.setAttribute("assignmentInfo", "Aucun nouveau PC Member sélectionné ou à assigner pour l'article ID " + submissionId + "."); }

        // Réafficher la liste des assignations
        showAssignmentList(request, response, currentUser);
    }

    // ======================== Méthodes Utilitaires (Implémentées) =======================

    /** Récupère et valide l'ID de conférence ('confId') depuis les paramètres de la requête. */
    private int getConferenceIdFromRequest(HttpServletRequest request) throws InvalidConferenceIdException {
        String confIdParam = request.getParameter("confId");
        if (confIdParam == null || confIdParam.trim().isEmpty()) { throw new InvalidConferenceIdException("Paramètre 'confId' manquant ou vide."); }
        try { int confId = Integer.parseInt(confIdParam); if (confId <= 0) { throw new InvalidConferenceIdException("ID de conférence invalide: " + confIdParam); } return confId; }
        catch (NumberFormatException e) { throw new InvalidConferenceIdException("Format de l'ID de conférence invalide: " + confIdParam); }
    }

    /** Récupère l'ID de conférence sans lancer d'exception (retourne 0 si invalide/manquant). */
    private int getConferenceIdFromRequestQuietly(HttpServletRequest request) {
        String confIdParam = request.getParameter("confId");
        if (confIdParam != null) { try { int confId = Integer.parseInt(confIdParam); if (confId > 0) return confId; } catch (NumberFormatException e) {} } return 0;
    }

    /** Gère les erreurs génériques (SQL ou autres). */
    private void handleGenericError(HttpServletRequest req, HttpServletResponse res, Exception e, String ctx) throws IOException, ServletException {
        System.err.println("ERREUR SERVEUR DANS ASSIGNMENT SERVLET - Contexte: " + ctx); e.printStackTrace();
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Une erreur interne est survenue. Consultez les logs du serveur.");
    }

    // ======================== Exceptions Internes =======================
    private static class InvalidConferenceIdException extends Exception { public InvalidConferenceIdException(String msg) { super(msg); } }
    private static class InvalidSubmissionOrPcIdException extends Exception { public InvalidSubmissionOrPcIdException(String msg) { super(msg); } }

}// Fin Servlet
