<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%-- Nécessaire pour la logique conditionnelle et les boucles --%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%-- Optionnel, mais utile pour des fonctions comme escapeXml --%>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>ConferenceMS - Accueil</title>

    <!-- Tailwind CSS via CDN -->
    <script src="https://cdn.tailwindcss.com"></script>

    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet"/>

    <!-- Font Awesome pour les icônes -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>

    <style>
        /* Style de base pour utiliser la police Inter */
        body {
            font-family: "Inter", sans-serif;
            /* Optionnel: Empêcher le contenu de coller au footer sur petit écran */
            padding-bottom: 8rem; /* Ajuster si le footer est plus grand */
        }
        @media (min-width: 768px) { /* md */
             body { padding-bottom: 0; } /* Pas besoin sur écran large */
        }
    </style>
</head>
<body class="bg-gray-100"> <%-- Fond légèrement gris --%>

    <!-- Barre de Navigation -->
    <nav class="bg-white shadow-md sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <!-- Logo/Nom du site -->
          <div class="flex items-center">
            <a href="${pageContext.request.contextPath}/" class="flex items-center text-purple-600 hover:text-purple-800">
                <i class="fas fa-university text-2xl mr-2"></i>
                <span class="text-xl font-bold">ConferenceMS</span>
            </a>
          </div>

          <!-- Liens Utilisateur (Connexion/Inscription ou Mon Espace/Déconnexion) -->
          <div class="flex items-center">
            <c:choose>
              <%-- Cas 1: L'utilisateur N'EST PAS connecté (pas d'attribut 'user' en session) --%>
              <c:when test="${empty sessionScope.user}">
                <a href="${pageContext.request.contextPath}/login.jsp" <%-- Lien vers le servlet de login --%>
                   class="text-gray-700 hover:text-purple-600 px-3 py-2 rounded-md text-sm font-medium transition duration-150 ease-in-out">
                  <i class="fas fa-sign-in-alt mr-1"></i> Connexion
                </a>
                <a href="${pageContext.request.contextPath}/register.jsp" <%-- Lien vers le servlet d'inscription --%>
                   class="ml-3 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 transition duration-150 ease-in-out">
                   <i class="fas fa-user-plus mr-1"></i> S'inscrire
                </a>
              </c:when>

              <%-- Cas 2: L'utilisateur EST connecté --%>
              <c:otherwise>
                 <%-- Lien vers le tableau de bord (Dashboard) --%>
                 <a href="${pageContext.request.contextPath}/dashboard
  "
                    class="text-gray-700 hover:text-purple-600 px-3 py-2 rounded-md text-sm font-medium flex items-center transition duration-150 ease-in-out">
                     <i class="fas fa-user-circle mr-2 text-lg"></i> <%-- Icône utilisateur --%>
                     Mon Espace (<c:out value="${sessionScope.user.firstName}"/>)
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

    <!-- Section Principale (Hero) -->
    <div class="relative bg-gradient-to-br from-purple-700 to-indigo-800 text-white overflow-hidden">
        <div class="max-w-7xl mx-auto py-20 px-4 sm:px-6 lg:px-8">
            <div class="text-center">
                <h1 class="text-4xl font-extrabold tracking-tight sm:text-5xl lg:text-6xl">
                    <span class="block">Plateforme de Gestion</span>
                    <span class="block text-yellow-300">Conférences Scientifiques</span>
                </h1>
                <p class="mt-6 max-w-lg mx-auto text-xl text-purple-100 sm:max-w-3xl">
                    Simplifiez chaque étape de l'organisation de votre conférence, de la soumission d'articles à la publication des articles.
                </p>
                <div class="mt-10 max-w-sm mx-auto sm:max-w-none sm:flex sm:justify-center space-y-4 sm:space-y-0 sm:space-x-4">
                    <%-- Bouton "Explorer les conférences" - Lien placeholder pour l'instant --%>
                    <a href="#" <%-- TODO: Remplacer par le lien vers la liste des conférences publiques --%>
                       class="flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md shadow-sm text-purple-700 bg-white hover:bg-gray-100 transition duration-150 ease-in-out">
                        <i class="fas fa-search mr-2"></i> cree conference
                    </a>

                     <%-- Bouton "Soumettre un article" - Conditionnel --%>
                     <c:choose>
                        <c:when test="${empty sessionScope.user}">
                             <%-- Si non connecté, pointe vers le login --%>
                            <a href="${pageContext.request.contextPath}/login"
                               class="flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md shadow-sm text-indigo-800 bg-yellow-400 hover:bg-yellow-300 transition duration-150 ease-in-out">
                                <i class="fas fa-paper-plane mr-2"></i> Soumettre un article <span class="ml-1 text-xs">(Connexion)</span>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <%-- Si connecté, pointe vers la page de soumission (à créer) --%>
                             <a href="${pageContext.request.contextPath}/submission/submit" <%-- TODO: Créer le servlet/jsp de soumission --%>
                               class="flex items-center justify-center px-6 py-3 border border-transparent text-base font-medium rounded-md shadow-sm text-indigo-800 bg-yellow-400 hover:bg-yellow-300 transition duration-150 ease-in-out">
                                <i class="fas fa-paper-plane mr-2"></i> Soumettre un article
                            </a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>

    <!-- Section Fonctionnalités -->
    <div class="py-16 bg-gray-50 overflow-hidden">
      <div class="max-w-7xl mx-auto px-4 space-y-8 sm:px-6 lg:px-8">
        <div class="text-center">
            <h2 class="text-3xl font-extrabold text-gray-900 sm:text-4xl">
                Une solution complète
            </h2>
            <p class="mt-4 text-lg text-gray-500">
                Conçue pour les organisateurs, auteurs et évaluateurs.
            </p>
        </div>

        <div class="grid grid-cols-1 gap-y-10 sm:grid-cols-2 lg:grid-cols-3 gap-x-8">
            <!-- Feature 1 -->
            <div class="flex flex-col bg-white rounded-lg shadow-lg overflow-hidden">
                <div class="flex-shrink-0 bg-purple-600 p-4 flex items-center justify-center">
                    <i class="fas fa-file-upload text-4xl text-white"></i>
                </div>
                <div class="flex-1 p-6 flex flex-col justify-between">
                    <div class="flex-1">
                        <p class="text-xl font-semibold text-gray-900">Soumissions Simplifiées</p>
                        <p class="mt-3 text-base text-gray-500">Interface intuitive pour les auteurs pour soumettre et suivre leurs articles.</p>
                    </div>
                </div>
            </div>
            <!-- Feature 2 -->
             <div class="flex flex-col bg-white rounded-lg shadow-lg overflow-hidden">
                <div class="flex-shrink-0 bg-purple-600 p-4 flex items-center justify-center">
                    <i class="fas fa-users-cog text-4xl text-white"></i>
                </div>
                <div class="flex-1 p-6 flex flex-col justify-between">
                    <div class="flex-1">
                        <p class="text-xl font-semibold text-gray-900">Gestion des Évaluateurs</p>
                        <p class="mt-3 text-base text-gray-500">Assignation efficace des articles aux membres du comité de programme (PC).</p>
                    </div>
                </div>
            </div>
             <!-- Feature 3 -->
            <div class="flex flex-col bg-white rounded-lg shadow-lg overflow-hidden">
                <div class="flex-shrink-0 bg-purple-600 p-4 flex items-center justify-center">
                    <i class="fas fa-tasks text-4xl text-white"></i>
                </div>
                <div class="flex-1 p-6 flex flex-col justify-between">
                    <div class="flex-1">
                        <p class="text-xl font-semibold text-gray-900">Suivi d'Évaluation</p>
                        <p class="mt-3 text-base text-gray-500">Tableaux de bord pour suivre l'avancement des évaluations et prendre des décisions éclairées (SC).</p>
                    </div>
                </div>
            </div>
            <%-- Ajouter d'autres fonctionnalités si nécessaire --%>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <footer class="bg-gray-800 mt-auto"> <%-- mt-auto pousse le footer en bas si le contenu est court --%>
      <div class="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between">
        <div class="text-center sm:text-left text-sm text-gray-400">
          © ${java.time.Year.now()} ConferenceMS. Tous droits réservés.
        </div>
        <div class="flex space-x-6 mt-4 sm:mt-0">
          <a href="#" class="text-gray-400 hover:text-white">
            <span class="sr-only">Twitter</span> <%-- Pour l'accessibilité --%>
            <i class="fab fa-twitter"></i>
          </a>
          <a href="#" class="text-gray-400 hover:text-white">
            <span class="sr-only">LinkedIn</span>
            <i class="fab fa-linkedin-in"></i>
          </a>
           <%-- Ajouter d'autres liens sociaux ou légaux --%>
        </div>
      </div>
    </footer>

</body>
</html>