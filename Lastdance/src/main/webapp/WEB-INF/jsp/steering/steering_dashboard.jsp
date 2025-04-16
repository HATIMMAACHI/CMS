<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité: Rediriger si non connecté --%>
<c:if test="${empty sessionScope.user}">
    <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" />
</c:if>
<%-- Sécurité: Vérif rôle Steering ou Chair (déjà fait dans servlet) --%>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pilotage: <c:out value="${conference.acronym}"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <style>
        body { font-family: 'Inter', sans-serif; }
        /* Classes CSS Utilitaires (styles de base pour inputs/boutons) */
        .input-field { @apply shadow-sm focus:ring-purple-500 focus:border-purple-500 block w-full sm:text-sm border-gray-300 rounded-md py-2 px-3; }
        .btn { @apply inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition duration-150 ease-in-out; }
        .btn-primary { @apply text-white bg-purple-600 hover:bg-purple-700 focus:ring-purple-500; }
        .btn-danger-sm { @apply text-red-500 hover:text-red-700 p-1 rounded-full hover:bg-red-100; }
        /* Style pour les cartes de section */
        .card { @apply bg-white shadow-lg rounded-lg overflow-hidden; }
        .card-header { @apply px-6 py-4 bg-gray-50 border-b border-gray-200; }
        .card-title { @apply text-lg font-semibold text-gray-800; }
        .card-description { @apply text-sm text-gray-600 mt-1; }
        .card-body { @apply p-6; }
        .list-item { @apply py-3 sm:py-4 flex items-center justify-between; }
        .list-item-content { @apply flex-1 min-w-0; }
        .list-item-actions { @apply flex-shrink-0 ml-4; }
        .error-message-specific { @apply text-sm text-red-600 mb-3; }
        .success-message-general { @apply bg-green-100 border-l-4 border-green-500 text-green-700 p-4 mb-6; }
    </style>
</head>
<body class="bg-gray-100">

    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8">

        <c:choose>
            <c:when test="${empty conference}">
                <%-- Erreur si la conférence n'est pas trouvée --%>
                <div class="card p-6 text-center">
                    <h2 class="text-xl font-semibold text-red-600 mb-2">Erreur</h2>
                    <p class="text-gray-600">Impossible de charger la conférence (ID: <c:out value="${param.confId}"/>).</p>
                    <a href="${pageContext.request.contextPath}/dashboard" class="mt-4 inline-block text-purple-600 hover:underline">Retour</a>
                </div>
            </c:when>
            <c:otherwise>
                <%-- En-tête de la page --%>
                <div class="mb-8 md:flex md:items-center md:justify-between">
                    <div class="flex-1 min-w-0">
                         <h1 class="text-3xl font-bold leading-tight text-gray-900">Espace Comité de Pilotage</h1>
                         <p class="mt-1 text-lg text-gray-500 truncate">Conférence : <c:out value="${conference.name}"/> (<c:out value="${conference.acronym}"/>)</p>
                    </div>
                    <div class="mt-4 flex md:mt-0 md:ml-4">
                        <a href="${pageContext.request.contextPath}/dashboard" class="text-sm text-purple-600 hover:underline inline-flex items-center">
                            <i class="fas fa-arrow-left mr-1"></i> Retour au dashboard
                        </a>
                    </div>
                </div>

                <%-- Affichage Message Succès Général --%>
                <c:if test="${not empty steeringSuccess}">
                    <div class="success-message-general" role="alert">
                        <p><strong class="font-bold">Succès:</strong> <c:out value="${steeringSuccess}"/></p>
                    </div>
                     <%-- Vider le message pour affichage unique --%>
                     <c:remove var="steeringSuccess" scope="request" /> <%-- Ou session selon comment il est mis --%>
                </c:if>


                <%-- Grid pour les comités SC et PC --%>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-8">

                    <%-- *** Carte Gestion Comité Scientifique (SC) *** --%>
                    <div class="card">
                         <div class="card-header">
                             <h3 class="card-title">Comité Scientifique (SC)</h3>
                             <p class="card-description">Gérer les membres responsables de l'évaluation.</p>
                         </div>
                         <div class="card-body">
                             <c:if test="${not empty steeringError_SC_MEMBER}"><p class="error-message-specific"><i class="fas fa-exclamation-circle mr-1"></i><c:out value="${steeringError_SC_MEMBER}"/></p></c:if>
                             <%-- Formulaire Ajout SC --%>
                             <form action="${pageContext.request.contextPath}/conference/steering/addScMember" method="post" class="mb-6 pb-6 border-b border-gray-200">
                                 <input type="hidden" name="confId" value="${conference.conferenceId}">
                                 <label for="userIdentifier_SC_MEMBER_steering" class="committee-form-label mb-1">Ajouter un membre par email</label>
                                 <div class="flex items-center gap-3">
                                     <input type="email" name="userIdentifier_SC_MEMBER" id="userIdentifier_SC_MEMBER_steering" required class="input-field flex-grow" placeholder="email@exemple.com" value="${fn:escapeXml(userIdentifierValue_SC_MEMBER)}">
                                     <button type="submit" class="btn btn-primary flex-shrink-0"><i class="fas fa-plus"></i></button>
                                 </div>
                             </form>
                             <%-- Liste Membres SC --%>
                             <h4 class="committee-list-title">Membres Actuels SC</h4>
                             <c:choose>
                                 <c:when test="${empty scMembers}"><p class="committee-list-empty">Aucun membre SC.</p></c:when>
                                 <c:otherwise>
                                     <ul role="list" class="divide-y divide-gray-200">
                                         <c:forEach var="member" items="${scMembers}">
                                             <li class="list-item">
                                                 <div class="list-item-content">
                                                     <p class="text-sm font-medium text-gray-900 truncate"><c:out value="${member.firstName}"/> <c:out value="${member.lastName}"/></p>
                                                     <p class="text-sm text-gray-500 truncate"><c:out value="${member.email}"/></p>
                                                 </div>
                                                 <div class="list-item-actions">
                                                     <form action="${pageContext.request.contextPath}/conference/steering/removeScMember" method="post" class="inline">
                                                         <input type="hidden" name="confId" value="${conference.conferenceId}"><input type="hidden" name="memberIdToRemove" value="${member.userId}">
                                                         <button type="submit" title="Retirer du SC" class="btn-danger-sm" onclick="return confirm('Retirer ${fn:escapeXml(member.firstName)} ${fn:escapeXml(member.lastName)} du SC ?');"><i class="fas fa-times"></i></button>
                                                     </form>
                                                 </div>
                                             </li>
                                         </c:forEach>
                                     </ul>
                                 </c:otherwise>
                             </c:choose>
                         </div>
                    </div>

                    <%-- *** Carte Gestion Comité de Programme (PC) *** --%>
                    <div class="card">
                         <div class="card-header">
                             <h3 class="card-title">Comité de Programme (PC)</h3>
                             <p class="card-description">Gérer les évaluateurs des articles.</p>
                         </div>
                         <div class="card-body">
                              <c:if test="${not empty steeringError_PC_MEMBER}"><p class="error-message-specific"><i class="fas fa-exclamation-circle mr-1"></i><c:out value="${steeringError_PC_MEMBER}"/></p></c:if>
                              <%-- Formulaire Ajout PC --%>
                             <form action="${pageContext.request.contextPath}/conference/steering/addPcMember" method="post" class="mb-6 pb-6 border-b border-gray-200">
                                  <input type="hidden" name="confId" value="${conference.conferenceId}">
                                   <label for="userIdentifier_PC_MEMBER_steering" class="committee-form-label mb-1">Ajouter un membre par email</label>
                                  <div class="flex items-center gap-3">
                                      <input type="email" name="userIdentifier_PC_MEMBER" id="userIdentifier_PC_MEMBER_steering" required class="input-field flex-grow" placeholder="email@exemple.com" value="${fn:escapeXml(userIdentifierValue_PC_MEMBER)}">
                                      <button type="submit" class="btn btn-primary flex-shrink-0"><i class="fas fa-plus"></i></button>
                                  </div>
                              </form>
                             <%-- Liste Membres PC --%>
                             <h4 class="committee-list-title">Membres Actuels PC</h4>
                              <c:choose>
                                 <c:when test="${empty pcMembers}"><p class="committee-list-empty">Aucun membre PC.</p></c:when>
                                 <c:otherwise>
                                     <ul role="list" class="divide-y divide-gray-200">
                                         <c:forEach var="member" items="${pcMembers}">
                                             <li class="list-item">
                                                  <div class="list-item-content">
                                                      <p class="text-sm font-medium text-gray-900 truncate"><c:out value="${member.firstName}"/> <c:out value="${member.lastName}"/></p>
                                                      <p class="text-sm text-gray-500 truncate"><c:out value="${member.email}"/></p>
                                                  </div>
                                                  <div class="list-item-actions">
                                                      <form action="${pageContext.request.contextPath}/conference/steering/removePcMember" method="post" class="inline">
                                                          <input type="hidden" name="confId" value="${conference.conferenceId}"><input type="hidden" name="memberIdToRemove" value="${member.userId}">
                                                          <button type="submit" title="Retirer du PC" class="btn-danger-sm" onclick="return confirm('Retirer ${fn:escapeXml(member.firstName)} ${fn:escapeXml(member.lastName)} du PC ?');"><i class="fas fa-times"></i></button>
                                                      </form>
                                                  </div>
                                             </li>
                                         </c:forEach>
                                     </ul>
                                 </c:otherwise>
                             </c:choose>
                         </div>
                    </div>

                </div> <%-- Fin Grid Comités --%>

                 <%-- Autres actions Pilotage --%>
                 <div class="mt-8 card">
                     <div class="card-header"><h3 class="card-title">Autres Actions (Pilotage)</h3></div>
                     <div class="card-body">
                         <ul class="list-disc list-inside text-gray-500 space-y-2">
                              <li><a href="#" class="text-purple-600 hover:underline">Gérer utilisateurs conf. (TODO)</a></li>
                              <li><a href="#" class="text-purple-600 hover:underline">Voir stats globales (TODO)</a></li>
                         </ul>
                     </div>
                 </div>

            </c:otherwise> <%-- Fin si conference existe --%>
        </c:choose>

    </div>

    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>

</body>
</html>