package servlets;


import DAO.ConferenceDAOImpl;
 // Assurez-vous que l'implémentation existe
import models.Conference;
import models.User;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map; // Pour stocker Conference -> List<Role>

/**
 * Servlet gérant l'affichage du tableau de bord principal de l'utilisateur.
 * Ce servlet est appelé après une connexion réussie.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private  ConferenceDAOImpl conferenceDAO;
    // On pourrait aussi avoir besoin de UserConferenceRoleDAO ici pour une logique plus fine

    @Override
    public void init() throws ServletException {
        super.init();
        // Instanciation du DAO nécessaire
        this.conferenceDAO = new ConferenceDAOImpl();
        // this.userConferenceRoleDAO = new UserConferenceRoleDAOImpl(); // Si besoin
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false); // Ne pas créer de session si elle n'existe pas

        // --- Vérification de Sécurité : Utilisateur connecté ? ---
        // Normalement, un filtre s'occuperait de ça, mais ajoutons une vérif ici aussi.
        if (session == null || session.getAttribute("user") == null) {
            // Utilisateur non connecté, rediriger vers la page de login
            response.sendRedirect(request.getContextPath() + "/login?error=session_expired"); // Message optionnel
            return; // Important d'arrêter l'exécution ici
        }

        // Récupérer l'utilisateur depuis la session
        User currentUser = (User) session.getAttribute("user");

        try {
            // --- Récupération des Données ---
            // Utiliser la méthode du DAO qui récupère les conférences ET les rôles
            Map<Conference, List<String>> userConferencesWithRoles = conferenceDAO.findUserConferencesAndRoles(currentUser.getUserId());

            // Transmettre les données à la page JSP
            request.setAttribute("conferencesMap", userConferencesWithRoles);
            request.setAttribute("userName", currentUser.getFirstName()); // Pour un message d'accueil

            // Afficher la page JSP du dashboard
            RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp");
            dispatcher.forward(request, response);

        } catch (SQLException e) {
            // Gérer les erreurs de base de données
            e.printStackTrace(); // Loguer l'erreur
            // Afficher une page d'erreur ou un message sur le dashboard
            request.setAttribute("dashboardError", "Impossible de charger les informations des conférences. Erreur base de données.");
             RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp"); // Afficher quand même le dashboard avec l'erreur
            dispatcher.forward(request, response);
            // Ou rediriger vers une page d'erreur générale :
            // response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur lors de la récupération des données.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Normalement, le dashboard n'a pas d'actions POST, on redirige vers GET
        doGet(request, response);
    }
}
