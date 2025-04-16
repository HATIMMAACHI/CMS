<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%-- Pour escapeXml --%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Connexion - Gestion Conférences</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"> <%-- Important pour le responsive --%>
    <!-- Font Awesome (Optionnel mais utilisé dans le style original) -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;700&display=swap" rel="stylesheet">

    <style>
        /* Styles CSS (simplifiés pour la page de login seule) */
        * { box-sizing: border-box; }
        body {
            background: #f3f0f9; /* Light purple background */
            display: flex;
            justify-content: center;
            align-items: center;
            flex-direction: column;
            font-family: 'Montserrat', sans-serif;
            height: 100vh;
            margin: -20px 0 50px; /* Ajustement marge */
        }
        h1 { font-weight: bold; margin: 0; color: #4B0082; /* Indigo */}
        p { font-size: 14px; line-height: 20px; letter-spacing: 0.5px; margin: 15px 0 20px; }
        a { color: #6B21A8; /* Darker Purple */ font-size: 14px; text-decoration: none; margin: 15px 0; }
        a:hover { text-decoration: underline; }
        button {
            border-radius: 20px; border: 1px solid #6B21A8; background-color: #6B21A8;
            color: #FFFFFF; font-size: 12px; font-weight: bold; padding: 12px 45px;
            letter-spacing: 1px; text-transform: uppercase; transition: transform 80ms ease-in; cursor: pointer;
        }
        button:active { transform: scale(0.95); }
        button:focus { outline: none; }

        form {
            background-color: #FFFFFF; display: flex; align-items: center; justify-content: center;
            flex-direction: column; padding: 40px 50px; text-align: center; border-radius: 10px;
             box-shadow: 0 14px 28px rgba(0,0,0,0.2), 0 10px 10px rgba(0,0,0,0.18); /* Ombre adoucie */
        }
        input {
            background-color: #eee; border: none; padding: 12px 15px; margin: 8px 0; width: 100%; border-radius: 5px;
        }
        .form-container { width: 100%; max-width: 420px; /* Largeur max pour le formulaire */}

        .error-message { color: #DC2626; background-color: #FEE2E2; border: 1px solid #FCA5A5; padding: 10px; border-radius: 5px; margin-bottom: 15px; font-size: 14px; width:100%; text-align: left; }
        .success-message { color: #047857; background-color: #D1FAE5; border: 1px solid #6EE7B7; padding: 10px; border-radius: 5px; margin-bottom: 15px; font-size: 14px; width:100%; text-align: left; }

        .link-container { margin-top: 20px; }
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
        <form action="${pageContext.request.contextPath}/login" method="post">
            <h1>Connexion</h1>
             <p style="color: #555;">Accédez à votre espace conférence</p>

            <%-- Affichage des messages d'erreur venant du Servlet --%>
            <c:if test="${not empty errorMessage}">
                <div class="error-message">
                    <i class="fas fa-exclamation-triangle"></i> ${fn:escapeXml(errorMessage)}
                </div>
            </c:if>

            <%-- Affichage des messages de succès (ex: après inscription ou déconnexion) --%>
            <c:if test="${not empty successMessage}">
                <div class="success-message">
                     <i class="fas fa-check-circle"></i> ${fn:escapeXml(successMessage)}
                </div>
                <%-- Important: Vider le message de la session pour qu'il ne s'affiche qu'une fois --%>
                <c:remove var="successMessage" scope="session" />
            </c:if>
            <c:if test="${param.logout == 'true'}">
                 <div class="success-message">
                      <i class="fas fa-check-circle"></i> Vous avez été déconnecté avec succès.
                 </div>
            </c:if>

            <input type="email" name="email" placeholder="Email" required
                   value="${fn:escapeXml(emailValue)}" <%-- Pré-remplissage si erreur --%>
            />
            <input type="password" name="password" placeholder="Mot de passe" required />

            <%-- Lien mot de passe oublié (non fonctionnel pour l'instant) --%>
            <%-- <a href="#">Mot de passe oublié ?</a> --%>

            <button type="submit">Se connecter</button>

            <div class="link-container">
                <p>Pas encore de compte ? <a href="${pageContext.request.contextPath}/register">Inscrivez-vous ici</a></p>
            </div>
        </form>
    </div>

</body>
</html>