<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité: Rediriger si non connecté --%>
<c:if test="${empty sessionScope.user}">
    <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" />
</c:if>
<%-- Idéalement, vérifier ici aussi si l'utilisateur est bien auteur pour cette conf --%>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mes Soumissions - <c:out value="${conference.acronym}"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <style>
        body { font-family: 'Inter', sans-serif; }
        /* Styles pour les badges de statut (identiques à dashboard.jsp si besoin) */
        .status-badge { display: inline-block; padding: 0.2em 0.6em; font-size: 0.75rem; font-weight: 600; line-height: 1; text-align: center; white-space: nowrap; vertical-align: baseline; border-radius: 9999px; /* rounded-full */ }
        .status-submitted { @apply bg-blue-100 text-blue-800; }
        .status-under-review { @apply bg-yellow-100 text-yellow-800; }
        .status-accepted { @apply bg-green-100 text-green-800; }
        .status-rejected { @apply bg-red-100 text-red-800; }
        .status-pending-revision { @apply bg-orange-100 text-orange-800; }
        .btn { @apply inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition duration-150 ease-in-out; }
        .btn-primary { @apply text-white bg-purple-600 hover:bg-purple-700 focus:ring-purple-500; }
    </style>
</head>
<body class="bg-gray-100">

    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8">

        <c:choose>
            <c:when test="${empty conference}">
                 <div class="bg-white shadow rounded-lg p-6 text-center"><h2 class="text-xl font-semibold text-red-600">Erreur</h2><p class="text-gray-600 mt-2">Conférence non trouvée.</p><a href="${pageContext.request.contextPath}/dashboard" class="mt-4 inline-block text-purple-600 hover:underline">Retour</a></div>
            </c:when>
            <c:otherwise>
                <header class="mb-8">
                    <h1 class="text-3xl font-bold leading-tight text-gray-900">Mes Soumissions</h1>
                    <p class="mt-2 text-lg text-gray-600">Conférence : <c:out value="${conference.name}"/> (<c:out value="${conference.acronym}"/>)</p>
                     <a href="${pageContext.request.contextPath}/dashboard" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-arrow-left mr-1"></i> Retour au dashboard</a>
                      <%-- Afficher bouton Nouvelle Soumission seulement si ouvert --%>
                      <c:if test="${submissionOpen}">
                           <a href="${pageContext.request.contextPath}/submission/submit?confId=${confId}" class="text-sm text-green-600 hover:text-green-800 mt-1 inline-block ml-4"><i class="fas fa-plus mr-1"></i> Nouvelle Soumission</a>
                      </c:if>
                       <c:if test="${!submissionOpen}">
                            <span class="text-sm text-red-600 mt-1 inline-block ml-4"><i class="fas fa-lock mr-1"></i> Soumissions fermées</span>
                       </c:if>
                </header>

                 <%-- Affichage Messages Succès/Erreur --%>
                <c:if test="${not empty sessionScope.successMessage}"> <div class="bg-green-100 border-l-4 border-green-500 text-green-700 p-4 mb-6" role="alert"><p><c:out value="${sessionScope.successMessage}"/></p></div> <c:remove var="successMessage" scope="session" /> </c:if>
                 <c:if test="${not empty sessionScope.errorMessage}"> <div class="bg-yellow-100 border-l-4 border-yellow-500 text-yellow-700 p-4 mb-6" role="alert"><i class="fas fa-exclamation-triangle mr-2"></i> <c:out value="${sessionScope.errorMessage}"/></div> <c:remove var="errorMessage" scope="session" /> </c:if>


                <div class="bg-white shadow overflow-hidden sm:rounded-lg">
                    <div class="px-4 py-5 sm:px-6 border-b border-gray-200">
                         <h3 class="text-lg leading-6 font-medium text-gray-900">Liste de vos articles soumis</h3>
                    </div>

                     <c:choose>
                         <c:when test="${empty submissions}">
                              <p class="px-4 py-10 text-center text-gray-500 italic">Vous n'avez soumis aucun article pour cette conférence.</p>
                              <%-- Bouton Soumettre si ouvert et liste vide --%>
                              <c:if test="${submissionOpen}">
                                   <div class="text-center pb-6">
                                         <a href="${pageContext.request.contextPath}/submission/submit?confId=${confId}" class="btn btn-primary">
                                            <i class="fas fa-plus mr-2"></i> Soumettre votre premier article
                                        </a>
                                   </div>
                               </c:if>
                         </c:when>
                         <c:otherwise>
                            <div class="overflow-x-auto">
                                <table class="min-w-full divide-y divide-gray-200">
                                    <thead class="bg-gray-50">
                                        <tr>
                                            <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Titre / ID</th>
                                            <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider hidden sm:table-cell">Date Soumission</th>
                                            <th scope="col" class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Statut</th>
                                            <th scope="col" class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                                        </tr>
                                    </thead>
                                     <tbody class="bg-white divide-y divide-gray-200">
                                         <c:forEach var="sub" items="${submissions}">
                                             <tr>
                                                 <td class="px-6 py-4 whitespace-nowrap">
                                                     <div class="text-sm font-medium text-gray-900 truncate" title="${fn:escapeXml(sub.title)}"><c:out value="${sub.title}"/></div>
                                                     <div class="text-xs text-gray-500">ID: <c:out value="${sub.uniquePaperId}"/></div>
                                                 </td>
                                                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 hidden sm:table-cell">
                                                      <fmt:formatDate value="${sub.submissionDate}" pattern="dd/MM/yyyy HH:mm"/>
                                                  </td>
                                                 <td class="px-6 py-4 whitespace-nowrap text-center">
                                                      <%-- Badge de Statut --%>
                                                      <span class="status-badge status-${fn:toLowerCase(fn:replace(sub.status.name(), '_', '-'))}">
                                                           <c:out value="${fn:replace(sub.status.name(), '_', ' ')}"/>
                                                      </span>
                                                 </td>
                                                 <td class="px-6 py-4 whitespace-nowrap text-center text-sm font-medium space-x-3"> <%-- Utilisation de space-x pour espacer les icônes --%>
                                                      <%-- Lien Voir PDF --%>
                                                      <a href="${pageContext.request.contextPath}/uploads/papers/${fn:escapeXml(sub.filePath)}" target="_blank" title="Voir le PDF soumis"
                                                         class="text-blue-600 hover:text-blue-800 inline-block"><i class="fas fa-file-pdf fa-fw"></i></a> <%-- fa-fw pour largeur fixe --%>

                                                      <%-- Lien Modifier (conditionnel) --%>
                                                      <c:if test="${sub.status == 'SUBMITTED' && submissionOpen}">
                                                          <a href="${pageContext.request.contextPath}/submission/edit?submissionId=${sub.submissionId}"
                                                             class="text-purple-600 hover:text-purple-800 inline-block" title="Modifier la soumission">
                                                              <i class="fas fa-edit fa-fw"></i>
                                                           </a>
                                                      </c:if>

                                                      <%-- Lien Supprimer (conditionnel) --%>
                                                       <c:if test="${sub.status == 'SUBMITTED' && submissionOpen}">
                                                          <form action="${pageContext.request.contextPath}/submission/delete" method="post" class="inline-block" onsubmit="return confirm('Supprimer cet article ? Cette action est irréversible.');">
                                                              <input type="hidden" name="submissionId" value="${sub.submissionId}">
                                                              <input type="hidden" name="confId" value="${confId}"> <%-- Pour redirection retour --%>
                                                              <button type="submit" class="text-red-500 hover:text-red-700 align-middle" title="Supprimer la soumission"> <%-- align-middle peut aider --%>
                                                                  <i class="fas fa-trash-alt fa-fw"></i>
                                                               </button>
                                                          </form>
                                                       </c:if>
                                                        <%-- Placeholder pour voir les reviews si ACCEPTED/REJECTED --%>
                                                        <c:if test="${sub.status == 'ACCEPTED' || sub.status == 'REJECTED'}">
                                                             <a href="#" class="text-gray-500 cursor-not-allowed inline-block" title="Voir les évaluations (Bientôt disponible)"><i class="fas fa-comments fa-fw"></i></a>
                                                        </c:if>
                                                 </td>
                                             </tr>
                                         </c:forEach>
                                     </tbody>
                                </table>
                             </div>
                         </c:otherwise>
                    </c:choose>
                 </div>
            </c:otherwise>
        </c:choose>
    </div>

    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>
</body>
</html>