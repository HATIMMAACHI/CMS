<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%-- Pour escapeXml --%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Inscription - Gestion Conférences</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;700&display=swap" rel="stylesheet">

     <style>
        /* Styles CSS (similaires à login.jsp, adaptés pour l'inscription) */
         * { box-sizing: border-box; }
        body {
            background: #f3f0f9; display: flex; justify-content: center; align-items: center;
            flex-direction: column; font-family: 'Montserrat', sans-serif; height: 100vh;
            margin: -20px 0 50px; padding: 20px; /* Ajout padding pour petits écrans */
        }
        h1 { font-weight: bold; margin: 0; color: #4B0082; }
        p { font-size: 14px; line-height: 20px; letter-spacing: 0.5px; margin: 15px 0 20px; }
        a { color: #6B21A8; font-size: 14px; text-decoration: none; margin: 15px 0; }
        a:hover { text-decoration: underline; }
        button {
            border-radius: 20px; border: 1px solid #6B21A8; background-color: #6B21A8;
            color: #FFFFFF; font-size: 12px; font-weight: bold; padding: 12px 45px;
            letter-spacing: 1px; text-transform: uppercase; transition: transform 80ms ease-in; cursor: pointer;
             margin-top: 10px; /* Espace avant le bouton */
        }
        button:active { transform: scale(0.95); }
        button:focus { outline: none; }

        form {
            background-color: #FFFFFF; display: flex; align-items: center; justify-content: center;
            flex-direction: column; padding: 30px 40px; text-align: center; border-radius: 10px;
            box-shadow: 0 14px 28px rgba(0,0,0,0.2), 0 10px 10px rgba(0,0,0,0.18);
        }
        input {
            background-color: #eee; border: none; padding: 12px 15px; margin: 8px 0; width: 100%; border-radius: 5px;
        }
         .form-container { width: 100%; max-width: 480px; /* Un peu plus large pour l'inscription */ }

        .error-message { color: #DC2626; background-color: #FEE2E2; border: 1px solid #FCA5A5; padding: 10px; border-radius: 5px; margin-bottom: 15px; font-size: 14px; width:100%; text-align: left; }
        /* Pas besoin de success message ici, car on redirige vers login après succès */

        .link-container { margin-top: 20px; }
        label { font-size: 12px; color: #555; text-align: left; width: 100%; margin-bottom: -5px; margin-top: 5px;} /* Labels optionnels */
        label .required { color: red; }
    </style>
</head>
<body>
 <div class="flex items-center">
        <a href="${pageContext.request.contextPath}/" class="flex items-center text-purple-600 hover:text-purple-800 transition duration-150 ease-in-out">
            <i class="fas fa-university text-2xl mr-2"></i> <%-- Icône Université --%>
            <span class="text-xl font-bold">ConferenceMS</span> <%-- Nom de l'application --%>
        </a>
      </div>

    <div class="form-container">
        <form action="${pageContext.request.contextPath}/register" method="post">
            <h1>Créer un compte</h1>
            <p style="color: #555;">Rejoignez la communauté scientifique</p>

            <%-- Affichage des messages d'erreur venant du Servlet --%>
            <c:if test="${not empty errorMessage}">
                <div class="error-message">
                     <i class="fas fa-exclamation-triangle"></i> ${fn:escapeXml(errorMessage)}
                </div>
            </c:if>

            <%-- Champ Prénom --%>
            <label for="firstName">Prénom <span class="required">*</span></label>
            <input type="text" id="firstName" name="firstName" placeholder="Votre prénom" required
                   value="${fn:escapeXml(firstNameValue)}" />

             <%-- Champ Nom --%>
            <label for="lastName">Nom <span class="required">*</span></label>
            <input type="text" id="lastName" name="lastName" placeholder="Votre nom" required
                   value="${fn:escapeXml(lastNameValue)}" />

             <%-- Champ Email --%>
            <label for="email">Email <span class="required">*</span></label>
            <input type="email" id="email" name="email" placeholder="adresse@exemple.com" required
                   value="${fn:escapeXml(emailValue)}" />

             <%-- Champ Affiliation --%>
            <label for="affiliation">Affiliation (Université, Institut...)</label>
            <input type="text" id="affiliation" name="affiliation" placeholder="Votre affiliation (optionnel)"
                   value="${fn:escapeXml(affiliationValue)}" />

             <%-- Champ Mot de passe --%>
            <label for="password">Mot de passe <span class="required">*</span></label>
            <input type="password" id="password" name="password" placeholder="Choisissez un mot de passe" required />
             <%-- Ajouter ici des contraintes JS si besoin (longueur min, etc.) --%>

             <%-- Champ Confirmation Mot de passe --%>
            <label for="confirmPassword">Confirmer le mot de passe <span class="required">*</span></label>
            <input type="password" id="confirmPassword" name="confirmPassword" placeholder="Retapez votre mot de passe" required />

            <button type="submit">S'inscrire</button>

             <div class="link-container">
                <p>Déjà un compte ? <a href="${pageContext.request.contextPath}/login">Connectez-vous ici</a></p>
            </div>
        </form>
    </div>

</body>
</html>