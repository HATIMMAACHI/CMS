package servlets; // Package correct

// Imports - PasswordUtil a été retiré
import DAO.UserDAOImpl; // Assurez-vous d'importer l'interface
import models.User;
// import utils.PasswordUtil; // RETIRÉ

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Servlet gérant l'authentification (connexion, inscription, déconnexion).
 * !!! VERSION NON SÉCURISÉE - MOTS DE PASSE EN CLAIR !!!
 * À N'UTILISER QUE POUR DÉMONSTRATION/APPRENTISSAGE TEMPORAIRE.
 * NE JAMAIS UTILISER EN PRODUCTION !
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UserDAOImpl userDAO; // Utiliser l'interface ici est une meilleure pratique

    @Override
    public void init() throws ServletException {
        super.init();
        this.userDAO = new UserDAOImpl(); // Instanciation de l'implémentation
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getServletPath();
        switch (action) {
            case "/login":
                showLoginForm(request, response);
                break;
            case "/register":
                showRegistrationForm(request, response);
                break;
            case "/logout":
                doLogout(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getServletPath();
        switch (action) {
            case "/login":
                doLogin(request, response);
                break;
            case "/register":
                doRegister(request, response);
                break;
            default:
                 response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void showLoginForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getSession(false) != null && request.getSession(false).getAttribute("user") != null) {
             response.sendRedirect(request.getContextPath() + "/dashboard");
             return;
        }
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/login.jsp");
        dispatcher.forward(request, response);
    }

    private void showRegistrationForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getSession(false) != null && request.getSession(false).getAttribute("user") != null) {
             response.sendRedirect(request.getContextPath() + "/dashboard");
             return;
        }
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/register.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * Traite la connexion avec comparaison de mot de passe EN CLAIR.
     * !!! NON SÉCURISÉ !!!
     */
    private void doLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password"); // Mot de passe entré par l'utilisateur
        HttpSession session = request.getSession();

        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("errorMessage", "L'email et le mot de passe sont requis.");
            showLoginForm(request, response);
            return;
        }

        try {
            Optional<User> userOptional = userDAO.findUserByEmail(email.trim());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                String storedPassword = user.getPasswordHash(); // Récupère le mot de passe EN CLAIR stocké

                // --- MODIFICATION ICI : Comparaison directe des mots de passe en clair ---
                if (password.equals(storedPassword)) {
                // -----------------------------------------------------------------------
                    // Authentification réussie !
                    session.setAttribute("user", user);
                    // Charger les rôles ici si nécessaire...
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                } else {
                    // Mot de passe incorrect
                    request.setAttribute("errorMessage", "Email ou mot de passe invalide.");
                    request.setAttribute("emailValue", email);
                    showLoginForm(request, response);
                }
            } else {
                // Utilisateur non trouvé
                request.setAttribute("errorMessage", "Email ou mot de passe invalide.");
                request.setAttribute("emailValue", email);
                showLoginForm(request, response);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Erreur serveur lors de la connexion. Veuillez réessayer.");
            showLoginForm(request, response);
        }
    }

    /**
     * Traite l'inscription en stockant le mot de passe EN CLAIR.
     * !!! NON SÉCURISÉ !!!
     */
    private void doRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password"); // Mot de passe en clair
        String confirmPassword = request.getParameter("confirmPassword");
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String affiliation = request.getParameter("affiliation");

        // --- Validation des entrées (reste identique) ---
        if (email == null || email.trim().isEmpty() ||
            password == null || password.isEmpty() || // Validation importante
            confirmPassword == null || confirmPassword.isEmpty() ||
            firstName == null || firstName.trim().isEmpty() ||
            lastName == null || lastName.trim().isEmpty()) {

            request.setAttribute("errorMessage", "Tous les champs obligatoires (*) doivent être remplis.");
            request.setAttribute("emailValue", email);
            request.setAttribute("firstNameValue", firstName);
            request.setAttribute("lastNameValue", lastName);
            request.setAttribute("affiliationValue", affiliation);
            showRegistrationForm(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "Les mots de passe ne correspondent pas.");
            request.setAttribute("emailValue", email);
            request.setAttribute("firstNameValue", firstName);
            request.setAttribute("lastNameValue", lastName);
            request.setAttribute("affiliationValue", affiliation);
            showRegistrationForm(request, response);
            return;
        }

        // --- Logique d'inscription ---
        try {
            if (userDAO.findUserByEmail(email.trim()).isPresent()) {
                request.setAttribute("errorMessage", "Cette adresse email est déjà utilisée.");
                request.setAttribute("emailValue", email);
                request.setAttribute("firstNameValue", firstName);
                request.setAttribute("lastNameValue", lastName);
                request.setAttribute("affiliationValue", affiliation);
                showRegistrationForm(request, response);
                return;
            }

            // --- MODIFICATION ICI : Pas de hachage ---
            // String hashedPassword = PasswordUtil.hashPassword(password); // Ligne retirée
            // ----------------------------------------

            // Créer l'objet User
            User newUser = new User();
            newUser.setEmail(email.trim());
            // --- MODIFICATION ICI : Stockage du mot de passe en clair ---
            newUser.setPasswordHash(password); // On stocke directement le mot de passe ! MAUVAIS !
            // ----------------------------------------------------------
            newUser.setFirstName(firstName.trim());
            newUser.setLastName(lastName.trim());
            newUser.setAffiliation(affiliation != null ? affiliation.trim() : null);

            userDAO.createUser(newUser);

            HttpSession session = request.getSession();
            session.setAttribute("successMessage", "Inscription réussie ! Vous pouvez maintenant vous connecter.");
            response.sendRedirect(request.getContextPath() + "/login");

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Erreur serveur lors de l'inscription. Veuillez réessayer.");
            request.setAttribute("emailValue", email);
            request.setAttribute("firstNameValue", firstName);
            request.setAttribute("lastNameValue", lastName);
            request.setAttribute("affiliationValue", affiliation);
            showRegistrationForm(request, response);
        }
        // Le catch (IllegalArgumentException iae) lié à PasswordUtil a été retiré car non pertinent ici.
    }

    private void doLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login?logout=true");
    }
}