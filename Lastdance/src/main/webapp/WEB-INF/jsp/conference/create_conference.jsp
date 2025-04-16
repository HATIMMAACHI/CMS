<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%-- Pour escapeXml --%>

<%-- Sécurité --%>
<c:if test="${empty sessionScope.user}">
    <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" />
</c:if>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Créer une Conférence - ConferenceMS</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <style>
        body { font-family: 'Inter', sans-serif; }
        /* Style pour rendre les champs de date/heure plus jolis si le navigateur ne le fait pas */
        input[type="date"], input[type="datetime-local"], input[type="text"], select, textarea {
             appearance: none; -webkit-appearance: none; -moz-appearance: none;
             @apply shadow-sm focus:ring-purple-500 focus:border-purple-500 block w-full sm:text-sm border-gray-300 rounded-md py-2 px-3;
        }
         .form-label { @apply block text-sm font-medium text-gray-700 mb-1; }
         .required-star { @apply text-red-500 ml-1; }
         .error-message { @apply bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6; }
         .btn { @apply inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition duration-150 ease-in-out; }
         .btn-primary { @apply text-white bg-purple-600 hover:bg-purple-700 focus:ring-purple-500; }
         .btn-secondary { @apply text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 focus:ring-indigo-500; }

    </style>
</head>
<body class="bg-gray-100">

    <!-- Navigation -->
    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %> <%-- Inclure la nav bar --%>

    <!-- Contenu Principal -->
    <div class="max-w-4xl mx-auto py-10 px-4 sm:px-6 lg:px-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-6">Créer une Nouvelle Conférence</h1>

        <!-- Affichage des Erreurs -->
        <c:if test="${not empty formError}">
             <div class="error-message" role="alert">
                <p class="font-bold">Erreur de validation</p>
                <%-- Attention: formError peut contenir du HTML (<br>), ne pas l'échapper ici --%>
                <p>${formError}</p>
            </div>
        </c:if>

        <%-- Formulaire de Création --%>
        <form action="${pageContext.request.contextPath}/conference/create" method="post" enctype="multipart/form-data" class="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4">

            <%-- Section Informations Générales --%>
            <fieldset class="mb-6 border p-4 rounded">
                <legend class="text-lg font-semibold text-gray-700 px-2 mb-4">Informations Générales</legend>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <%-- Nom --%>
                    <div class="mb-4 md:mb-0"> <%-- Enlever mb-4 ici si gap-6 suffit --%>
                        <label class="form-label" for="name">Nom complet<span class="required-star">*</span></label>
                        <input id="name" name="name" type="text" placeholder="Ex: International Conference on Web Technologies" required value="${fn:escapeXml(nameValue)}">
                    </div>

                    <%-- Acronyme --%>
                    <div class="mb-4 md:mb-0">
                        <label class="form-label" for="acronym">Acronyme<span class="required-star">*</span></label>
                        <input id="acronym" name="acronym" type="text" placeholder="Ex: ICWT 2024" required value="${fn:escapeXml(acronymValue)}">
                    </div>
                </div>

                 <%-- Site Web --%>
                <div class="mt-4 mb-4"> <%-- Ajouter mt-4 si gap ne suffit pas --%>
                    <label class="form-label" for="website">Site Web (optionnel)</label>
                    <input id="website" name="website" type="url" placeholder="https://..." value="${fn:escapeXml(websiteValue)}">
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                     <%-- Type --%>
                    <div class="mb-4 md:mb-0">
                        <label class="form-label" for="type">Type<span class="required-star">*</span></label>
                        <select id="type" name="type" required>
                            <option value="" disabled ${empty typeValue ? 'selected' : ''}>-- Choisir --</option>
                            <option value="PRESENTIAL" ${typeValue == 'PRESENTIAL' ? 'selected' : ''}>Présentiel</option>
                            <option value="VIRTUAL" ${typeValue == 'VIRTUAL' ? 'selected' : ''}>Virtuel</option>
                            <option value="HYBRID" ${typeValue == 'HYBRID' ? 'selected' : ''}>Hybride</option>
                        </select>
                    </div>

                    <%-- Lieu --%>
                    <div class="mb-4 md:mb-0">
                        <label class="form-label" for="location">Lieu (si Présentiel/Hybride)</label>
                        <input id="location" name="location" type="text" placeholder="Ville, Pays" value="${fn:escapeXml(locationValue)}">
                    </div>
                </div>

                 <%-- Dates Conférence --%>
                 <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-4 mb-4">
                      <div>
                           <label class="form-label" for="startDate">Date de début<span class="required-star">*</span></label>
                            <input id="startDate" name="startDate" type="date" required value="${fn:escapeXml(startDateValue)}">
                      </div>
                       <div>
                           <label class="form-label" for="endDate">Date de fin<span class="required-star">*</span></label>
                            <input id="endDate" name="endDate" type="date" required value="${fn:escapeXml(endDateValue)}">
                       </div>
                 </div>

                 <%-- Description --%>
                 <div class="mb-4">
                    <label class="form-label" for="description">Description / Thèmes</label>
                    <textarea id="description" name="description" rows="4" class="textarea-field" placeholder="Décrivez brièvement la conférence...">${fn:escapeXml(descriptionValue)}</textarea>
                 </div>

                 <%-- Upload Logo --%>
                  <div class="mb-4">
                    <label class="form-label" for="logo">Logo (optionnel, JPG/PNG/GIF, max 10MB)</label>
                    <input class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-purple-50 file:text-purple-700 hover:file:bg-purple-100" id="logo" name="logo" type="file" accept=".jpg,.jpeg,.png,.gif">
                 </div>

            </fieldset>

             <%-- Section Dates Importantes --%>
            <fieldset class="mb-6 border p-4 rounded">
                <legend class="text-lg font-semibold text-gray-700 px-2 mb-4">Dates Importantes</legend>
                 <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                     <div class="mb-4 md:mb-0">
                         <label class="form-label" for="submissionDeadline">Deadline Soumission<span class="required-star">*</span></label>
                        <input id="submissionDeadline" name="submissionDeadline" type="datetime-local" required value="${fn:escapeXml(submissionDeadlineValue)}">
                     </div>
                      <div class="mb-4 md:mb-0">
                         <label class="form-label" for="reviewDeadline">Deadline Évaluation<span class="required-star">*</span></label> <%-- Rendu obligatoire? --%>
                        <input id="reviewDeadline" name="reviewDeadline" type="datetime-local" required value="${fn:escapeXml(reviewDeadlineValue)}">
                     </div>
                      <div class="mb-4 md:mb-0">
                         <label class="form-label" for="notificationDate">Date Notification<span class="required-star">*</span></label>
                        <input id="notificationDate" name="notificationDate" type="date" required value="${fn:escapeXml(notificationDateValue)}">
                     </div>
                      <div class="mb-4 md:mb-0">
                         <label class="form-label" for="cameraReadyDeadline">Deadline Version Finale<span class="required-star">*</span></label>
                        <input id="cameraReadyDeadline" name="cameraReadyDeadline" type="datetime-local" required value="${fn:escapeXml(cameraReadyDeadlineValue)}">
                     </div>
                 </div>
            </fieldset>

            <%-- Boutons d'action --%>
            <div class="flex items-center justify-end pt-4">
                 <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary mr-4">
                     Annuler
                 </a>
                <button class="btn btn-primary" type="submit">
                    <i class="fas fa-save mr-2"></i> Créer la Conférence
                </button>
            </div>

        </form>
    </div> <%-- Fin max-w --%>

    <!-- Footer -->
    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %> <%-- Inclure le footer --%>

</body>
</html>