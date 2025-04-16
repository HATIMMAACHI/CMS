<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité --%>
<c:if test="${empty sessionScope.user}"> <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" /> </c:if>
<%-- Vérif rôle CHAIR (fait dans servlet) --%>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestion Conférence: <c:out value="${conference.acronym}"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <style>
        body { font-family: 'Inter', sans-serif; }
        /* ... (garder les styles utilitaires .input-field, .btn, .card, etc.) ... */
        .input-field { @apply shadow-sm focus:ring-purple-500 focus:border-purple-500 block w-full sm:text-sm border-gray-300 rounded-md py-2 px-3; }
        .btn { @apply inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition duration-150 ease-in-out; }
        .btn-primary { @apply text-white bg-purple-600 hover:bg-purple-700 focus:ring-purple-500; }
        .btn-danger-sm { @apply text-red-500 hover:text-red-700 p-1 rounded-full hover:bg-red-100 transition; }
        .card { @apply bg-white shadow-lg rounded-lg overflow-hidden; }
        .card-header { @apply px-6 py-4 bg-gray-50 border-b border-gray-200; }
        .card-title { @apply text-lg font-semibold text-gray-800; }
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
                 <div class="card p-6 text-center">... Erreur Conf non trouvée ...</div>
            </c:when>
            <c:otherwise>
                <header class="mb-8">
                    <h1 class="text-3xl font-bold leading-tight text-gray-900">Gestion de la Conférence</h1>
                    <p class="mt-2 text-lg text-gray-600"><c:out value="${conference.name}"/> (<c:out value="${conference.acronym}"/>)</p>
                    <a href="${pageContext.request.contextPath}/dashboard" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-arrow-left mr-1"></i> Retour dashboard</a>
                </header>

                <%-- Messages Succès/Erreur --%>
                <c:if test="${not empty managementSuccess}"> <div class="success-message-general" role="alert">... Succès ...</div> <c:remove var="managementSuccess" scope="request" /> </c:if>
                <c:if test="${not empty managementError}"> <div class="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6" role="alert">... Erreur ...</div> </c:if>

                <%-- Infos Générales --%>
                 <div class="card mb-8">
                    <%-- ... (Affichage détails conf) ... --%>
                 </div>

                 <%-- Deadlines --%>
                  <div class="card mb-8">
                    <%-- ... (Affichage deadlines) ... --%>
                  </div>

                <%-- Grid pour les sections de gestion --%>
                <%-- On retire la grille si seule la carte Pilotage reste, ou on l'adapte --%>
                <div> <%-- Simple div ou grid grid-cols-1 --%>

                    <%-- *** Carte Gestion Comité de Pilotage *** --%>
                    <div class="card">
                         <div class="card-header"><h3 class="card-title">Comité de Pilotage</h3></div>
                         <div class="card-body">
                             <%-- Affichage Erreur spécifique Pilotage --%>
                             <c:if test="${not empty managementError_STEERING_COMMITTEE}"><p class="error-message-specific"><i class="fas fa-exclamation-circle mr-1"></i><c:out value="${managementError_STEERING_COMMITTEE}"/></p></c:if>
                             <%-- Formulaire Ajout Pilotage --%>
                             <form action="${pageContext.request.contextPath}/conference/addSteeringMember" method="post" class="mb-6 pb-6 border-b border-gray-200">
                                 <input type="hidden" name="confId" value="${conference.conferenceId}">
                                 <label for="userIdentifier_STEERING_COMMITTEE" class="block text-sm font-medium text-gray-700 mb-1">Ajouter par email</label>
                                 <div class="flex items-center gap-3">
                                     <%-- *** NAME et ID CORRIGÉS ICI *** --%>
                                     <input type="email" name="userIdentifier_STEERING_COMMITTEE" id="userIdentifier_STEERING_COMMITTEE" required class="input-field flex-grow" placeholder="email@exemple.com" value="${fn:escapeXml(userIdentifierValue_STEERING_COMMITTEE)}">
                                     <button type="submit" class="btn btn-primary flex-shrink-0"><i class="fas fa-plus"></i></button>
                                 </div>
                             </form>
                             <%-- Liste Membres Pilotage --%>
                             <h4 class="text-md font-semibold text-gray-800 mb-3">Membres Actuels</h4>
                             <c:choose>
                                 <c:when test="${empty steeringCommitteeMembers}"><p class="italic text-sm text-gray-500">Aucun membre.</p></c:when>
                                 <c:otherwise>
                                     <ul role="list" class="divide-y divide-gray-200">
                                         <c:forEach var="member" items="${steeringCommitteeMembers}">
                                             <li class="list-item">
                                                 <%-- ... Affichage membre ... --%>
                                                 <div class="list-item-content"><p class="text-sm font-medium text-gray-900 truncate"><c:out value="${member.firstName}"/> <c:out value="${member.lastName}"/></p><p class="text-sm text-gray-500 truncate"><c:out value="${member.email}"/></p></div>
                                                 <div class="list-item-actions">
                                                      <%-- Formulaire Suppression Pilotage --%>
                                                      <form action="${pageContext.request.contextPath}/conference/removeSteeringMember" method="post" class="inline">
                                                          <input type="hidden" name="confId" value="${conference.conferenceId}"><input type="hidden" name="memberIdToRemove" value="${member.userId}">
                                                          <button type="submit" title="Retirer" class="btn-danger-sm" onclick="return confirm('Retirer ${fn:escapeXml(member.firstName)} ${fn:escapeXml(member.lastName)} ?');"><i class="fas fa-times"></i></button>
                                                      </form>
                                                 </div>
                                             </li>
                                         </c:forEach>
                                     </ul>
                                 </c:otherwise>
                             </c:choose>
                         </div>
                    </div>

                    <%-- *** CARTES POUR SC et PC SUPPRIMÉES DE CETTE PAGE *** --%>

                </div> <%-- Fin div/grid --%>

            </c:otherwise>
        </c:choose>
    </div>

    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>

</body>
</html>