<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité --%>
<c:if test="${empty sessionScope.user}">
    <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" />
</c:if>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Soumettre un Article - ConferenceMS</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
    <%-- On n'a plus besoin de Select2 CSS ici --%>
    <%-- <link href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" rel="stylesheet" /> --%>
    <style>
        body { font-family: 'Inter', sans-serif; }
        .input-field, .select-field { @apply shadow-sm focus:ring-purple-500 focus:border-purple-500 block w-full sm:text-sm border-gray-300 rounded-md py-2 px-3; }
        .textarea-field { @apply shadow-sm focus:ring-purple-500 focus:border-purple-500 block w-full sm:text-sm border-gray-300 rounded-md; }
        .btn { @apply inline-flex justify-center items-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition duration-150 ease-in-out; }
        .btn-primary { @apply text-white bg-purple-600 hover:bg-purple-700 focus:ring-purple-500; }
        .btn-secondary { @apply text-gray-700 bg-white border-gray-300 hover:bg-gray-50 focus:ring-indigo-500; }
        .form-label { @apply block text-sm font-medium text-gray-700 mb-1; }
        .required-star { @apply text-red-500 ml-1; }
        .error-message { @apply bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-6; }
        .help-text { @apply text-xs text-gray-500 mt-1; }
    </style>
</head>
<body class="bg-gray-100">

    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-4xl mx-auto py-10 px-4 sm:px-6 lg:px-8">
        <div class="bg-white shadow-lg rounded-lg overflow-hidden">
            <div class="px-6 py-5 bg-gray-50 border-b border-gray-200">
                 <h1 class="text-2xl font-semibold text-gray-900">Soumettre un Nouvel Article</h1>
            </div>

            <form action="${pageContext.request.contextPath}/submission/submit" method="post" enctype="multipart/form-data" class="p-6 md:p-8 space-y-6">

                <%-- Affichage Erreurs Soumission --%>
                <c:if test="${not empty submissionError}">
                     <div class="error-message" role="alert">
                        <p class="font-bold mb-1">Erreur de soumission :</p>
                        <div>${submissionError}</div> <%-- Le message peut contenir du HTML (<br>) --%>
                    </div>
                </c:if>

                <%-- Choix Conférence (reste identique) --%>
                <div>
                    <label for="conferenceId" class="form-label">Conférence<span class="required-star">*</span></label>
                    <c:choose>
                        <c:when test="${empty conferences}"><p class="text-sm text-red-600 italic mt-2">Aucune conférence n'accepte de soumission.</p></c:when>
                        <c:otherwise>
                             <select id="conferenceId" name="conferenceId" required class="select-field">
                                 <option value="" disabled ${empty conferenceIdValue ? 'selected' : ''}>-- Sélectionnez --</option>
                                 <c:forEach var="conf" items="${conferences}">
                                     <option value="${conf.conferenceId}" ${conferenceIdValue == conf.conferenceId ? 'selected' : ''}><c:out value="${conf.acronym}"/>: <c:out value="${conf.name}"/> (DL: <fmt:formatDate value="${conf.submissionDeadline}" pattern="dd/MM/yy HH:mm"/>)</option>
                                 </c:forEach>
                             </select>
                        </c:otherwise>
                     </c:choose>
                </div>

                 <%-- Titre (reste identique) --%>
                <div>
                    <label for="title" class="form-label">Titre de l'article<span class="required-star">*</span></label>
                    <input type="text" id="title" name="title" required class="input-field" placeholder="Titre complet" value="${fn:escapeXml(titleValue)}">
                </div>

                 <%-- Résumé (reste identique) --%>
                <div>
                    <label for="abstractText" class="form-label">Résumé (Abstract)<span class="required-star">*</span></label>
                    <textarea id="abstractText" name="abstractText" rows="6" required class="textarea-field" placeholder="Collez ou écrivez votre résumé...">${fn:escapeXml(abstractTextValue)}</textarea>
                </div>

                 <%-- Mots-clés (reste identique) --%>
                <div>
                    <label for="keywords" class="form-label">Mots-clés<span class="required-star">*</span></label>
                    <input type="text" id="keywords" name="keywords" required class="input-field" placeholder="Séparés par des virgules" value="${fn:escapeXml(keywordsValue)}">
                    <p class="help-text">Entrez plusieurs mots-clés pertinents séparés par une virgule.</p>
                </div>

                 <%-- *** SECTION AUTEURS MODIFIÉE *** --%>
                 <div class="border-t border-gray-200 pt-6">
                     <h3 class="text-lg font-medium text-gray-900 mb-3">Auteurs</h3>

                     <%-- Champ pour saisir les emails des co-auteurs --%>
                     <div>
                        <label for="coAuthorEmails" class="form-label">
                            Emails des Co-auteurs (séparés par virgule ou point-virgule)
                        </label>
                        <textarea id="coAuthorEmails" name="coAuthorEmails" rows="3" class="textarea-field"
                                  placeholder="email1@exemple.com, email2@exemple.com; email3@exemple.com...">${fn:escapeXml(coAuthorEmailsValue)}</textarea>
                        <p class="help-text">
                            Entrez les adresses email de tous les autres auteurs. Votre email (<c:out value="${sessionScope.user.email}"/>) est inclus automatiquement. Chaque co-auteur doit avoir un compte sur la plateforme.
                        </p>
                     </div>

                     <%-- Champ caché pour l'auteur courant (simplifie la logique serveur) --%>
                     <%-- <input type="hidden" name="currentUserEmail" value="${sessionScope.user.email}"> --%> <%-- Plus nécessaire si on a l'objet User --%>
                     <%-- <input type="hidden" name="currentUserId" value="${sessionScope.user.userId}"> --%>   <%-- Plus nécessaire si on a l'objet User --%>

                     <%-- Choix de l'Auteur Correspondant --%>
                     <div class="mt-4">
                         <label for="correspondingAuthorEmail" class="form-label">
                             Email de l'Auteur Correspondant <span class="required-star">*</span>
                         </label>
                         <input type="email" id="correspondingAuthorEmail" name="correspondingAuthorEmail" required class="input-field"
                                placeholder="Entrez l'email d'UN des auteurs (vous ou un co-auteur)" value="${fn:escapeXml(correspondingAuthorEmailValue)}">
                         <p class="help-text">Cet auteur recevra les communications et notifications.</p>
                     </div>
                 </div>
                 <%-- *** FIN SECTION AUTEURS MODIFIÉE *** --%>

                 <%-- Upload Fichier PDF (reste identique) --%>
                 <div class="border-t border-gray-200 pt-6">
                     <h3 class="text-lg font-medium text-gray-900 mb-3">Fichier Article</h3>
                    <label for="paperFile" class="form-label">Fichier PDF (max 20MB)<span class="required-star">*</span></label>
                    <input type="file" id="paperFile" name="paperFile" required accept=".pdf"
                           class="block w-full text-sm text-gray-900 border border-gray-300 rounded-lg cursor-pointer bg-gray-50 focus:outline-none file:bg-purple-100 file:text-purple-700 file:border-0 file:py-2 file:px-4 file:mr-4 hover:file:bg-purple-200"/>
                    <p class="help-text">Assurez-vous que le fichier est bien au format PDF.</p>
                 </div>

                <%-- Boutons d'Action (reste identique) --%>
                <div class="flex items-center justify-end pt-6 border-t border-gray-200">
                     <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary mr-4">Annuler</a>
                    <button type="submit" class="btn btn-primary" <c:if test="${empty conferences}">disabled title="Aucune conférence ouverte"</c:if> >
                        <i class="fas fa-paper-plane mr-2"></i> Soumettre l'Article
                    </button>
                </div>

            </form>
        </div>
    </div>

    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>

    <%-- Plus besoin de jQuery ni Select2 pour cette version --%>
    <%-- <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script> --%>
    <%-- <script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script> --%>
    <%-- <script> /* ... ancien script Select2 ... */ </script> --%>

</body>
</html>