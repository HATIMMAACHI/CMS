package servlets;

// Imports Java standard
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Imports Jakarta EE (Servlet API)
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

// Imports DAO (Adapter le nom du package si besoin)
import DAO.ConferenceDAOImpl;
import DAO.SubmissionAuthorDAOImpl;
import DAO.SubmissionDAOImpl;
import DAO.UserConferenceRoleDAOImpl;
import DAO.UserDAOImpl;

// Imports Models (Adapter le nom du package si besoin)
import models.Conference;
import models.Submission;
import models.Submission.SubmissionStatus;
import models.SubmissionAuthor;
import models.User;
import models.UserConferenceRole.Role;


/**
 * Servlet pour gérer la soumission, la liste, et la modification des articles par les auteurs.
 */
@WebServlet(name = "SubmissionServlet", urlPatterns = {
        "/submission/submit",   // GET: Affiche formulaire création, POST: Traite création
        "/submission/list",     // GET: Affiche liste des soumissions de l'auteur
        "/submission/edit",     // GET: Affiche formulaire édition
        "/submission/update"    // POST: Traite mise à jour
})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2 MB
    maxFileSize = 1024 * 1024 * 20, // 20 MB (PDF)
    maxRequestSize = 1024 * 1024 * 25 // 25 MB (Total)
)
public class SubmissionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private ConferenceDAOImpl conferenceDAO;
    private SubmissionDAOImpl submissionDAO;
    private SubmissionAuthorDAOImpl submissionAuthorDAO;
    private UserDAOImpl userDAO;
    private UserConferenceRoleDAOImpl userConferenceRoleDAO;
    private static final String UPLOAD_DIR_PAPER = "uploads" + File.separator + "papers";
    private String paperUploadBasePath;

    @Override
    public void init() throws ServletException {
        super.init();
        this.conferenceDAO = new ConferenceDAOImpl();
        this.submissionDAO = new SubmissionDAOImpl();
        this.submissionAuthorDAO = new SubmissionAuthorDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.userConferenceRoleDAO = new UserConferenceRoleDAOImpl();
        try {
            String contextPath = getServletContext().getRealPath("");
            if (contextPath == null) { throw new ServletException("Upload path null"); }
            this.paperUploadBasePath = contextPath + File.separator + UPLOAD_DIR_PAPER;
            File uploadDir = new File(this.paperUploadBasePath);
            if (!uploadDir.exists()) { if (!uploadDir.mkdirs()) { throw new ServletException("Cannot create upload dir PDF: " + this.paperUploadBasePath); } System.out.println("PDF Upload dir created: " + this.paperUploadBasePath); }
            else { System.out.println("PDF Upload dir found: " + this.paperUploadBasePath); }
        } catch (Exception e) { throw new ServletException("Erreur init upload path PDF", e); }
    }

    // ======================== doGet Router ========================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendRedirect(request.getContextPath() + "/login?error=session_expired"); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();
        System.out.println("SubmissionServlet doGet: " + actionPath);

        try {
            switch (actionPath) {
                case "/submission/submit": showSubmissionForm(request, response, currentUser); break;
                case "/submission/list": showSubmissionList(request, response, currentUser); break;
                case "/submission/edit": showEditForm(request, response, currentUser); break;
                default: response.sendError(HttpServletResponse.SC_NOT_FOUND, "Action GET inconnue.");
            }
        } catch (InvalidIdException e) { System.err.println("... InvalidIdException (GET): " + e.getMessage()); response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); }
        catch (SQLException e) { System.err.println("... SQLException (GET): " + e.getMessage()); handleGenericError(request, response, e, "Erreur DB (GET)"); }
        catch (Exception e) { System.err.println("... Exception (GET): " + e.getMessage()); handleGenericError(request, response, e, "Erreur serveur (GET)"); }
        finally { System.out.println("SubmissionServlet doGet: Sortie pour " + actionPath); }
    }

    // ======================== doPost Router =======================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non autorisé."); return; }
        User currentUser = (User) session.getAttribute("user");
        String actionPath = request.getServletPath();
        System.out.println("SubmissionServlet doPost: " + actionPath);

        try {
            switch (actionPath) {
                case "/submission/submit": processPaperSubmission(request, response, currentUser); break;
                case "/submission/update": processPaperUpdate(request, response, currentUser); break;
                default: response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Action POST inconnue: " + actionPath);
            }
        } catch (InvalidIdException e) { System.err.println("... InvalidIdException (POST): " + e.getMessage()); response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); }
        catch (SQLException e) {
             System.err.println("... SQLException (POST): " + e.getMessage());
             String errorMsg = "Erreur base de données: " + e.getMessage(); e.printStackTrace();
             if ("/submission/submit".equals(actionPath) || "/submission/update".equals(actionPath)) {
                 request.setAttribute("submissionError", errorMsg);
                 try {
                      if ("/submission/submit".equals(actionPath)) { showSubmissionForm(request, response, currentUser); }
                      else { int submissionIdForRetry = getIntParamFromRequestQuietly(request, "submissionId"); showEditForm(request, response, currentUser); }
                 } catch (Exception displayError) { handleGenericError(request, response, e, "Erreur DB (POST) + échec réaffichage"); }
             } else { handleGenericError(request, response, e, "Erreur DB (POST)"); }
        } catch (Exception e) {
             System.err.println("... Exception générique (POST): " + e.getMessage());
             String errorMsg = "Erreur inattendue: " + e.getMessage(); e.printStackTrace();
             if ("/submission/submit".equals(actionPath) || "/submission/update".equals(actionPath)) {
                  request.setAttribute("submissionError", errorMsg);
                  try {
                      if ("/submission/submit".equals(actionPath)) { showSubmissionForm(request, response, currentUser); }
                      else { int submissionIdForRetry = getIntParamFromRequestQuietly(request, "submissionId"); showEditForm(request, response, currentUser); }
                  } catch (Exception e2) { throw new ServletException(e); }
             } else { handleGenericError(request, response, e, "Erreur serveur (POST)"); }
        }
        finally { System.out.println("SubmissionServlet doPost: Sortie pour " + actionPath); }
    }

    // ======================== Méthodes Actions GET ========================

    /** Prépare et affiche la liste des soumissions de l'auteur pour une conférence. (Implémenté) */
    private void showSubmissionList(HttpServletRequest request, HttpServletResponse response, User currentUser) throws ServletException, IOException, SQLException, InvalidIdException {
        // ... (Code Validé) ...
        System.out.println("--- Dans showSubmissionList ---"); int confId=getIntParamFromRequest(request,"confId"); Optional<Conference>confOpt=conferenceDAO.findConferenceById(confId); if(confOpt.isEmpty()){throw new InvalidIdException("Conf ID "+confId+" non trouvée.");} List<Submission>subs=submissionDAO.findSubmissionsByAuthor(confId,currentUser.getUserId()); LocalDateTime now=LocalDateTime.now();Timestamp nowTs=Timestamp.valueOf(now); boolean open=confOpt.get().getSubmissionDeadline()!=null&&confOpt.get().getSubmissionDeadline().after(nowTs); request.setAttribute("conference",confOpt.get());request.setAttribute("submissions",subs); request.setAttribute("confId",confId);request.setAttribute("submissionOpen",open); RequestDispatcher d=request.getRequestDispatcher("/WEB-INF/jsp/submission/submission_list.jsp"); d.forward(request,response);
    }

    /** Prépare et affiche le formulaire de SOUMISSION (création). (Validé) */
    private void showSubmissionForm(HttpServletRequest request, HttpServletResponse response, User currentUser) throws ServletException, IOException, SQLException {
         // ... (Code Validé - récupère toutes les confs ouvertes) ...
        System.out.println("--- Dans showSubmissionForm (Création) ---"); List<Conference> allConf=conferenceDAO.findAllConferences(); LocalDateTime now=LocalDateTime.now(); Timestamp nowTs=Timestamp.valueOf(now); List<Conference> openConf=allConf.stream().filter(c->c.getSubmissionDeadline()!=null&&c.getSubmissionDeadline().after(nowTs)).collect(Collectors.toList()); request.setAttribute("conferences",openConf); if(request.getAttribute("authorsValue")==null){List<User> ca=new ArrayList<>(); ca.add(currentUser); request.setAttribute("authorsValue",ca); request.setAttribute("correspondingAuthorIdValue",String.valueOf(currentUser.getUserId())); request.setAttribute("authorIdsValue",List.of(String.valueOf(currentUser.getUserId())));} request.setAttribute("isEditMode",false); RequestDispatcher d=request.getRequestDispatcher("/WEB-INF/jsp/submission/submit_paper.jsp"); d.forward(request,response);
    }

    /** Prépare et affiche le formulaire de MODIFICATION d'une soumission. (Validé) */
    private void showEditForm(HttpServletRequest request, HttpServletResponse response, User currentUser) throws ServletException, IOException, SQLException, InvalidIdException {
        // ... (Code Validé - récupère sub, vérifie droits/deadline, pré-remplit) ...
         System.out.println("--- Dans showEditForm ---"); int submissionId=getIntParamFromRequest(request,"submissionId"); Optional<Submission> subOpt=submissionDAO.findSubmissionById(submissionId); if(subOpt.isEmpty()){response.sendError(HttpServletResponse.SC_NOT_FOUND,"Sub non trouvée.");return;} Submission sub=subOpt.get(); List<User> authors=submissionAuthorDAO.findAuthorsBySubmissionId(submissionId); boolean isAuthor=authors.stream().anyMatch(a->a.getUserId()==currentUser.getUserId()); if(!isAuthor){response.sendError(HttpServletResponse.SC_FORBIDDEN,"Non auteur.");return;} Optional<Conference> confOpt=conferenceDAO.findConferenceById(sub.getConferenceId()); if(confOpt.isEmpty()){throw new ServletException("Conf associée non trouvée.");} LocalDateTime now=LocalDateTime.now();Timestamp nowTs=Timestamp.valueOf(now); boolean canEdit=sub.getStatus()==SubmissionStatus.SUBMITTED&&confOpt.get().getSubmissionDeadline()!=null&&confOpt.get().getSubmissionDeadline().after(nowTs); if(!canEdit){request.getSession().setAttribute("errorMessage","Modif non autorisée.");response.sendRedirect(request.getContextPath()+"/submission/list?confId="+sub.getConferenceId());return;} if(request.getAttribute("titleValue")==null){request.setAttribute("conferenceIdValue",String.valueOf(sub.getConferenceId()));request.setAttribute("titleValue",sub.getTitle());request.setAttribute("abstractTextValue",sub.getAbstractText());request.setAttribute("keywordsValue",sub.getKeywords()); String coEmails=authors.stream().filter(a->a.getUserId()!=currentUser.getUserId()).map(User::getEmail).collect(Collectors.joining(", "));request.setAttribute("coAuthorEmailsValue",coEmails); Optional<User> correspOpt=authors.stream().filter(a->{try{List<SubmissionAuthor> links=submissionAuthorDAO.findSubmissionAuthorLinks(submissionId);return links.stream().anyMatch(l->l.getUserId()==a.getUserId()&&l.isCorresponding());}catch(SQLException e){return false;}}).findFirst();correspOpt.ifPresent(u->request.setAttribute("correspondingAuthorEmailValue",u.getEmail()));} request.setAttribute("isEditMode",true);request.setAttribute("submissionId",submissionId);request.setAttribute("conferences",List.of(confOpt.get())); RequestDispatcher d=request.getRequestDispatcher("/WEB-INF/jsp/submission/submit_paper.jsp"); d.forward(request,response);
    }

    // ======================== Méthodes Actions POST =======================

    
    
    
    
    
    
    
    
    
    
    
    
    
    /** Traite la CRÉATION d'une nouvelle soumission. (Validé) */
    private void processPaperSubmission(HttpServletRequest request, HttpServletResponse response, User currentUser) throws ServletException, IOException, SQLException {
        System.out.println("--- Dans processPaperSubmission ---");
        String confIdStr = request.getParameter("conferenceId");
        String title = request.getParameter("title");
        String abstractText = request.getParameter("abstractText");
        String keywords = request.getParameter("keywords");
        String coAuthorEmailsStr = request.getParameter("coAuthorEmails");
        String correspEmail = request.getParameter("correspondingAuthorEmail");
        StringBuilder err = new StringBuilder();
        int confId = 0;
        List<User> authors = new ArrayList<>();
        User correspAuthor = null;
        authors.add(currentUser);

        if (coAuthorEmailsStr != null && !coAuthorEmailsStr.trim().isEmpty()) {
            String[] emails = coAuthorEmailsStr.trim().split("\\s*[,;]+\\s*");
            for (String e : emails) {
                if (!e.isEmpty()) {
                    if (e.equalsIgnoreCase(currentUser.getEmail())) continue;
                    Optional<User> coOpt = userDAO.findUserByEmail(e);
                    if (coOpt.isPresent()) {
                        if (!authors.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(e)))
                            authors.add(coOpt.get());
                    } else {
                        err.append("Co-auteur non trouvé: ").append(fn(e)).append(".<br>");
                    }
                }
            }
        }

        if (correspEmail == null || correspEmail.trim().isEmpty()) {
            err.append("Email corresp. requis.<br>");
        } else {
            boolean f = false;
            for (User a : authors) {
                if (a.getEmail().equalsIgnoreCase(correspEmail.trim())) {
                    correspAuthor = a;
                    f = true;
                    break;
                }
            }
            if (!f) err.append("Email corresp. ('").append(fn(correspEmail)).append("') invalide.<br>");
        }

        if (confIdStr == null || confIdStr.isEmpty()) {
            err.append("Conf requis.<br>");
        } else {
            try {
                confId = Integer.parseInt(confIdStr);
                if (confId <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                err.append("ID Conf invalide.<br>");
            }
        }

        if (title == null || title.trim().isEmpty()) err.append("Titre requis.<br>");
        if (abstractText == null || abstractText.trim().isEmpty()) err.append("Résumé requis.<br>");
        if (keywords == null || keywords.trim().isEmpty()) err.append("Mots-clés requis.<br>");

        if (confId > 0) {
            Optional<Conference> co = conferenceDAO.findConferenceById(confId);
            if (co.isPresent()) {
                Timestamp dl = co.get().getSubmissionDeadline();
                if (dl == null || Timestamp.valueOf(LocalDateTime.now()).after(dl))
                    err.append("Deadline dépassée.<br>");
            } else {
                err.append("Conf invalide.<br>");
            }
        }

        String paperFn = null;
        Part fp = request.getPart("paperFile");
        if (fp == null || fp.getSize() == 0) {
            err.append("PDF requis.<br>");
        } else {
            String sfn = Paths.get(fp.getSubmittedFileName()).getFileName().toString();
            if (sfn.isEmpty() || !sfn.toLowerCase().endsWith(".pdf")) {
                err.append("PDF invalide.<br>");
            } else {
                paperFn = UUID.randomUUID().toString() + ".pdf";
                String upPath = this.paperUploadBasePath + File.separator + paperFn;
                try (InputStream fic = fp.getInputStream()) {
                    Files.copy(fic, Paths.get(upPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    err.append("Erreur upload PDF.<br>");
                    paperFn = null;
                    ex.printStackTrace();
                }
            }
        }

        if (err.length() > 0) {
            request.setAttribute("submissionError", err.toString());
            request.setAttribute("conferenceIdValue", confIdStr);
            request.setAttribute("titleValue", title);
            request.setAttribute("abstractTextValue", abstractText);
            request.setAttribute("keywordsValue", keywords);
            request.setAttribute("coAuthorEmailsValue", coAuthorEmailsStr);
            request.setAttribute("correspondingAuthorEmailValue", correspEmail);
            showSubmissionForm(request, response, currentUser);
            return;
        }

        Submission sub = new Submission();
        sub.setConferenceId(confId);
        sub.setTitle(title.trim());
        sub.setAbstractText(abstractText.trim());
        sub.setKeywords(keywords.trim());
        sub.setFilePath(paperFn);
        sub.setStatus(SubmissionStatus.SUBMITTED);
        sub.setUniquePaperId(UUID.randomUUID().toString());

        try {
            submissionDAO.createSubmission(sub);
            if (sub.getSubmissionId() <= 0) throw new SQLException("ID sub non généré.");

            int order = 1;
            for (User author : authors) {
                SubmissionAuthor sa = new SubmissionAuthor(sub.getSubmissionId(), author.getUserId(), (author.getUserId() == correspAuthor.getUserId()), order++);
                submissionAuthorDAO.addAuthorToSubmission(sa);
                if (!userConferenceRoleDAO.userHasRole(author.getUserId(), confId, Role.AUTHOR))
                    userConferenceRoleDAO.assignRoleToUser(author.getUserId(), confId, Role.AUTHOR);
            }

            request.getSession().setAttribute("successMessage", "Article '" + fn(sub.getTitle()) + "' soumis !");
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (SQLException e) {
            if (paperFn != null) {
                try {
                    Files.deleteIfExists(Paths.get(this.paperUploadBasePath + File.separator + paperFn));
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                }
            }
            request.setAttribute("submissionError", "Erreur DB: " + e.getMessage());
            request.setAttribute("conferenceIdValue", confIdStr);
            request.setAttribute("titleValue", title);
            request.setAttribute("abstractTextValue", abstractText);
            request.setAttribute("keywordsValue", keywords);
            request.setAttribute("coAuthorEmailsValue", coAuthorEmailsStr);
            request.setAttribute("correspondingAuthorEmailValue", correspEmail);
            showSubmissionForm(request, response, currentUser);
        }
    }
  
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private void processPaperUpdate(HttpServletRequest request, HttpServletResponse response, User currentUser) throws ServletException, IOException, SQLException, InvalidIdException {
        System.out.println("--- Dans processPaperUpdate ---");
        int submissionId = getIntParamFromRequest(request, "submissionId");
        String confIdStr = request.getParameter("conferenceId");
        String title = request.getParameter("title");
        String abstractText = request.getParameter("abstractText");
        String keywords = request.getParameter("keywords");
        String coAuthorEmailsStr = request.getParameter("coAuthorEmails");
        String correspEmail = request.getParameter("correspondingAuthorEmail");
        Part fp = request.getPart("paperFile");

        StringBuilder err = new StringBuilder();
        int confId = 0;
        List<User> authors = new ArrayList<>();
        User correspAuthor = null;
        Submission exSub = null;

        Optional<Submission> subOpt = submissionDAO.findSubmissionById(submissionId);
        if (subOpt.isEmpty()) {
            err.append("Soumission non trouvée.<br>");
        } else {
            exSub = subOpt.get();
            try {
                confId = Integer.parseInt(confIdStr);
            } catch (Exception e) {
                confId = 0;
            }
            if (confId <= 0 || exSub.getConferenceId() != confId) {
                err.append("Incohérence dans l'ID de conférence.<br>");
            }

            List<User> dbAuthors = submissionAuthorDAO.findAuthorsBySubmissionId(submissionId);
            boolean isAuthor = dbAuthors.stream().anyMatch(a -> a.getUserId() == currentUser.getUserId());
            if (!isAuthor) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Vous n'êtes pas auteur de cette soumission.");
                return;
            }

            Optional<Conference> confOpt = conferenceDAO.findConferenceById(confId);
            if (confOpt.isEmpty()) {
                err.append("Conférence non trouvée.<br>");
            } else {
                LocalDateTime now = LocalDateTime.now();
                Timestamp nowTs = Timestamp.valueOf(now);
                boolean canEdit = exSub.getStatus() == SubmissionStatus.SUBMITTED &&
                    confOpt.get().getSubmissionDeadline() != null &&
                    confOpt.get().getSubmissionDeadline().after(nowTs);
                if (!canEdit) {
                    err.append("La soumission ne peut plus être modifiée (date limite dépassée ou statut différent).<br>");
                }
            }
        }

        if (title == null || title.trim().isEmpty()) err.append("Titre requis.<br>");
        if (abstractText == null || abstractText.trim().isEmpty()) err.append("Résumé requis.<br>");
        if (keywords == null || keywords.trim().isEmpty()) err.append("Mots-clés requis.<br>");

        authors.add(currentUser);
        if (coAuthorEmailsStr != null && !coAuthorEmailsStr.trim().isEmpty()) {
            String[] emails = coAuthorEmailsStr.trim().split("\\s*[,;]+\\s*");
            for (String e : emails) {
                if (!e.isEmpty() && !e.equalsIgnoreCase(currentUser.getEmail())) {
                    Optional<User> coOpt = userDAO.findUserByEmail(e);
                    if (coOpt.isPresent()) {
                        if (authors.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(e))) {
                            authors.add(coOpt.get());
                        }
                    } else {
                        err.append("Co-auteur non trouvé: ").append(fn(e)).append(".<br>");
                    }
                }
            }
        }

        if (correspEmail == null || correspEmail.trim().isEmpty()) {
            err.append("Email de l'auteur correspondant requis.<br>");
        } else {
            for (User a : authors) {
                if (a.getEmail().equalsIgnoreCase(correspEmail.trim())) {
                    correspAuthor = a;
                    break;
                }
            }
            if (correspAuthor == null) {
                err.append("Email de l'auteur correspondant invalide.<br>");
            }
        }

        String newPaperFn = (exSub != null) ? exSub.getFilePath() : null;
        boolean delOld = false;
        String oldPath = newPaperFn;

        if (fp != null && fp.getSize() > 0) {
            String sfn = Paths.get(fp.getSubmittedFileName()).getFileName().toString();
            if (sfn.isEmpty() || !sfn.toLowerCase().endsWith(".pdf")) {
                err.append("Fichier PDF invalide.<br>");
            } else {
                String newN = UUID.randomUUID().toString() + ".pdf";
                String upPath = this.paperUploadBasePath + File.separator + newN;
                try (InputStream fic = fp.getInputStream()) {
                    Files.copy(fic, Paths.get(upPath), StandardCopyOption.REPLACE_EXISTING);
                    newPaperFn = newN;
                    delOld = oldPath != null && !oldPath.equals(newPaperFn);
                    System.out.println("Nouveau PDF: " + upPath);
                } catch (IOException ex) {
                    err.append("Erreur lors de l'upload du nouveau PDF.<br>");
                    newPaperFn = oldPath;
                    delOld = false;
                    ex.printStackTrace();
                }
            }
        }

        if (err.length() > 0) {
            request.setAttribute("submissionError", err.toString());
            request.setAttribute("conferenceIdValue", confIdStr);
            request.setAttribute("titleValue", title);
            request.setAttribute("abstractTextValue", abstractText);
            request.setAttribute("keywordsValue", keywords);
            request.setAttribute("coAuthorEmailsValue", coAuthorEmailsStr);
            request.setAttribute("correspondingAuthorEmailValue", correspEmail);
            request.setAttribute("isEditMode", true);
            request.setAttribute("submissionId", submissionId);
            showEditForm(request, response, currentUser);
            return;
        }

        exSub.setTitle(title.trim());
        exSub.setAbstractText(abstractText.trim());
        exSub.setKeywords(keywords.trim());
        exSub.setFilePath(newPaperFn);

        try {
            submissionDAO.updateSubmissionDetails(exSub);
            submissionAuthorDAO.removeAllAuthorsFromSubmission(submissionId);

            int order = 1;
            for (User author : authors) {
                SubmissionAuthor sa = new SubmissionAuthor(submissionId, author.getUserId(),
                    (author.getUserId() == correspAuthor.getUserId()), order++);
                submissionAuthorDAO.addAuthorToSubmission(sa);
                if (!userConferenceRoleDAO.userHasRole(author.getUserId(), confId, Role.AUTHOR)) {
                    userConferenceRoleDAO.assignRoleToUser(author.getUserId(), confId, Role.AUTHOR);
                }
            }

            if (delOld && oldPath != null) {
                try {
                    Files.deleteIfExists(Paths.get(this.paperUploadBasePath + File.separator + oldPath));
                } catch (IOException ioEx) {
                    System.err.println("Erreur suppression ancien PDF: " + ioEx.getMessage());
                }
            }

            request.getSession().setAttribute("successMessage", "Article '" + fn(exSub.getTitle()) + "' mis à jour !");
            response.sendRedirect(request.getContextPath() + "/submission/list?confId=" + confId);

        } catch (SQLException e) {
            if (newPaperFn != null && !newPaperFn.equals(oldPath)) {
                try {
                    Files.deleteIfExists(Paths.get(this.paperUploadBasePath + File.separator + newPaperFn));
                } catch (IOException ioEx) {
                    ioEx.printStackTrace();
                }
            }
            request.setAttribute("submissionError", "Erreur lors de la mise à jour : " + e.getMessage());
            request.setAttribute("conferenceIdValue", confIdStr);
            request.setAttribute("titleValue", title);
            request.setAttribute("abstractTextValue", abstractText);
            request.setAttribute("keywordsValue", keywords);
            request.setAttribute("coAuthorEmailsValue", coAuthorEmailsStr);
            request.setAttribute("correspondingAuthorEmailValue", correspEmail);
            request.setAttribute("isEditMode", true);
            request.setAttribute("submissionId", submissionId);
            showEditForm(request, response, currentUser);}}
        

    
    
    
    
    
    
    
    
    
    
    
    // ======================== Méthodes Utilitaires (Implémentées) =======================

    /** Récupère et valide l'ID de conférence ('confId'). */
    private int getConferenceIdFromRequest(HttpServletRequest request) throws InvalidIdException {
        String confIdParam = request.getParameter("confId"); if (confIdParam == null || confIdParam.trim().isEmpty()) { throw new InvalidIdException("Paramètre 'confId' manquant."); } try { int confId = Integer.parseInt(confIdParam); if (confId <= 0) { throw new InvalidIdException("ID conf invalide: " + confIdParam); } return confId; } catch (NumberFormatException e) { throw new InvalidIdException("Format ID conf invalide: " + confIdParam); }
    }
    /** Récupère un paramètre entier positif. */
    private int getIntParamFromRequest(HttpServletRequest request, String paramName) throws InvalidIdException {
        String paramValue = request.getParameter(paramName); if (paramValue == null || paramValue.trim().isEmpty()) { throw new InvalidIdException("Paramètre '" + paramName + "' manquant."); } try { int id = Integer.parseInt(paramValue); if (id <= 0) { throw new InvalidIdException("ID invalide pour '" + paramName + "'."); } return id; } catch (NumberFormatException e) { throw new InvalidIdException("Format invalide pour '" + paramName + "'."); }
    }
    /** Récupère un paramètre entier positif sans lancer d'exception (retourne 0 si invalide). */
    private int getIntParamFromRequestQuietly(HttpServletRequest request, String paramName) {
        String paramValue = request.getParameter(paramName); if (paramValue != null) { try { int id = Integer.parseInt(paramValue); if (id > 0) return id; } catch (NumberFormatException e) {} } return 0;
    }
    /** Gère les erreurs génériques. */
    private void handleGenericError(HttpServletRequest req, HttpServletResponse res, Exception e, String ctx) throws IOException, ServletException {
        System.err.println("ERREUR SERVEUR DANS SUBMISSION SERVLET - Contexte: " + ctx); e.printStackTrace(); if (!res.isCommitted()) { res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne serveur."); } else { System.err.println("Réponse déjà engagée, impossible d'envoyer erreur 500."); }
    }
    /** Échappe les caractères HTML. */
    private String fn(String input) {
        if (input == null) return ""; return input.replace("&", "&").replace("<", "<").replace(">", ">").replace("\"", "").replace("'", "'").replace("/", "/");
    }

    // ======================== Exceptions Internes =======================
    private static class InvalidIdException extends Exception { public InvalidIdException(String message) { super(message); } }
    private static class InvalidSubmissionOrPcIdException extends Exception { public InvalidSubmissionOrPcIdException(String msg) { super(msg); }  // Gardée pour compatibilité
}
}// Fin Servlet
