package Filter; // Nouveau package

import jakarta.servlet.*; // Pour Filter, FilterChain, etc.
import jakarta.servlet.annotation.WebFilter; // Pour l'annotation
import jakarta.servlet.http.HttpServletRequest; // Pour caster la requête
import jakarta.servlet.http.HttpServletResponse; // Pour caster la réponse
import jakarta.servlet.http.HttpSession; // Pour vérifier la session

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Ce filtre intercepte les requêtes pour vérifier si l'utilisateur est authentifié
 * avant d'autoriser l'accès aux ressources protégées.
 */
// Appliquer ce filtre à TOUTES les requêtes "/*"
// Nous allons ensuite exclure les pages publiques à l'intérieur du filtre.
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"/*"})
public class AuthenticationFilter implements Filter {

    // URLs qui NE nécessitent PAS d'authentification
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/login",
            "/register",
            "/index.jsp", // Si index.jsp est votre page d'accueil publique
            "/home.jsp",  // Ou si home.jsp est votre page d'accueil publique
            "/"           // La racine de l'application (redirige souvent vers index ou login)
    ));

     // Préfixes de chemins publics (ex: pour les ressources CSS, JS, Images)
     // Si vous avez des dossiers /css, /js, /images directement sous WebContent
     private static final Set<String> PUBLIC_PREFIXES = new HashSet<>(Arrays.asList(
            "/css/",
            "/js/",
            "/images/",
            "/uploads/" // Potentiellement, si certains uploads sont publics (logos?), sinon à protéger
     ));


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Méthode d'initialisation du filtre (peut être laissée vide pour cet exemple)
        System.out.println("AuthenticationFilter initialisé.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false); // Récupère la session SANS en créer une nouvelle

        // Obtenir le chemin demandé par l'utilisateur, SANS le context path
        String contextPath = httpRequest.getContextPath();
        String requestURI = httpRequest.getRequestURI();
        String path = requestURI.substring(contextPath.length()); // Ex: /login, /dashboard, /css/style.css

        // --- Vérification si le chemin est public ---
        boolean isPublicPath = PUBLIC_PATHS.contains(path);
        boolean isPublicResource = false;
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                isPublicResource = true;
                break;
            }
        }

         // Laisser passer si c'est un chemin public ou une ressource publique
        if (isPublicPath || isPublicResource) {
             System.out.println("AuthenticationFilter: Chemin public détecté: " + path + " -> Laissé passer.");
            chain.doFilter(request, response); // Continue vers la ressource demandée (servlet ou fichier)
            return; // Important de s'arrêter ici
        }

        // --- Vérification de l'Authentification pour les chemins protégés ---
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);

        if (isLoggedIn) {
             // L'utilisateur est connecté, laisser passer la requête vers la ressource protégée
             System.out.println("AuthenticationFilter: Utilisateur connecté accédant à: " + path + " -> Laissé passer.");
            chain.doFilter(request, response);
        } else {
            // L'utilisateur n'est PAS connecté et essaie d'accéder à une ressource protégée
            System.out.println("AuthenticationFilter: Utilisateur NON connecté tentant d'accéder à: " + path + " -> Redirection vers /login.");
            // Rediriger vers la page de connexion
            // On peut ajouter un message ou l'URL demandée en paramètre si on veut le rediriger après connexion
            httpResponse.sendRedirect(contextPath + "/login?error=login_required&requestedURI=" + requestURI);
        }
    }

    @Override
    public void destroy() {
        // Méthode appelée lors de l'arrêt de l'application (peut être laissée vide)
         System.out.println("AuthenticationFilter détruit.");
    }
}