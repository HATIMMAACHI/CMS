<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité: Rediriger si non connecté --%>
<c:if test="${empty sessionScope.user}">
    <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" />
</c:if>
<%-- Sécurité: Vérification rôle SC (déjà fait dans servlet, mais bonne pratique de le rappeler) --%>
<%-- Idéalement, on passerait les rôles de l'utilisateur pour cette conf à la JSP
     <c:set var="userRoles" value="${sessionScope.userRolesForConference[conference.conferenceId]}" />
     <c:if test="${not fn:contains(userRoles, 'SC_MEMBER')}">
         <c:redirect url="${pageContext.request.contextPath}/dashboard?error=forbidden" />
     </c:if>
--%>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Assigner Articles : <c:out value="${conference.acronym}"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <%-- Select2 CSS (pour le multi-select des PC members) --%>
    <link href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" rel="stylesheet" />
    <style>
        body { font-family: 'Inter', sans-serif; }
        /* Styles Utilitaires */
        .btn { @apply inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition duration-150 ease-in-out; }
        .btn-primary { @apply text-white bg-purple-600 hover:bg-purple-700 focus:ring-purple-500; }
        .error-message { @apply bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6; }
        .success-message { @apply bg-green-100 border-l-4 border-green-500 text-green-700 p-4 mb-6; }
        .info-message { @apply bg-blue-100 border-l-4 border-blue-500 text-blue-700 p-4 mb-6; }
        /* Ajustements Select2 */
        .select2-container { width: 100% !important; }
        .select2-container .select2-selection--multiple { min-height: 42px; @apply shadow-sm focus:ring-purple-500 focus:border-purple-500 block w-full sm:text-sm border-gray-300 rounded-md py-1 px-1; } /* Style input */
        .select2-container--default .select2-selection--multiple .select2-selection__choice { @apply bg-purple-100 text-purple-800 border-purple-200 mt-1 mr-1 px-2 py-0.5; }
        .select2-container--default .select2-selection--multiple .select2-selection__choice__remove { @apply text-purple-600 hover:text-purple-800; margin-right: 4px; font-weight: bold;}
    </style>
</head>
<body class="bg-gray-100">

    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8">

        <c:choose>
            <c:when test="${empty conference}">
                 <div class="bg-white shadow rounded-lg p-6 text-center"><h2 class="text-xl font-semibold text-red-600">Erreur</h2><p class="text-gray-600 mt-2">Conférence non trouvée (ID: <c:out value="${param.confId}"/>).</p><a href="${pageContext.request.contextPath}/dashboard" class="mt-4 inline-block text-purple-600 hover:underline">Retour</a></div>
            </c:when>
            <c:otherwise>
                <header class="mb-8">
                    <h1 class="text-3xl font-bold leading-tight text-gray-900">Assignation des Articles</h1>
                    <p class="mt-2 text-lg text-gray-600">Conférence : <c:out value="${conference.name}"/> (<c:out value="${conference.acronym}"/>)</p>
                    <a href="${pageContext.request.contextPath}/dashboard" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-arrow-left mr-1"></i> Retour au dashboard</a>
                </header>

                <%-- Affichage Messages Succès/Erreur/Info venant du POST --%>
                 <c:if test="${not empty assignmentSuccess}"> <div class="success-message" role="alert"><p><c:out value="${assignmentSuccess}"/></p></div> </c:if>
                 <c:if test="${not empty assignmentError}"> <div class="error-message" role="alert"><p class="font-bold mb-1">Erreur:</p><div>${assignmentError}</div></div> </c:if> <%-- Erreur peut contenir HTML --%>
                 <c:if test="${not empty assignmentInfo}"> <div class="info-message" role="alert"><p><c:out value="${assignmentInfo}"/></p></div> </c:if>


                <%-- Liste des Articles à Assigner --%>
                <div class="bg-white shadow overflow-hidden sm:rounded-lg">
                    <div class="px-4 py-5 sm:px-6 border-b border-gray-200">
                         <h3 class="text-lg leading-6 font-medium text-gray-900">Articles Soumis (en attente d'assignation)</h3>
                    </div>

                    <c:choose>
                         <c:when test="${empty submissions}">
                             <p class="px-4 py-5 text-center text-gray-500 italic">Aucun article en attente d'assignation pour cette conférence.</p>
                         </c:when>
                         <c:otherwise>
                            <div class="divide-y divide-gray-200">
                                <c:forEach var="sub" items="${submissions}">
                                     <div class="px-4 py-5 sm:px-6">
                                        <div class="md:flex md:items-start md:justify-between">
                                             <%-- Détails Article --%>
                                             <div class="flex-1 min-w-0 mb-4 md:mb-0">
                                                 <h4 class="text-lg font-semibold text-purple-800"><c:out value="${sub.title}"/></h4>
                                                 <p class="text-sm text-gray-500 mt-1">ID: <c:out value="${sub.uniquePaperId}"/></p>
                                                 <%-- TODO: Ajouter les auteurs ici (nécessite de les charger dans le servlet) --%>
                                                 <%-- <p class="text-sm text-gray-600 mt-1">Auteurs: ... </p> --%>
                                                  <p class="text-sm text-gray-600 mt-1">Mots-clés: <c:out value="${sub.keywords}"/></p>
                                                   <p class="text-sm text-gray-600 mt-1">
                                                       <a href="${pageContext.request.contextPath}/uploads/papers/${fn:escapeXml(sub.filePath)}" target="_blank" class="text-blue-600 hover:underline">
                                                           <i class="fas fa-file-pdf mr-1"></i> Télécharger PDF
                                                       </a>
                                                       <span class="ml-4">Assignations actuelles: <span class="font-bold">${assignmentCounts[sub.submissionId]}</span></span>
                                                    </p>
                                             </div>

                                             <%-- Formulaire d'Assignation pour cet Article --%>
                                             <div class="md:ml-6 flex-shrink-0 w-full md:w-1/2 lg:w-1/3">
                                                <form action="${pageContext.request.contextPath}/assignment/assign" method="post">
                                                     <input type="hidden" name="confId" value="${confId}">
                                                     <input type="hidden" name="submissionId" value="${sub.submissionId}">

                                                     <label for="pcMemberIds_${sub.submissionId}" class="block text-sm font-medium text-gray-700 mb-1">Assigner à (PC Members)</label>
                                                     <c:choose>
                                                         <c:when test="${empty pcMembers}">
                                                              <p class="text-sm text-red-600 italic">Aucun membre PC défini pour cette conférence.</p>
                                                         </c:when>
                                                         <c:otherwise>
                                                              <select name="pcMemberIds_${sub.submissionId}" id="pcMemberIds_${sub.submissionId}" multiple="multiple" class="select2-pc-members" style="width: 100%;">
                                                                  <c:forEach var="pc" items="${pcMembers}">
                                                                       <%-- Pré-sélectionner si déjà assigné --%>
                                                                       <c:set var="isAssigned" value="${fn:contains(currentAssignments[sub.submissionId], pc.userId)}" />
                                                                      <option value="${pc.userId}" ${isAssigned ? 'selected' : ''}>
                                                                          <c:out value="${pc.firstName}"/> <c:out value="${pc.lastName}"/> (<c:out value="${pc.email}"/>)
                                                                      </option>
                                                                  </c:forEach>
                                                              </select>
                                                              <div class="mt-3 text-right">
                                                                  <button type="submit" class="btn btn-primary text-sm">
                                                                      <i class="fas fa-check mr-2"></i> Valider Assignations
                                                                  </button>
                                                              </div>
                                                         </c:otherwise>
                                                     </c:choose>
                                                 </form>
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

    <%-- jQuery et Select2 JS --%>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script>
    <script>
        $(document).ready(function() {
            // Initialiser Select2 pour toutes les listes de PC members
            $('.select2-pc-members').select2({
                placeholder: "Sélectionnez un ou plusieurs évaluateurs...",
                // closeOnSelect: false // Garder ouvert pour sélection multiple facile
            });
        });
    </script>

</body>
</html>