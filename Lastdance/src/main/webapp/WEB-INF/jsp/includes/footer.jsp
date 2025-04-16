<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%-- Nécessaire pour l'année dynamique --%>

<%-- ======================================================================= --%>
<%-- ================= Composant Réutilisable : Footer =================== --%>
<%-- ======================================================================= --%>

<footer class="bg-gray-800 mt-auto w-full"> <%-- Fond sombre, prend toute la largeur, se pousse en bas si possible --%>
  <div class="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between">
    <%-- Copyright avec année dynamique --%>
    <div class="text-center sm:text-left text-sm text-gray-400">
      © ${java.time.Year.now()} ConferenceMS. Tous droits réservés.
    </div>
    <%-- Liens réseaux sociaux (Optionnel) --%>
    <div class="flex space-x-6 mt-4 sm:mt-0">
      <a href="#" class="text-gray-400 hover:text-white transition duration-150 ease-in-out">
        <span class="sr-only">Twitter</span> <%-- Texte pour lecteurs d'écran --%>
        <i class="fab fa-twitter"></i>
      </a>
      <a href="#" class="text-gray-400 hover:text-white transition duration-150 ease-in-out">
        <span class="sr-only">LinkedIn</span>
        <i class="fab fa-linkedin-in"></i>
      </a>
      <%-- Ajouter d'autres liens (GitHub, Facebook, etc.) si pertinent --%>
    </div>
  </div>
</footer>