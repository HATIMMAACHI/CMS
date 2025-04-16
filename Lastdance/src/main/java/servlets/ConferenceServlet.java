package servlets;

// Imports Java et Jakarta EE
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Imports DAO (Adapter le nom du package si besoin: 'dao' ou 'DAO')
import DAO.ConferenceDAOImpl;
import DAO.UserConferenceRoleDAOImpl;
import DAO.UserDAOImpl;

// Imports Models (Adapter le nom du package si besoin: 'models' ou 'Models')
import models.Conference;
import models.Conference.ConferenceType;
import models.User;
import models.UserConferenceRole.Role;


/**
 * Servlet gérant TOUTES les actions liées aux conférences :
 * Création, Affichage/Gestion (infos + membres comités pour CHAIR),
 * Affichage spécifique Comité Pilotage, Ajout/Suppression membres par Steering/Chair.
 * Accès réservé aux utilisateurs connectés. Droits spécifiques vérifiés.
 */
@WebServlet(name = "ConferenceServlet", urlPatterns = {
        "/conference/create",             // GET (affiche form), POST (traite création)
        "/conference/manage",             // GET (affiche page gestion pour CHAIR)
        "/conference/steering",           // GET (affiche page spécifique pour Steering Committee/Chair)
        // Actions POST depuis /conference/manage (par Chair)
        "/conference/addSteeringMember",
        "/conference/removeSteeringMember",
        "/conference/addScMember",
        "/conference/removeScMember",
        "/conference/addPcMember",
        "/conference/removePcMember",
        // Actions POST depuis /conference/steering (par Steering/Chair)
        "/conference/steering/addScMember",
        "/conference/steering/removeScMember",
        "/conference/steering/addPcMember",
        "/conference/steering/removePcMember"
})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1,  // 1 MB
    maxFileSize = 1024 * 1024 * 10, // 10 MB Logo
    maxRequestSize = 1024 * 1024 * 15 // 15 MB Total
)
public class ConferenceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private ConferenceDAOImpl conferenceDAO;
    private UserConferenceRoleDAOImpl userConferenceRoleDAO;
    private UserDAOImpl userDAO;
    private static final String UPLOAD_DIR_LOGO = "uploads" + File.separator + "logos";
    private String uploadBasePath;

    @Override
    public void init() throws ServletException {
        super.init();
        this.conferenceDAO = new ConferenceDAOImpl();
        this.userConferenceRoleDAO = new UserConferenceRoleDAOImpl();
        this.userDAO = new UserDAOImpl();
        try {
            String contextPath = getServletContext().getRealPath("");
            if (contextPath == null) {
                 System.err.println("ERREUR CRITIQUE: getServletContext().getRealPath(\"\") = null.");
                 throw new ServletException("Chemin d'upload non déterminable.");
            }
            this.uploadBasePath = contextPath + File.separator + UPLOAD_DIR_LOGO;
            File uploadDir = new File(this.uploadBasePath);
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) { throw new ServletException("Impossible de créer upload dir: " + this.uploadBasePath); }
                System.out.println("Upload dir créé: " + this.uploadBasePath);
            } else { System.out.println("Upload dir trouvé: " + this.uploadBasePath); }
        } catch (Exception e) { throw new ServletException("Erreur init upload path", e); }
    }

    // ======================== doGet Router ========================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendRedirect(request.getContextPath() + "/login?error=session_expired"); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();
        System.out.println("ConferenceServlet doGet: Entrée pour " + actionPath);

        try {
            int conferenceId = 0;
            if (!actionPath.equals("/conference/create")) { conferenceId = getConferenceIdFromRequest(request); }

            switch (actionPath) {
                case "/conference/create": System.out.println("... Routage vers showCreateConferenceForm"); showCreateConferenceForm(request, response); break;
                case "/conference/manage": System.out.println("... Routage vers showManageConferencePage"); showManageConferencePage(request, response, currentUser, conferenceId); break;
                case "/conference/steering": System.out.println("... Routage vers showSteeringPage"); showSteeringPage(request, response, currentUser, conferenceId); break;
                default: System.err.println("... Action GET inconnue: " + actionPath); response.sendError(HttpServletResponse.SC_NOT_FOUND, "Action GET inconnue.");
            }
        } catch (InvalidConferenceIdException e) { System.err.println("... InvalidConferenceIdException (GET): " + e.getMessage()); response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); }
        catch (SQLException e) { System.err.println("... SQLException (GET): " + e.getMessage()); handleGenericError(request, response, e, "Erreur DB (GET)"); }
        catch (Exception e) { System.err.println("... Exception (GET): " + e.getMessage()); handleGenericError(request, response, e, "Erreur serveur (GET)"); }
        finally { System.out.println("ConferenceServlet doGet: Sortie pour " + actionPath); }
    }

    // ======================== doPost Router =======================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non autorisé."); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();
        System.out.println("ConferenceServlet doPost: Entrée pour " + actionPath);

        try {
            int conferenceId = 0;
            if (!actionPath.equals("/conference/create")) { conferenceId = getConferenceIdFromRequest(request); }

            switch (actionPath) {
                case "/conference/create": System.out.println("... Routage vers processCreateConference"); processCreateConference(request, response, currentUser); break;
                // --- Actions POST depuis /conference/manage (par Chair) ---
                case "/conference/addSteeringMember": System.out.println("... Routage vers processAddMember (Steering)"); processAddMember(request, response, currentUser, conferenceId, Role.STEERING_COMMITTEE, "Comité de Pilotage"); break;
                case "/conference/removeSteeringMember": System.out.println("... Routage vers processRemoveMember (Steering)"); processRemoveMember(request, response, currentUser, conferenceId, Role.STEERING_COMMITTEE); break;
                case "/conference/addScMember": System.out.println("... Routage vers processAddMember (SC via Manage)"); processAddMember(request, response, currentUser, conferenceId, Role.SC_MEMBER, "Comité Scientifique"); break;
                case "/conference/removeScMember": System.out.println("... Routage vers processRemoveMember (SC via Manage)"); processRemoveMember(request, response, currentUser, conferenceId, Role.SC_MEMBER); break;
                case "/conference/addPcMember": System.out.println("... Routage vers processAddMember (PC via Manage)"); processAddMember(request, response, currentUser, conferenceId, Role.PC_MEMBER, "Comité de Programme"); break;
                case "/conference/removePcMember": System.out.println("... Routage vers processRemoveMember (PC via Manage)"); processRemoveMember(request, response, currentUser, conferenceId, Role.PC_MEMBER); break;
                // --- Actions POST depuis /conference/steering (par Steering/Chair) ---
                case "/conference/steering/addScMember": System.out.println("... Routage vers processAddMember (SC via Steering)"); processAddMember(request, response, currentUser, conferenceId, Role.SC_MEMBER, "Comité Scientifique"); break;
                case "/conference/steering/removeScMember": System.out.println("... Routage vers processRemoveMember (SC via Steering)"); processRemoveMember(request, response, currentUser, conferenceId, Role.SC_MEMBER); break;
                case "/conference/steering/addPcMember": System.out.println("... Routage vers processAddMember (PC via Steering)"); processAddMember(request, response, currentUser, conferenceId, Role.PC_MEMBER, "Comité de Programme"); break;
                case "/conference/steering/removePcMember": System.out.println("... Routage vers processRemoveMember (PC via Steering)"); processRemoveMember(request, response, currentUser, conferenceId, Role.PC_MEMBER); break;
                default:
                    System.err.println("... Action POST inconnue: " + actionPath); response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Action POST inconnue: " + actionPath);
            }
        } catch (InvalidConferenceIdException e) { System.err.println("... InvalidConferenceIdException (POST): " + e.getMessage()); response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); }
        catch (SQLException e) {
            System.err.println("... SQLException (POST): " + e.getMessage());
            int confIdForRetry = getConferenceIdFromRequestQuietly(request); String errorMsg = "Erreur base de données: " + e.getMessage(); e.printStackTrace();
            try {
                if (actionPath.contains("/steering/")) { request.setAttribute("steeringError_" + getRoleFromAction(actionPath), errorMsg); showSteeringPage(request, response, currentUser, confIdForRetry); }
                else if (actionPath.startsWith("/conference/add") || actionPath.startsWith("/conference/remove")) { request.setAttribute("managementError", errorMsg); showManageConferencePage(request, response, currentUser, confIdForRetry); }
                else { handleGenericError(request, response, e, "Erreur DB (POST)"); }
            } catch (Exception displayError) { handleGenericError(request, response, e, "Erreur DB (POST) + échec réaffichage"); }
        } catch (Exception e) {
            System.err.println("... Exception générique (POST): " + e.getMessage());
            if ("/conference/create".equals(actionPath)) { request.setAttribute("formError", "Erreur: " + e.getMessage()); e.printStackTrace(); showCreateConferenceForm(request, response); }
            else { handleGenericError(request, response, e, "Erreur serveur (POST)"); }
        }
        finally { System.out.println("ConferenceServlet doPost: Sortie pour " + actionPath); }
    }

    // ======================== Méthodes Utilitaires ========================
    private int getConferenceIdFromRequest(HttpServletRequest request) throws InvalidConferenceIdException { /* ... Code validé ... */
        String confIdParam = request.getParameter("confId"); if (confIdParam == null || confIdParam.trim().isEmpty()) { throw new InvalidConferenceIdException("Paramètre 'confId' manquant."); } try { int confId = Integer.parseInt(confIdParam); if (confId <= 0) { throw new InvalidConferenceIdException("ID conférence invalide: " + confIdParam); } return confId; } catch (NumberFormatException e) { throw new InvalidConferenceIdException("Format ID conférence invalide: " + confIdParam); }
    }
    private int getConferenceIdFromRequestQuietly(HttpServletRequest request) { /* ... Code validé ... */
         String confIdParam = request.getParameter("confId"); if (confIdParam != null) { try { int confId = Integer.parseInt(confIdParam); if (confId > 0) return confId; } catch (NumberFormatException e) {} } return 0;
     }
    private void handleGenericError(HttpServletRequest req, HttpServletResponse res, Exception e, String ctx) throws IOException, ServletException { /* ... Code validé ... */
        System.err.println("ERREUR SERVEUR - Contexte: " + ctx); e.printStackTrace(); res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne. Consultez les logs.");
    }
    private String getRoleFromAction(String actionPath) { /* ... Code validé ... */
        if (actionPath.contains("ScMember")) return Role.SC_MEMBER.name(); if (actionPath.contains("PcMember")) return Role.PC_MEMBER.name(); if (actionPath.contains("SteeringMember")) return Role.STEERING_COMMITTEE.name(); return "";
    }
    private String fn(String input) { if (input == null) return ""; return input.replace("<", "<").replace(">", ">").replace("\"", ""); }

    // ======================== Méthodes Actions GET ========================
    private void showCreateConferenceForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { /* ... Code validé ... */
         System.out.println("--- Dans showCreateConferenceForm ---");
         RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/conference/create_conference.jsp"); dispatcher.forward(request, response);
     }
    private void showManageConferencePage(HttpServletRequest request, HttpServletResponse response, User user, int conferenceId) throws ServletException, IOException, SQLException, InvalidConferenceIdException { /* ... Code validé ... */
         System.out.println("--- Dans showManageConferencePage pour confId=" + conferenceId + " ---");
        if (!userConferenceRoleDAO.userHasRole(user.getUserId(), conferenceId, Role.CHAIR)) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé."); return; }
        Optional<Conference> confOpt = conferenceDAO.findConferenceById(conferenceId); if (confOpt.isEmpty()) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        List<User> steeringMembers = userConferenceRoleDAO.findUsersByConferenceAndRole(conferenceId, Role.STEERING_COMMITTEE); List<User> scMembers = userConferenceRoleDAO.findUsersByConferenceAndRole(conferenceId, Role.SC_MEMBER); List<User> pcMembers = userConferenceRoleDAO.findUsersByConferenceAndRole(conferenceId, Role.PC_MEMBER);
        request.setAttribute("conference", confOpt.get()); request.setAttribute("steeringCommitteeMembers", steeringMembers); request.setAttribute("scMembers", scMembers); request.setAttribute("pcMembers", pcMembers); request.setAttribute("confId", conferenceId);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/conference/manage_conference.jsp"); dispatcher.forward(request, response);
    }
    private void showSteeringPage(HttpServletRequest request, HttpServletResponse response, User user, int conferenceId) throws ServletException, IOException, SQLException, InvalidConferenceIdException { /* ... Code validé ... */
         System.out.println("--- Dans showSteeringPage pour confId=" + conferenceId + " ---");
         boolean canAccess = userConferenceRoleDAO.userHasRole(user.getUserId(), conferenceId, Role.STEERING_COMMITTEE) || userConferenceRoleDAO.userHasRole(user.getUserId(), conferenceId, Role.CHAIR); if (!canAccess) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès réservé."); return; }
         Optional<Conference> confOpt = conferenceDAO.findConferenceById(conferenceId); if (confOpt.isEmpty()) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
         List<User> scMembers = userConferenceRoleDAO.findUsersByConferenceAndRole(conferenceId, Role.SC_MEMBER); List<User> pcMembers = userConferenceRoleDAO.findUsersByConferenceAndRole(conferenceId, Role.PC_MEMBER);
         request.setAttribute("conference", confOpt.get()); request.setAttribute("scMembers", scMembers); request.setAttribute("pcMembers", pcMembers); request.setAttribute("confId", conferenceId);
         RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/steering/steering_dashboard.jsp"); dispatcher.forward(request, response);
    }

    // ======================== Méthodes Actions POST =======================

    /** Traite la création d'une nouvelle conférence. (Complété) */
    private void processCreateConference(HttpServletRequest request, HttpServletResponse response, User creator) throws ServletException, IOException {
        System.out.println("--- Dans processCreateConference ---");
        String name = request.getParameter("name"); String acronym = request.getParameter("acronym"); String website = request.getParameter("website"); String typeStr = request.getParameter("type");
        String startDateStr = request.getParameter("startDate"); String endDateStr = request.getParameter("endDate"); String location = request.getParameter("location"); String description = request.getParameter("description");
        String subDStr = request.getParameter("submissionDeadline"); String revDStr = request.getParameter("reviewDeadline"); String notDStr = request.getParameter("notificationDate"); String camDStr = request.getParameter("cameraReadyDeadline");
        StringBuilder errors = new StringBuilder();
        if(name==null||name.trim().isEmpty()) errors.append("Nom requis.<br>"); if(acronym==null||acronym.trim().isEmpty()) errors.append("Acronyme requis.<br>"); if(typeStr==null||typeStr.isEmpty()) errors.append("Type requis.<br>"); if(startDateStr==null||startDateStr.isEmpty()) errors.append("Date début requise.<br>"); if(endDateStr==null||endDateStr.isEmpty()) errors.append("Date fin requise.<br>"); if(subDStr==null||subDStr.isEmpty()) errors.append("Deadline soumission requise.<br>"); if(revDStr==null||revDStr.isEmpty()) errors.append("Deadline évaluation requise.<br>"); if(notDStr==null||notDStr.isEmpty()) errors.append("Date notification requise.<br>"); if(camDStr==null||camDStr.isEmpty()) errors.append("Deadline finale requise.<br>");
        ConferenceType type = null; Date startDate = null; Date endDate = null; Timestamp submissionDeadline = null, reviewDeadline = null, notificationDate = null, cameraReadyDeadline = null;
         try { if(typeStr!=null&&!typeStr.isEmpty()) type=ConferenceType.valueOf(typeStr); if(startDateStr!=null&&!startDateStr.isEmpty()) startDate=Date.valueOf(LocalDate.parse(startDateStr)); if(endDateStr!=null&&!endDateStr.isEmpty()) endDate=Date.valueOf(LocalDate.parse(endDateStr)); DateTimeFormatter dtF=DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"); if(subDStr!=null&&!subDStr.isEmpty()) submissionDeadline=Timestamp.valueOf(LocalDateTime.parse(subDStr, dtF)); if(revDStr!=null&&!revDStr.isEmpty()) reviewDeadline=Timestamp.valueOf(LocalDateTime.parse(revDStr, dtF)); if(camDStr!=null&&!camDStr.isEmpty()) cameraReadyDeadline=Timestamp.valueOf(LocalDateTime.parse(camDStr, dtF)); if(notDStr!=null&&!notDStr.isEmpty()) notificationDate=Timestamp.valueOf(LocalDate.parse(notDStr).atStartOfDay()); if(startDate!=null&&endDate!=null&&startDate.after(endDate)) errors.append("Date fin avant début.<br>"); /*TODO: Autres val dates*/ } catch (Exception e) { errors.append("Erreur format date/type.<br>"); e.printStackTrace();}
         String logoFileName = null; try { Part filePart = request.getPart("logo"); if (filePart!=null&&filePart.getSize()>0) { String submFn=Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); if(submFn!=null&&!submFn.trim().isEmpty()){ String ext="";int i=submFn.lastIndexOf('.');if(i>0)ext=submFn.substring(i+1).toLowerCase(); if(!Arrays.asList("jpg","jpeg","png","gif").contains(ext)){errors.append("Format logo invalide.<br>");} else { logoFileName=UUID.randomUUID().toString()+"."+ext; String upPath=this.uploadBasePath+File.separator+logoFileName; try(InputStream fic=filePart.getInputStream()){Files.copy(fic,Paths.get(upPath),StandardCopyOption.REPLACE_EXISTING);}catch(IOException ex){errors.append("Erreur upload logo.<br>");logoFileName=null;ex.printStackTrace();}}}}}catch(Exception e){errors.append("Erreur upload logo.<br>");e.printStackTrace();}
         if (errors.length() > 0) { request.setAttribute("formError",errors.toString()); request.setAttribute("nameValue",name);request.setAttribute("acronymValue",acronym);request.setAttribute("websiteValue",website);request.setAttribute("typeValue",typeStr);request.setAttribute("startDateValue",startDateStr);request.setAttribute("endDateValue",endDateStr);request.setAttribute("locationValue",location);request.setAttribute("descriptionValue",description);request.setAttribute("submissionDeadlineValue",subDStr);request.setAttribute("reviewDeadlineValue",revDStr);request.setAttribute("notificationDateValue",notDStr);request.setAttribute("cameraReadyDeadlineValue",camDStr); try{showCreateConferenceForm(request,response);}catch(Exception e){throw new ServletException(e);} return; }
         Conference conf = new Conference(); conf.setName(name.trim());conf.setAcronym(acronym.trim());conf.setWebsite(website!=null?website.trim():null);conf.setType(type);conf.setStartDate(startDate);conf.setEndDate(endDate);conf.setLocation(location!=null?location.trim():null);conf.setDescription(description!=null?description.trim():null);conf.setSubmissionDeadline(submissionDeadline);conf.setReviewDeadline(reviewDeadline);conf.setNotificationDate(notificationDate);conf.setCameraReadyDeadline(cameraReadyDeadline);conf.setCreatedByUserId(creator.getUserId());conf.setLogoPath(logoFileName);
         try { conferenceDAO.createConference(conf); if(conf.getConferenceId()<=0)throw new SQLException("ID Conf non généré."); userConferenceRoleDAO.assignRoleToUser(creator.getUserId(),conf.getConferenceId(),Role.CHAIR); request.getSession().setAttribute("successMessage","Conférence '"+fn(conf.getAcronym())+"' créée !"); response.sendRedirect(request.getContextPath()+"/dashboard"); }
         catch(SQLException e){ e.printStackTrace(); if(logoFileName!=null){try{Files.deleteIfExists(Paths.get(this.uploadBasePath+File.separator+logoFileName));}catch(IOException ioEx){ioEx.printStackTrace();}} request.setAttribute("formError","Erreur DB: "+e.getMessage()); request.setAttribute("nameValue",name);/*...pré-remplir...*/ try{showCreateConferenceForm(request,response);}catch(Exception e2){throw new ServletException(e);} }
    }

    /** Traite l'ajout générique d'un membre à un comité. (Permissions Adaptées) */
    private void processAddMember(HttpServletRequest request, HttpServletResponse response, User currentUser, int conferenceId, Role roleToAdd, String committeeName) throws ServletException, IOException, SQLException, InvalidConferenceIdException {
        System.out.println("--- Dans processAddMember pour " + roleToAdd + " ---"); // Log
        // --- VÉRIFICATION DROITS ADAPTÉE ---
        boolean canManage = false; if (roleToAdd==Role.STEERING_COMMITTEE) { canManage=userConferenceRoleDAO.userHasRole(currentUser.getUserId(),conferenceId,Role.CHAIR); } else if (roleToAdd==Role.SC_MEMBER || roleToAdd==Role.PC_MEMBER) { canManage=userConferenceRoleDAO.userHasRole(currentUser.getUserId(),conferenceId,Role.CHAIR)||userConferenceRoleDAO.userHasRole(currentUser.getUserId(),conferenceId,Role.STEERING_COMMITTEE); }
        if (!canManage) { String reqRole=(roleToAdd==Role.STEERING_COMMITTEE)?"Président":"Président/Pilotage"; System.err.println("ALERTE SECU: Ajout refusé - User "+currentUser.getUserId()+" non "+reqRole); response.sendError(HttpServletResponse.SC_FORBIDDEN,"Réservé au "+reqRole+"."); return; }
        // --- Fin Vérification Droits ---
        String userIdentifierParamName="userIdentifier_"+roleToAdd.name(); String userIdentifier=request.getParameter(userIdentifierParamName); System.out.println("Email reçu ("+userIdentifierParamName+"): "+userIdentifier); // Log
        boolean fromSteering=request.getServletPath().contains("/steering/"); String errorAttr=(fromSteering?"steeringError_":"managementError_")+roleToAdd.name(); String valueAttr=userIdentifierParamName+(fromSteering?"_steering":""); String successAttr=fromSteering?"steeringSuccess":"managementSuccess";
        if(userIdentifier==null||userIdentifier.trim().isEmpty()){request.setAttribute(errorAttr,"Email requis."); System.out.println("Erreur: Email requis.");}
        else{ Optional<User> userOpt=userDAO.findUserByEmail(userIdentifier.trim()); if(userOpt.isEmpty()){request.setAttribute(errorAttr,"Utilisateur non trouvé: "+fn(userIdentifier));request.setAttribute(valueAttr,userIdentifier); System.out.println("Erreur: Utilisateur non trouvé.");}
        else{ User userToAdd=userOpt.get(); if(userConferenceRoleDAO.userHasRole(userToAdd.getUserId(),conferenceId,roleToAdd)){request.setAttribute(successAttr,fn(userToAdd.getEmail())+" est déjà membre: "+committeeName); System.out.println("Info: Déjà membre.");}
        else{userConferenceRoleDAO.assignRoleToUser(userToAdd.getUserId(),conferenceId,roleToAdd);request.setAttribute(successAttr,fn(userToAdd.getEmail())+" ajouté à: "+committeeName); System.out.println("Succès: Membre ajouté.");}}}
        if(fromSteering){System.out.println("... Réaffichage steering page"); showSteeringPage(request,response,currentUser,conferenceId);}else{System.out.println("... Réaffichage manage page"); showManageConferencePage(request,response,currentUser,conferenceId);}
    }

    /** Traite la suppression générique d'un membre d'un comité. (Permissions Adaptées) */
    private void processRemoveMember(HttpServletRequest request, HttpServletResponse response, User currentUser, int conferenceId, Role roleToRemove) throws ServletException, IOException, SQLException, InvalidConferenceIdException {
        System.out.println("--- Dans processRemoveMember pour " + roleToRemove + " ---"); // Log
        // --- VÉRIFICATION DROITS ADAPTÉE ---
        boolean canManage = false; if (roleToRemove==Role.STEERING_COMMITTEE) { canManage=userConferenceRoleDAO.userHasRole(currentUser.getUserId(),conferenceId,Role.CHAIR); } else if (roleToRemove==Role.SC_MEMBER || roleToRemove==Role.PC_MEMBER) { canManage=userConferenceRoleDAO.userHasRole(currentUser.getUserId(),conferenceId,Role.CHAIR)||userConferenceRoleDAO.userHasRole(currentUser.getUserId(),conferenceId,Role.STEERING_COMMITTEE); }
        if (!canManage) { String reqRole=(roleToRemove==Role.STEERING_COMMITTEE)?"Président":"Président/Pilotage"; System.err.println("ALERTE SECU: Suppr refusée - User "+currentUser.getUserId()+" non "+reqRole); response.sendError(HttpServletResponse.SC_FORBIDDEN,"Réservé au "+reqRole+"."); return; }
        // --- Fin Vérification Droits ---
        int memberIdToRemove = 0; try { memberIdToRemove=Integer.parseInt(request.getParameter("memberIdToRemove")); System.out.println("ID à supprimer: " + memberIdToRemove); } catch (Exception e) {} if (memberIdToRemove<=0) { response.sendError(HttpServletResponse.SC_BAD_REQUEST,"ID Membre invalide."); return; }
        boolean removed=userConferenceRoleDAO.removeRoleFromUser(memberIdToRemove,conferenceId,roleToRemove); System.out.println("Résultat suppression: " + removed);
        boolean fromSteering=request.getServletPath().contains("/steering/"); String successAttr=fromSteering?"steeringSuccess":"managementSuccess"; String errorAttr=(fromSteering?"steeringError_":"managementError_")+roleToRemove.name();
        if(removed){request.setAttribute(successAttr,"Membre retiré.");}else{request.setAttribute(errorAttr,"Retrait échoué.");}
        if(fromSteering){System.out.println("... Réaffichage steering page"); showSteeringPage(request,response,currentUser,conferenceId);}else{System.out.println("... Réaffichage manage page"); showManageConferencePage(request,response,currentUser,conferenceId);}
    }

    /** Exception interne pour les ID invalides */
    private static class InvalidConferenceIdException extends Exception { public InvalidConferenceIdException(String message) { super(message); } }

} // Fin Servlet