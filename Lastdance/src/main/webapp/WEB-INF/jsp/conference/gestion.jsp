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
    <style> body { font-family: 'Inter', sans-serif; } </style>
</head>
<body class="bg-gray-100">

    <%-- Barre de Navigation --%>
    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8">

        <c:choose>
            <c:when test="${empty conference}">
                 <div class="bg-white shadow rounded-lg p-6 text-center">
                    <h2 class="text-xl font-semibold text-red-600">Erreur</h2>
                    <p class="text-gray-600 mt-2">Impossible de charger les informations de la conférence (ID: <c:out value="${param.confId}"/>).</p>
                    <a href="${pageContext.request.contextPath}/dashboard" class="mt-4 inline-block text-purple-600 hover:underline">Retour au tableau de bord</a>
                </div>
            </c:when>
            <c:otherwise>
                <header class="mb-8">
                    <h1 class="text-3xl font-bold leading-tight text-gray-900">Espace Comité de Pilotage</h1>
                    <p class="mt-2 text-lg text-gray-600">Conférence : <c:out value="${conference.name}"/> (<c:out value="${conference.acronym}"/>)</p>
                     <a href="${pageContext.request.contextPath}/dashboard" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-arrow-left mr-1"></i> Retour au dashboard</a>
                </header>

                <div class="bg-white shadow overflow-hidden sm:rounded-lg p-6">
                    <h2 class="text-xl font-semibold text-gray-800 mb-4">Actions du Comité de Pilotage</h2>
                    <p class="text-gray-600 mb-6">Interface spécifique au comité de pilotage.</p>
                    <div class="border-t border-gray-200 pt-6">
                        <h3 class="text-lg font-medium text-gray-700 mb-3">Fonctionnalités Prévues :</h3>
                        <ul class="list-disc list-inside text-gray-500 space-y-2">
                            <li>Nommer / Gérer les membres des comités SC et PC (si ce rôle est différent de celui du Chair).</li>
                            <li>Gérer les utilisateurs globaux de la conférence.</li>
                            <li>Visualiser des statistiques avancées.</li>
                        </ul>
                    </div>
                     <div class="mt-8 p-4 bg-blue-50 border border-blue-200 rounded-md"><p class="text-sm text-blue-700"><i class="fas fa-info-circle mr-2"></i>Section en développement.</p></div>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- Footer --%>
    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>

</body>
</html>