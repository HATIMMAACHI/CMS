<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Sécurité --%>
<c:if test="${empty sessionScope.user}"> <c:redirect url="${pageContext.request.contextPath}/login?error=unauthorized" /> </c:if>
<c:if test="${empty assignment || empty submission || assignment.pcMemberId != sessionScope.user.userId}">
    <%-- Vérif de base que les données sont là et que l'assignation est pour l'user connecté --%>
    <c:redirect url="${pageContext.request.contextPath}/dashboard?error=invalid_review_access" />
</c:if>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Évaluation Article: <c:out value="${submission.uniquePaperId}"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css"/>
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
        .info-box { @apply bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6; }
    </style>
</head>
<body class="bg-gray-100">

    <%@ include file="/WEB-INF/jsp/includes/navbar.jsp" %>

    <div class="max-w-4xl mx-auto py-10 px-4 sm:px-6 lg:px-8">

        <header class="mb-6">
             <h1 class="text-3xl font-bold leading-tight text-gray-900">Formulaire d'Évaluation</h1>
             <p class="mt-1 text-lg text-gray-600">Conférence: <c:out value="${conference.acronym}"/></p>
             <a href="${pageContext.request.contextPath}/review/assigned?confId=${conference.conferenceId}" class="text-sm text-purple-600 hover:underline mt-1 inline-block"><i class="fas fa-arrow-left mr-1"></i> Retour à la liste</a>
        </header>

         <%-- Affichage Erreurs Soumission Évaluation --%>
        <c:if test="${not empty reviewError}">
             <div class="error-message" role="alert">
                <p class="font-bold mb-1">Erreur lors de l'enregistrement :</p>
                <div>${reviewError}</div> <%-- Peut contenir HTML --%>
            </div>
        </c:if>

        <%-- Boîte d'informations sur l'article --%>
        <div class="info-box">
             <h2 class="text-xl font-semibold text-gray-800 mb-3">Détails de l'Article</h2>
             <dl class="grid grid-cols-1 gap-x-4 gap-y-4 sm:grid-cols-2">
                <div class="sm:col-span-2">
                  <dt class="text-sm font-medium text-gray-500">Titre</dt>
                  <dd class="mt-1 text-md text-gray-900 font-semibold"><c:out value="${submission.title}"/></dd>
                </div>
                 <div class="sm:col-span-2">
                   <dt class="text-sm font-medium text-gray-500">Résumé</dt>
                   <dd class="mt-1 text-sm text-gray-700"><c:out value="${submission.abstractText}"/></dd>
                 </div>
                <div>
                  <dt class="text-sm font-medium text-gray-500">Mots-clés</dt>
                  <dd class="mt-1 text-sm text-gray-900"><c:out value="${submission.keywords}"/></dd>
                </div>
                <div>
                    <dt class="text-sm font-medium text-gray-500">Fichier PDF</dt>
                    <dd class="mt-1 text-sm">
                        <a href="${pageContext.request.contextPath}/uploads/papers/${fn:escapeXml(submission.filePath)}" target="_blank" class="text-blue-600 hover:underline inline-flex items-center">
                           <i class="fas fa-download mr-1"></i> Télécharger l'article
                       </a>
                    </dd>
                </div>
             </dl>
        </div>


        <%-- Formulaire d'Évaluation --%>
        <form action="${pageContext.request.contextPath}/review/submit" method="post" class="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4 space-y-6">
            <%-- Champ caché essentiel pour savoir quelle assignation on évalue --%>
            <input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
             <%-- Champ caché pour éventuellement repasser confId si besoin après POST --%>
             <input type="hidden" name="confId" value="${conference.conferenceId}">

            <h2 class="text-xl font-semibold text-gray-800 border-b pb-2 mb-4">Votre Évaluation</h2>

             <%-- Commentaires pour l'Auteur --%>
            <div>
                <label for="commentsAuthor" class="form-label">Commentaires pour l'Auteur<span class="required-star">*</span></label>
                <textarea id="commentsAuthor" name="commentsAuthor" rows="8" required class="textarea-field" placeholder="Fournissez un retour constructif et détaillé pour les auteurs...">${fn:escapeXml(reviewData.commentsToAuthor)}</textarea> <%-- Pré-remplissage --%>
                 <p class="help-text">Ces commentaires seront transmis aux auteurs (de manière anonyme).</p>
            </div>

             <%-- Commentaires pour le Comité Scientifique --%>
            <div>
                <label for="commentsSC" class="form-label">Commentaires Confidentiels pour le Comité Scientifique (SC)</label>
                <textarea id="commentsSC" name="commentsSC" rows="4" class="textarea-field" placeholder="Ajoutez ici des commentaires ou préoccupations spécifiques pour le SC (ex: plagiat suspecté, conflit d'intérêt potentiel, points non destinés à l'auteur)...">${fn:escapeXml(reviewData.commentsToSc)}</textarea> <%-- Pré-remplissage --%>
                 <p class="help-text">Ces commentaires ne seront PAS vus par les auteurs.</p>
            </div>

             <%-- Recommandation et Confiance (sur la même ligne) --%>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 border-t pt-6">
                 <%-- Recommandation --%>
                 <div>
                     <label for="recommendation" class="form-label">Recommandation Globale<span class="required-star">*</span></label>
                     <select id="recommendation" name="recommendation" required class="select-field">
                         <option value="" disabled ${empty reviewData.recommendation ? 'selected' : ''}>-- Choisissez --</option>
                         <c:forEach var="rec" items="${recommendations}"> <%-- Boucle sur l'enum passé par le servlet --%>
                             <option value="${rec.name()}" ${reviewData.recommendation == rec ? 'selected' : ''}>
                                <c:out value="${fn:replace(rec.name(), '_', ' ')}"/> <%-- Affiche le nom lisiblement --%>
                             </option>
                         </c:forEach>
                     </select>
                 </div>

                  <%-- Confiance --%>
                 <div>
                     <label for="confidence" class="form-label">Votre Niveau de Confiance<span class="required-star">*</span></label>
                     <select id="confidence" name="confidence" required class="select-field">
                          <option value="" disabled ${empty reviewData.confidence ? 'selected' : ''}>-- Choisissez --</option>
                         <c:forEach var="conf" items="${confidences}"> <%-- Boucle sur l'enum passé par le servlet --%>
                             <option value="${conf.name()}" ${reviewData.confidence == conf ? 'selected' : ''}>
                                <c:out value="${fn:replace(conf.name(), '_', ' ')}"/>
                             </option>
                         </c:forEach>
                     </select>
                 </div>
            </div>


            <%-- Boutons d'Action --%>
            <div class="flex items-center justify-end pt-6 border-t border-gray-200">
                 <a href="${pageContext.request.contextPath}/review/assigned?confId=${conference.conferenceId}" class="btn btn-secondary mr-4">
                     Annuler
                 </a>
                <button type="submit" class="btn btn-primary">
                    <c:choose>
                        <c:when test="${isEditMode}"><i class="fas fa-save mr-2"></i> Enregistrer les Modifications</c:when>
                        <c:otherwise><i class="fas fa-paper-plane mr-2"></i> Soumettre l'Évaluation</c:otherwise>
                    </c:choose>
                </button>
            </div>
        </form>

    </div>

    <%@ include file="/WEB-INF/jsp/includes/footer.jsp" %>

</body>
</html>