<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%-- Nécessaire pour la logique c:choose, c:when, c:otherwise, c:if, c:out --%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%-- Nécessaire pour fn:escapeXml --%>

<%-- ======================================================================= --%>
<%-- ============= Composant Réutilisable : Barre de Navigation ============ --%>
<%-- ======================================================================= --%>

<nav class="bg-white shadow-md sticky top-0 z-50"> <%-- Fond blanc, ombre, fixe en haut, au-dessus du reste --%>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8"> <%-- Conteneur principal avec largeur max et padding --%>
    <div class="flex justify-between h-16"> <%-- Aligne les éléments horizontalement, hauteur fixe --%>

      <%-- Logo / Nom du site (à gauche) --%>
      <div class="flex items-center">
        <a href="${pageContext.request.contextPath}/" class="flex items-center text-purple-600 hover:text-purple-800 transition duration-150 ease-in-out">
            <i class="fas fa-university text-2xl mr-2"></i> <%-- Icône Université --%>
            <span class="text-xl font-bold">ConferenceMS</span> <%-- Nom de l'application --%>
        </a>
      </div>

      <%-- Liens Utilisateur (à droite) --%>
      <div class="flex items-center">
        <c:choose>
          <%-- Cas 1: Utilisateur NON connecté --%>
          <c:when test="${empty sessionScope.user}">
            <%-- Lien vers la page de connexion --%>
            <a href="${pageContext.request.contextPath}/login"
               class="text-gray-700 hover:text-purple-600 px-3 py-2 rounded-md text-sm font-medium transition duration-150 ease-in-out">
              <i class="fas fa-sign-in-alt mr-1"></i> Connexion
            </a>
            <%-- Bouton pour s'inscrire --%>
            <a href="${pageContext.request.contextPath}/register"
               class="ml-3 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 transition duration-150 ease-in-out">
               <i class="fas fa-user-plus mr-1"></i> S'inscrire
            </a>
          </c:when>

          <%-- Cas 2: Utilisateur connecté --%>
          <c:otherwise>
             <%-- Lien vers le tableau de bord personnel --%>
             <a href="${pageContext.request.contextPath}/dashboard"
                class="text-gray-700 hover:text-purple-600 px-3 py-2 rounded-md text-sm font-medium flex items-center transition duration-150 ease-in-out">
                 <i class="fas fa-user-circle mr-2 text-lg"></i>
                 Mon Espace (<c:out value="${sessionScope.user.firstName}"/>) <%-- Affiche le prénom --%>
             </a>
             <%-- Lien de déconnexion --%>
             <a href="${pageContext.request.contextPath}/logout"
                class="ml-3 text-red-600 hover:text-red-800 px-3 py-2 rounded-md text-sm font-medium flex items-center transition duration-150 ease-in-out">
               <i class="fas fa-sign-out-alt mr-1"></i> Déconnexion
             </a>
          </c:otherwise>
        </c:choose>
      </div>

    </div>
  </div>
</nav>