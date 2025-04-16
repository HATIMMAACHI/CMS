<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité --%>
<c:if test="${empty sessionScope.user}"> <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" /> </c:if>
<%-- TODO: Vérif rôle SC pour cette conf --%>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Décisions SC: <c:out value="${conference.acronym}"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <style>
        body { font-family: 'Inter', sans-serif; }
        .btn { @apply inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition duration-150 ease-in-out; }
        .btn-success { @apply text-white bg-green-600 hover:bg-green-700 focus:ring-green-500; }
        .btn-danger { @apply text-white bg-red-600 hover:bg-red-700 focus:ring-red-500; }
        .error-message { @apply bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6; }
        .success-message { @apply bg-green-100 border-l-4 border-green-500 text-green-700 p-4 mb-6; }
        /* Couleurs pour recommandations */
        .rec-strong-accept { color: #059669; font-weight: 600; } /* emerald-600 */
        .rec-accept { color: #10B981; } /* emerald-500 */
        .rec-weak-accept { color: #34D399; } /* emerald-400 */
        .rec-borderline { color: #F59E0B; } /* amber-500 */
        .rec-weak-reject { color: #F87171; } /* red-400 */
        .rec-reject { color: #EF4444; } /* red-500 */
        .rec-strong-reject { color: #DC2626; font-weight: 600; } /* red-600 */
    </style>
</head>
<body class="bg-gray-100">

    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8">

         <c:choose>
            <c:when test="${empty conference}">
                 <div class="bg-white shadow rounded-lg p-6 text-center">...</c:when> <%-- Erreur Conf non trouvée --%>
            <c:otherwise>
                <header class="mb-8">
                    <h1 class="text-3xl font-bold leading-tight text-gray-900">Dashboard Comité Scientifique</h1>
                    <p class="mt-2 text-lg text-gray-600">Conférence : <c:out value="${conference.name}"/> (<c:out value="${conference.acronym}"/>)</p>
                    <a href="${pageContext.request.contextPath}/dashboard" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-arrow-left mr-1"></i> Retour dashboard</a>
                     | <a href="${pageContext.request.contextPath}/assignment/list?confId=${conference.conferenceId}" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-share-square mr-1"></i> Assigner Articles</a>
                </header>

                 <%-- Affichage Messages Succès/Erreur --%>
                 <c:if test="${not empty sessionScope.successMessage}"> <div class="success-message" role="alert"><p><c:out value="${sessionScope.successMessage}"/></p></div> <c:remove var="successMessage" scope="session" /> </c:if>
                 <c:if test="${not empty scError}"> <div class="error-message" role="alert"><p class="font-bold mb-1">Erreur:</p><div><c:out value="${scError}"/></div></div> </c:if>

                <div class="bg-white shadow overflow-hidden sm:rounded-lg">
                    <div class="px-4 py-5 sm:px-6 border-b border-gray-200">
                         <h3 class="text-lg leading-6 font-medium text-gray-900">Articles et Décisions</h3>
                         <p class="mt-1 max-w-2xl text-sm text-gray-500">Visualisez les évaluations et prenez les décisions finales.</p>
                    </div>

                    <c:choose>
                         <c:when test="${empty submissions}">
                             <p class="px-4 py-10 text-center text-gray-500 italic">Aucun article en attente de décision ou évalué pour cette conférence.</p>
                         </c:when>
                         <c:otherwise>
                             <div class="divide-y divide-gray-200">
                                <c:forEach var="sub" items="${submissions}">
                                     <c:set var="reviews" value="${reviewsMap[sub.submissionId]}" /> <%-- Récupère la liste des reviews pour ce sub --%>
                                     <div class="px-4 py-5 sm:px-6 hover:bg-gray-50">
                                         <div class="sm:flex sm:items-start sm:justify-between">
                                             <%-- Infos Article --%>
                                             <div class="flex-1 min-w-0 mb-4 sm:mb-0 sm:mr-6">
                                                 <h4 class="text-lg font-semibold text-purple-800"><c:out value="${sub.title}"/></h4>
                                                 <p class="text-sm text-gray-500 mt-1">ID: <c:out value="${sub.uniquePaperId}"/></p>
                                                 <p class="text-sm text-gray-600 mt-1">Statut Actuel:
                                                     <span class="font-medium
                                                         ${sub.status == 'ACCEPTED' ? 'text-green-600' : ''}
                                                         ${sub.status == 'REJECTED' ? 'text-red-600' : ''}
                                                         ${sub.status == 'UNDER_REVIEW' ? 'text-blue-600' : ''}">
                                                         <c:out value="${sub.status}"/>
                                                     </span>
                                                 </p>
                                                 <p class="mt-2">
                                                      <a href="${pageContext.request.contextPath}/uploads/papers/${fn:escapeXml(sub.filePath)}" target="_blank" class="text-blue-600 hover:underline text-sm"><i class="fas fa-file-pdf mr-1"></i> Voir PDF</a>
                                                       <%-- TODO: Lien vers détails des reviews --%>
                                                       <a href="#" class="ml-4 text-blue-600 hover:underline text-sm"><i class="fas fa-comments mr-1"></i> Voir Évaluations Détail (TODO)</a>
                                                 </p>
                                             </div>

                                             <%-- Résumé Évaluations & Formulaire Décision --%>
                                             <div class="flex-shrink-0 w-full sm:w-auto">
                                                 <h5 class="text-sm font-medium text-gray-700 mb-2">Résumé Évaluations (${fn:length(reviews)} reçues) :</h5>
                                                 <c:choose>
                                                     <c:when test="${empty reviews}">
                                                         <p class="text-sm text-gray-500 italic">Aucune évaluation reçue.</p>
                                                     </c:when>
                                                     <c:otherwise>
                                                         <div class="flex flex-wrap gap-x-3 gap-y-1 mb-4">
                                                             <c:forEach var="rev" items="${reviews}">
                                                                 <span class="text-xs px-2 py-0.5 rounded-full border ${rev.recommendation.name().contains('ACCEPT') ? 'border-green-300 bg-green-50 text-green-700' : (rev.recommendation.name().contains('REJECT') ? 'border-red-300 bg-red-50 text-red-700' : 'border-yellow-300 bg-yellow-50 text-yellow-700')}">
                                                                     <c:out value="${fn:replace(rev.recommendation.name(), '_', ' ')}"/>
                                                                 </span>
                                                             </c:forEach>
                                                         </div>
                                                     </c:otherwise>
                                                 </c:choose>

                                                 <%-- Formulaire de Décision (seulement si pas déjà décidé) --%>
                                                 <c:if test="${sub.status == 'UNDER_REVIEW'}">
                                                      <form action="${pageContext.request.contextPath}/sc/makeDecision" method="post">
                                                           <input type="hidden" name="confId" value="${conference.conferenceId}">
                                                           <input type="hidden" name="submissionId" value="${sub.submissionId}">
                                                           <div class="flex items-center space-x-3 mt-2">
                                                               <button type="submit" name="decision" value="ACCEPT" class="btn btn-success text-xs px-3 py-1">
                                                                   <i class="fas fa-check mr-1"></i> Accepter
                                                               </button>
                                                               <button type="submit" name="decision" value="REJECT" class="btn btn-danger text-xs px-3 py-1">
                                                                    <i class="fas fa-times mr-1"></i> Rejeter
                                                                </button>
                                                           </div>
                                                       </form>
                                                 </c:if>
                                                 <c:if test="${sub.status != 'UNDER_REVIEW'}">
                                                      <p class="text-sm font-semibold ${sub.status == 'ACCEPTED' ? 'text-green-700' : 'text-red-700'} mt-4">Décision finale: <c:out value="${sub.status}"/></p>
                                                 </c:if>

                                             </div>
                                         </div>
                                     </div>
                                </c:forEach>
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