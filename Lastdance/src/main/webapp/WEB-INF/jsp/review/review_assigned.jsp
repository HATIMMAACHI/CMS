<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité --%>
<c:if test="${empty sessionScope.user}"> <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" /> </c:if>
<%-- TODO: Vérif rôle PC pour cette conf --%>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Articles à Évaluer - <c:out value="${conference.acronym}"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <style> body { font-family: 'Inter', sans-serif; } </style>
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
                    <h1 class="text-3xl font-bold leading-tight text-gray-900">Mes Évaluations à Faire</h1>
                    <p class="mt-2 text-lg text-gray-600">Conférence : <c:out value="${conference.name}"/> (<c:out value="${conference.acronym}"/>)</p>
                    <a href="${pageContext.request.contextPath}/dashboard" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-arrow-left mr-1"></i> Retour au dashboard</a>
                </header>

                <%-- Affichage Message Succès (après soumission évaluation) --%>
                <c:if test="${not empty sessionScope.successMessage}">
                    <div class="bg-green-100 border-l-4 border-green-500 text-green-700 p-4 mb-6" role="alert">
                        <p><c:out value="${sessionScope.successMessage}"/></p>
                    </div>
                    <c:remove var="successMessage" scope="session" />
                </c:if>

                <div class="bg-white shadow overflow-hidden sm:rounded-lg">
                    <div class="px-4 py-5 sm:px-6 border-b border-gray-200">
                         <h3 class="text-lg leading-6 font-medium text-gray-900">Articles Assignés</h3>
                    </div>

                    <c:choose>
                         <c:when test="${empty assignments}">
                             <p class="px-4 py-10 text-center text-gray-500 italic">Aucun article ne vous est actuellement assigné pour évaluation dans cette conférence.</p>
                         </c:when>
                         <c:otherwise>
                             <ul role="list" class="divide-y divide-gray-200">
                                <c:forEach var="assign" items="${assignments}">
                                    <c:set var="sub" value="${submissionDetails[assign.submissionId]}" /> <%-- Récupère les détails de la soumission via la Map --%>
                                     <li class="p-4 hover:bg-gray-50 sm:p-6">
                                         <div class="md:flex md:items-center md:justify-between">
                                             <%-- Infos Article --%>
                                             <div class="flex-1 min-w-0 mb-4 md:mb-0">
                                                  <h4 class="text-lg font-semibold text-purple-800 truncate" title="${fn:escapeXml(sub.title)}"><c:out value="${sub.title}"/></h4>
                                                  <p class="text-sm text-gray-500 mt-1">ID: <c:out value="${sub.uniquePaperId}"/></p>
                                                  <%-- TODO: Afficher les mots-clés si besoin --%>
                                                  <p class="mt-2">
                                                     <a href="${pageContext.request.contextPath}/uploads/papers/${fn:escapeXml(sub.filePath)}" target="_blank"
                                                        class="text-blue-600 hover:underline inline-flex items-center text-sm">
                                                         <i class="fas fa-file-pdf mr-1"></i> Télécharger l'article
                                                     </a>
                                                  </p>
                                             </div>
                                             <%-- Statut & Actions --%>
                                             <div class="md:ml-6 flex-shrink-0 flex flex-col items-end space-y-2">
                                                  <%-- Affichage Statut Assignation --%>
                                                  <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full
                                                      ${assign.status == 'PENDING' ? 'bg-yellow-100 text-yellow-800' : ''}
                                                      ${assign.status == 'ACCEPTED' ? 'bg-blue-100 text-blue-800' : ''} <%-- Si on ajoute ce statut --%>
                                                      ${assign.status == 'COMPLETED' ? 'bg-green-100 text-green-800' : ''}
                                                      ${assign.status == 'DECLINED' ? 'bg-red-100 text-red-800' : ''}">
                                                      <c:out value="${assign.status}"/>
                                                  </span>

                                                 <%-- Bouton pour évaluer/voir l'évaluation --%>
                                                  <a href="${pageContext.request.contextPath}/review/form?assignId=${assign.assignmentId}"
                                                     class="inline-flex items-center px-3 py-1.5 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500">
                                                      <c:choose>
                                                         <c:when test="${hasReview[assign.assignmentId]}"> <%-- Vérifie si une review existe déjà --%>
                                                             <i class="fas fa-edit mr-2"></i> Modifier / Voir Évaluation
                                                         </c:when>
                                                         <c:otherwise>
                                                             <i class="fas fa-pen-alt mr-2"></i> Rédiger Évaluation
                                                         </c:otherwise>
                                                      </c:choose>
                                                  </a>
                                                   <%-- TODO: Ajouter bouton pour décliner l'évaluation ? --%>
                                             </div>
                                         </div>
                                     </li>
                                 </c:forEach>
                             </ul>
                         </c:otherwise>
                    </c:choose>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>
</body>
</html>