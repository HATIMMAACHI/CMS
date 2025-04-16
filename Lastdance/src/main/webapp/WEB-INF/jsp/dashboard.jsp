<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité: Rediriger si non connecté --%>
<c:if test="${empty sessionScope.user}">
    <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" />
</c:if>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tableau de Bord - ConferenceMS</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <style>
        body { font-family: 'Inter', sans-serif; }
        .role-badge { display: inline-block; padding: 0.2em 0.6em; font-size: 0.75rem; font-weight: 600; line-height: 1; text-align: center; white-space: nowrap; vertical-align: baseline; border-radius: 0.375rem; margin-right: 0.5rem; margin-bottom: 0.25rem; }
        .badge-author { background-color: #DBEAFE; color: #1E40AF; } /* blue-100 / blue-800 */
        /* Correction: Classe CSS pour PC Member */
        .badge-pc-member { background-color: #D1FAE5; color: #065F46; } /* green-100 / green-800 */
         /* Correction: Classe CSS pour SC Member */
        .badge-sc-member { background-color: #FEF3C7; color: #92400E; } /* yellow-100 / yellow-800 */
        .badge-chair { background-color: #FEE2E2; color: #991B1B; } /* red-100 / red-800 */
         /* Correction: Classe CSS pour Steering Committee */
        .badge-steering-committee { background-color: #E5E7EB; color: #1F2937; } /* gray-200 / gray-800 */
    </style>
</head>
<body class="bg-gray-100">

    <%-- Barre de Navigation --%>
    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8">

        <header class="mb-8">
            <h1 class="text-3xl font-bold leading-tight text-gray-900">
                Bienvenue, <c:out value="${sessionScope.user.firstName}"/> !
            </h1>
            <p class="mt-2 text-lg text-gray-600">Vos conférences et rôles actuels.</p>
        </header>

        <%-- Affichage des Messages (Erreurs, Succès, Infos) --%>
        <c:if test="${not empty dashboardError}"> <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-6" role="alert"><strong class="font-bold">Erreur:</strong> <span class="block sm:inline"><c:out value="${dashboardError}"/></span></div> </c:if>
        <c:if test="${not empty sessionScope.successMessage}"> <div class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded relative mb-6" role="alert"><strong class="font-bold">Succès:</strong> <span class="block sm:inline"><c:out value="${sessionScope.successMessage}"/></span></div> <c:remove var="successMessage" scope="session" /> </c:if>
        <c:if test="${param.logout == 'true'}"> <div class="bg-blue-100 border border-blue-400 text-blue-700 px-4 py-3 rounded relative mb-6" role="alert">Vous avez été déconnecté avec succès.</div> </c:if>
        <c:if test="${param.error == 'login_required' || param.error == 'unauthorized'}"> <div class="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded relative mb-6" role="alert"><i class="fas fa-exclamation-triangle mr-2"></i> Vous devez être connecté pour accéder à cette page.</div> </c:if>
        <c:if test="${param.error == 'access_denied_for_conf'}"> <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-6" role="alert"><i class="fas fa-exclamation-triangle mr-2"></i> Accès refusé à la section demandée pour la conférence <c:out value="${param.confId}"/>.</div> </c:if>
        <c:if test="${not empty sessionScope.errorMessage}"> <div class="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded relative mb-6" role="alert"><i class="fas fa-exclamation-triangle mr-2"></i> <c:out value="${sessionScope.errorMessage}"/></div> <c:remove var="errorMessage" scope="session" /> </c:if>


        <%-- Section des Conférences --%>
        <div class="bg-white shadow overflow-hidden sm:rounded-lg">
            <div class="px-4 py-5 sm:px-6 border-b border-gray-200">
                <h3 class="text-lg leading-6 font-medium text-gray-900">Mes Conférences</h3>
                <p class="mt-1 max-w-2xl text-sm text-gray-500">Liste des conférences où vous avez un rôle actif.</p>
            </div>

            <c:choose>
                <c:when test="${empty conferencesMap}">
                     <div class="text-center px-4 py-12">
                         <i class="fas fa-folder-open fa-4x text-gray-300 mb-4"></i>
                        <p class="text-gray-500">Vous n'êtes associé à aucune conférence pour le moment.</p>
                         <a href="${pageContext.request.contextPath}/conference/create" class="mt-6 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500">
                             <i class="fas fa-plus mr-2"></i> Créer une conférence
                         </a>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="overflow-x-auto">
                        <table class="min-w-full divide-y divide-gray-200">
                            <thead class="bg-gray-50">
                                <tr>
                                    <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Conférence</th>
                                    <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Vos Rôles</th>
                                    <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden md:table-cell">Dates Clés</th>
                                    <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Accès / Interfaces</th>
                                </tr>
                            </thead>
                            <tbody class="bg-white divide-y divide-gray-200">
                                <c:forEach var="entry" items="${conferencesMap}">
                                    <c:set var="conf" value="${entry.key}" />
                                    <c:set var="roles" value="${entry.value}" />
                                    <tr>
                                        <%-- Colonne Conférence --%>
                                        <td class="px-6 py-4 whitespace-nowrap">
                                            <div class="flex items-center">
                                                 <div class="flex-shrink-0 h-10 w-10">
                                                     <c:choose>
                                                          <c:when test="${not empty conf.logoPath}"><img class="h-10 w-10 rounded-full object-cover" src="${pageContext.request.contextPath}/uploads/logos/${fn:escapeXml(conf.logoPath)}" alt="Logo"></c:when>
                                                          <c:otherwise><span class="h-10 w-10 rounded-full bg-purple-100 flex items-center justify-center"><i class="fas fa-university text-purple-600"></i></span></c:otherwise>
                                                     </c:choose>
                                                 </div>
                                                <div class="ml-4">
                                                    <div class="text-sm font-medium text-gray-900"><c:out value="${conf.name}"/> (<c:out value="${conf.acronym}"/>)</div>
                                                    <div class="text-sm text-gray-500"><fmt:formatDate value="${conf.startDate}" pattern="dd/MM/yy"/>-<fmt:formatDate value="${conf.endDate}" pattern="dd/MM/yy"/></div>
                                                </div>
                                            </div>
                                        </td>
                                        <%-- Colonne Rôles --%>
                                        <td class="px-6 py-4">
                                            <div class="flex flex-wrap">
                                                <c:forEach var="role" items="${roles}">
                                                    <%-- Correction: Utilisation de fn:toLowerCase et fn:replace pour générer la classe CSS du badge --%>
                                                    <span class="role-badge badge-${fn:toLowerCase(fn:replace(role, '_', '-'))}">
                                                        <%-- Affichage lisible du rôle --%>
                                                        <c:out value="${fn:replace(role, '_', ' ')}"/>
                                                    </span>
                                                </c:forEach>
                                            </div>
                                        </td>
                                        <%-- Colonne Dates Clés --%>
                                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 hidden md:table-cell">
                                            <div>Sub: <fmt:formatDate value="${conf.submissionDeadline}" pattern="dd/MM HH:mm"/></div>
                                            <div>Notif: <fmt:formatDate value="${conf.notificationDate}" pattern="dd/MM/yy"/></div>
                                        </td>
                                        <%-- Colonne Accès / Interfaces (avec liens spécifiques par rôle) --%>
                                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                             <c:forEach var="roleStr" items="${roles}">
                                                <div class="mb-1">
                                                    <c:choose>
                                                        <c:when test="${roleStr == 'CHAIR'}">
                                                            <a href="${pageContext.request.contextPath}/conference/manage?confId=${conf.conferenceId}"
                                                               class="text-purple-600 hover:text-purple-900 hover:underline inline-flex items-center">
                                                                <i class="fas fa-cog w-4 h-4 mr-1"></i> Gérer (Président)
                                                            </a>
                                                        </c:when>
                                                        <c:when test="${roleStr == 'STEERING_COMMITTEE'}">
                                                            <%-- *** LIEN CORRIGÉ ICI *** --%>
                                                            <a href="${pageContext.request.contextPath}/conference/steering?confId=${conf.conferenceId}"
                                                               class="text-indigo-600 hover:text-indigo-900 hover:underline inline-flex items-center">
                                                                 <i class="fas fa-users-cog w-4 h-4 mr-1"></i> Accès Pilotage
                                                             </a>
                                                             </c:when>
  <c:when test="${roleStr == 'SC_MEMBER'}">
     <a href="${pageContext.request.contextPath}/sc/dashboard?confId=${conf.conferenceId}" <%-- Vérifiez que c'est bien cette URL --%>
        class="text-blue-600 hover:text-blue-900 hover:underline inline-flex items-center"
        title="Accéder à l'espace Comité Scientifique (Assignations & Décisions)">
         <i class="fas fa-microscope w-4 h-4 mr-1"></i> Espace SC
     </a>
 </c:when>
                                                       <%-- Dans dashboard.jsp, dans la boucle sur les rôles --%>
<c:when test="${roleStr == 'PC_MEMBER'}">
    <%-- Lien pour PC -> pointe maintenant vers /review/assigned --%>
    <a href="${pageContext.request.contextPath}/review/assigned?confId=${conf.conferenceId}" <%-- URL MISE A JOUR --%>
       class="text-green-600 hover:text-green-900 hover:underline inline-flex items-center" <%-- Couleur verte (suggestion) --%>
       title="Voir les articles à évaluer">
        <i class="fas fa-tasks w-4 h-4 mr-1"></i> Évaluer Articles
    </a>
</c:when>
                                                         <%-- Dans dashboard.jsp, dans la boucle c:forEach sur les rôles --%>

<c:when test="${roleStr == 'AUTHOR'}">
    <%-- Lien pour AUTEUR -> pointe vers /submission/list --%>
    <a href="${pageContext.request.contextPath}/submission/list?confId=${conf.conferenceId}" <%-- URL MISE A JOUR --%>
       class="text-cyan-600 hover:text-cyan-900 hover:underline inline-flex items-center" <%-- Couleur Cyan (suggestion) --%>
       title="Voir mes soumissions pour cette conférence">
        <i class="fas fa-file-alt w-4 h-4 mr-1"></i> Mes Soumissions
    </a>
</c:when>
                                                         <c:otherwise>
                                                              <span class="text-gray-400 text-xs">Rôle: <c:out value="${roleStr}"/></span>
                                                         </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </c:forEach>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- Section Actions Rapides --%>
         <div class="mt-10">
             <h3 class="text-lg font-medium text-gray-900 mb-3">Actions rapides</h3>
             <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <a href="${pageContext.request.contextPath}/conference/create" class="bg-white p-4 rounded-lg shadow hover:shadow-md transition flex items-center text-purple-700"><i class="fas fa-plus-circle fa-2x mr-3"></i><span>Créer une conférence</span></a>
                  <%-- TODO: Mettre à jour le lien de soumission --%>
                  <a href="${pageContext.request.contextPath}/submission/submit" class="bg-white p-4 rounded-lg shadow hover:shadow-md transition flex items-center text-purple-700"><i class="fas fa-paper-plane fa-2x mr-3"></i><span>Soumettre un article</span></a>
                   <%-- TODO: Créer la page profil --%>
                   <a href="#" class="bg-white p-4 rounded-lg shadow hover:shadow-md transition flex items-center text-purple-700"><i class="fas fa-user-edit fa-2x mr-3"></i><span>Modifier mon profil</span></a>
             </div>
         </div>
    </div>

    <%-- Footer --%>
    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>

</body>
</html>