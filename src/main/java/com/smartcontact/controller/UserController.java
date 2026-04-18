package com.smartcontact.controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartcontact.dto.ContactFormDTO;
import com.smartcontact.entities.Contact;
import com.smartcontact.entities.User;
import com.smartcontact.exception.DuplicateContactException;
import com.smartcontact.service.ContactService;
import com.smartcontact.service.EmailService;
import com.smartcontact.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/user")

public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private EmailService emailService;

    @ModelAttribute
    public void commonData(Model model, Principal principal, Authentication authentication) {

        if (principal == null) {
            return;
        }
        String email = principal.getName();
        User user = userService.getUserByEmail(email);
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("contactCount", contactService.getContactCount(user));
            model.addAttribute("favCount", contactService.favoriteCount(user));
            model.addAttribute("profileStrength", contactService.getProfileStrength(user));
        }
    }

    @GetMapping("/dashboard_home")
    public String dashboard(Model model, HttpSession session, HttpServletRequest request, Principal principal) {

        String email = principal.getName();
        log.info("Welcome to dashboard: Mr. {} ", email);
        String sMsg = (String) session.getAttribute("session_msg");
        String sType = (String) session.getAttribute("session_type");
        if (sMsg != null) {
            model.addAttribute("msg", sMsg);
            model.addAttribute("type", sType);
            session.removeAttribute("session_msg");
            session.removeAttribute("session_type");
        }

        model.addAttribute("title", "Dashboard | SCM");
        return "user/dashboard_home";

    }

    @GetMapping("/add-contact")
    public String addContact(Model model) {
        model.addAttribute("title", "Add Contact | SCM");
        model.addAttribute("contactFormDTO", new ContactFormDTO());
        model.addAttribute("isUpdate", false);
        return "user/add-contact";
    }

    @PostMapping("/processContact")
    public String processContact(
            @Valid @ModelAttribute("contactFormDTO") ContactFormDTO contactFormDTO,
            BindingResult br, Model model, RedirectAttributes ra, Principal principal,
            HttpSession session, @RequestParam("contactimage") MultipartFile file) {

        if (br.hasErrors()) {
            String allErrors = br.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining("<br>"));

            model.addAttribute("msg", allErrors);
            model.addAttribute("type", "error");
            model.addAttribute("isUpdate", false);
            return "user/add-contact";
        }

        if (!file.isEmpty()) {
            String filename = file.getOriginalFilename();
            if (filename == null ||
                    !(filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))) {

                model.addAttribute("msg", "Only JPG, JPEG & PNG files are allowed!");
                model.addAttribute("type", "error");
                model.addAttribute("isUpdate", false);
                return "user/add-contact";
            }
        }
        try {
            contactService.saveContact(contactFormDTO, file, principal);
            ra.addFlashAttribute("msg", "Contact successfully added!");
            ra.addFlashAttribute("type", "success");
        } catch (DuplicateContactException dc) {
            model.addAttribute("msg", dc.getMessage());
            model.addAttribute("type", "error");
            model.addAttribute("isUpdate", false);
            return "user/add-contact";
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Duplicate entry") || msg.contains("1062")
                    || msg.contains("ConstraintViolation"))) {
                log.error("Error in process contact:", msg);
                model.addAttribute("msg", "Contact successfully added!");
                model.addAttribute("type", "success");
                model.addAttribute("isUpdate", false);
                return "user/add-contact";
            }
            log.error("Exception in Process Contact: ", e);
            model.addAttribute("msg", "Something went Wrong");
            model.addAttribute("type", "error");
            model.addAttribute("isUpdate", false);
            return "user/add-contact";
        }

        return "redirect:/user/add-contact";

    }

    @GetMapping("/show-contacts")
    public String showContactsHandler(@RequestParam(defaultValue = "0") int currentPage,
            @RequestParam(value = "key", required = false) String key,
            Model model, Principal principal, RedirectAttributes ra) {

        model.addAttribute("title", "Show Contact | SCM");
        if (currentPage < 0) {
            currentPage = 0;
        }
        try {
            Page<Contact> contacts = contactService.getAllContacts(principal, currentPage, key);

            if (currentPage >= contacts.getTotalPages() && contacts.getTotalPages() > 0) {
                currentPage = contacts.getTotalPages() - 1;
                contacts = contactService.getAllContacts(principal, currentPage, key);
            }
            model.addAttribute("contacts", contacts);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("totalPages", contacts.getTotalPages());
            model.addAttribute("searchkey", key);
        } catch (Exception e) {
            ra.addFlashAttribute("msg", "Invalid Page! Redirecting to Home.");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/show-contacts?currentPage=0";
        }
        return "user/show-contacts";
    }

    @GetMapping("/toggle-favorite/{cid}")
    public String toggleFavorite(@PathVariable("cid") Long cid, Principal principal, RedirectAttributes ra) {

        try {
            contactService.toggleFavorite(cid, principal);
            ra.addFlashAttribute("msg", "Favorite Status Updated!");
            ra.addFlashAttribute("type", "success");
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("msg", "Something went wrong!");
            ra.addFlashAttribute("type", "error");
        }

        return "redirect:/user/show-contacts";

    }

    @GetMapping("delete/{cid}")
    public String deleteContactHandler(
            @PathVariable Long cid,
            Principal principal,
            RedirectAttributes ra) {

        User user = userService.getUserByEmail(principal.getName());
        Contact contact = contactService.getContactById(cid);

        if (!contact.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("msg", "Unauthorized Access");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/show-contacts";
        }

        contactService.deleteContact(contact);
        ra.addFlashAttribute("msg", "Contact Deleted Successfully");
        ra.addFlashAttribute("type", "success");

        return "redirect:/user/show-contacts";
    }

    @GetMapping("/update-contact/{cid}")
    public String updateContactHandler(@PathVariable("cid") Long cid, Principal principal, Model model) {

        model.addAttribute("title", "Update Contact | SCM");

        Contact contact = this.contactService.getContactById(cid);
        User user = this.userService.getUserByEmail(principal.getName());
        if (!contact.getUser().getId().equals(user.getId())) {
            return "redirect:/user/show-contacts";
        }

        ContactFormDTO dto = new ContactFormDTO();
        dto.setFirstName(contact.getFirstName());
        dto.setLastName(contact.getLastName());
        dto.setEmail(contact.getEmail());
        dto.setPhone(contact.getPhone());
        dto.setWork(contact.getWork());
        dto.setDescription(contact.getDescription());
        dto.setFavorite(contact.isFavorite());

        model.addAttribute("contactFormDTO", dto);
        model.addAttribute("cid", cid);
        model.addAttribute("isUpdate", true);
        return "user/add-contact";
    }

    @PostMapping("/processUpdate")
    public String updateContacthandler(
            @RequestParam("cid") Long cid,
            @Valid @ModelAttribute("contactFormDTO") ContactFormDTO contactFormDTO,
            BindingResult br, Model model, RedirectAttributes ra, Principal principal,
            @RequestParam("contactimage") MultipartFile file) {

        User user = userService.getUserByEmail(principal.getName());
        Contact contactId = contactService.getContactById(cid);
        if (br.hasErrors()) {
            String allErrors = br.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining("<br>"));

            model.addAttribute("msg", allErrors);
            model.addAttribute("type", "error");
            model.addAttribute("isUpdate", true);
            model.addAttribute("cid", cid);
            model.addAttribute("contactFormDTO", contactFormDTO);
            return "/user/add-contact";
        }

        if (!file.isEmpty()) {
            String filename = file.getOriginalFilename().toLowerCase();

            if (!(filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))) {
                model.addAttribute("msg", "Only JPG, JPEG & PNG files are allowed!");
                model.addAttribute("type", "error");
                model.addAttribute("cid", cid);
                model.addAttribute("contactFormDTO", contactFormDTO);
                return "user/add-contact";
            }
        }

        if (!contactId.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("msg", "Unauthorized Access");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/show-contacts";
        }
        try

        {
            contactService.updateContact(cid, contactFormDTO, file, principal);
            ra.addFlashAttribute("msg", "Contact Update Sucessfully");
            ra.addFlashAttribute("type", "success");
            return "redirect:/user/show-contacts";
        } catch (DuplicateContactException de) {
            log.error("Error Msg: {} ", de.getMessage());
            model.addAttribute("msg", de.getMessage());
            model.addAttribute("type", "error");
            model.addAttribute("isUpdate", true);
            model.addAttribute("cid", cid);
            model.addAttribute("contactFormDTO", contactFormDTO);
            return "user/add-contact";
        }

        catch (Exception e) {
            log.error("Exception in Update Contact", e);
            model.addAttribute("msg", "Something went Wrong");
            model.addAttribute("type", "error");
            model.addAttribute("isUpdate", true);
            model.addAttribute("cid", cid);
            model.addAttribute("contactFormDTO", contactFormDTO);
            return "user/add-contact";
        }

    }

    @GetMapping("/profile")
    public String profileHandler(Model model) {
        model.addAttribute("title", "Profile | SCM");
        return "user/profile";
    }

    @GetMapping("/profile_settings")
    public String profileEditHandler(Model model) {
        model.addAttribute("title", "Profile Edit | SCM");
        return "user/profile_settings";
    }

    @PostMapping("/update-profile")
    public String proUpdateHandler(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("about") String about,
            Principal principal,
            Model model,
            RedirectAttributes ra,
            @RequestParam("profileImage") MultipartFile file) {

        if (name == null || name.trim().length() < 3 || name.trim().length() > 100) {

            model.addAttribute("msg", "Name must be between 3 and 100 characters !!");
            model.addAttribute("type", "error");
            return "user/profile_settings";
        }
        User userProfile = userService.getUserByEmail(principal.getName());
        if (!userProfile.getEmail().equals(email)) {
            model.addAttribute("msg", "Don't Try to be Smart");
            model.addAttribute("type", "error");
            return "user/profile_settings";
        }

        if (!file.isEmpty()) {
            String contentType = file.getContentType();
            if (!(contentType.equals("image/jpeg") ||
                    contentType.equals("image/png") ||
                    contentType.equals("image/jpg"))) {

                model.addAttribute("msg", "Only JPG, JPEG & PNG files are allowed!");
                model.addAttribute("type", "error");
                return "user/profile_settings";
            }
        }
        try {
            contactService.updateProfile(userProfile, name, file, about);
            ra.addFlashAttribute("msg", "Profile Updated Successfully!");
            ra.addFlashAttribute("type", "success");
            return "redirect:/user/profile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("msg", "Something Went Wrong");
            model.addAttribute("type", "error");
            return "user/profile_settings";
        }

    }

    // Open Password Rest Form
    @GetMapping("/reset_password")
    public String resetPasswordHandler(
            @RequestParam(value = "clear", required = false) String clear,
            Model model, Principal principal, HttpSession session, RedirectAttributes ra) {

        User user = userService.getUserByEmail(principal.getName());

        if (!"SELF".equals(user.getProvided())) {
            ra.addFlashAttribute("msg", "Password management is handled by your social provider.");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/dashboard_home";
        }

        if ("true".equals(clear)) {
            session.removeAttribute("otpSent");
            session.removeAttribute("resetEmail");
        }
        Boolean otpSent = (Boolean) session.getAttribute("otpSent");
        String email = (String) session.getAttribute("resetEmail");

        if (otpSent != null && otpSent) {
            System.out.println("Restoring state from session for: " + email);
            model.addAttribute("otpSent", true);
            model.addAttribute("email", email);
        } else {
            model.addAttribute("otpSent", false);
            model.addAttribute("email", principal.getName());
        }

        model.addAttribute("title", "Password : Change");
        return "user/reset_password";
    }

    /* Send OTP handler */
    @PostMapping("/send-otp-reset")
    public String sendOtpHandler(@RequestParam("email") String email, Model model, Principal principal,
            RedirectAttributes ra, HttpSession session) {

        User user = userService.getUserByEmail(principal.getName());

        if (!"SELF".equals(user.getProvided())) {
            ra.addFlashAttribute("msg", "Password management is handled by your social provider.");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/dashboard_home";
        }
        if (!user.getEmail().equals(email) || user.getStatus().equals("inactive")) {
            ra.addFlashAttribute("msg", "Account is Not Valid");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/reset_password";

        } else {
            try {
                String otp = userService.checkLimitAndGenerateOtp(email);
                emailService.otpEmailVerification(email, otp);
                System.out.println("Generated OTP: " + otp);
                userService.finalizeAndSaveOtp(email, otp);

                session.setAttribute("otpSent", true);
                session.setAttribute("resetEmail", email);

                ra.addFlashAttribute("otpSent", true);
                ra.addFlashAttribute("email", email);
                ra.addFlashAttribute("msg", "OTP sent! (Attempt: " + email + ")");
                ra.addFlashAttribute("type", "success");

            } catch (Exception e) {
                session.removeAttribute("otpSent");
                session.removeAttribute("resetEmail");
                if ("MAX_LIMIT_REACHED".equals(e.getMessage())) {
                    ra.addFlashAttribute("msg", "Max limit reached! Please try after 5 minutes.");
                } else {
                    ra.addFlashAttribute("msg", "Mail Server Down! Try after some time.");

                }

            }
            return "redirect:/user/reset_password";

        }
    }

    // Password Reset after OTP Sent

    @PostMapping("/do-password-reset")
    public String chnagePasswordHandler(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("cnfpassword") String cnfpassword,
            @RequestParam("otp") String otp,
            Model model, RedirectAttributes ra, Principal principal, HttpSession session) {

        Boolean otpSent = (Boolean) session.getAttribute("otpSent");
        String emailReset = (String) session.getAttribute("resetEmail");

        User user = userService.getUserByEmail(principal.getName());

        if (!"SELF".equals(user.getProvided())) {
            ra.addFlashAttribute("msg", "Change Your Password from GOOGLE");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/dashboard_home";
        }
        if (!user.getEmail().equals(email) || user.getStatus().equals("inactive")) {
            ra.addFlashAttribute("msg", "Account is Not Valid");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/reset_password";

        }

        String passRegex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,500}$";

        if (!password.equals(cnfpassword) && otpSent != null && otpSent) {
            ra.addFlashAttribute("msg", "Passwords do not match!");
            ra.addFlashAttribute("type", "error");
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("email", emailReset);
            return "redirect:/user/reset_password";
        } else if (!password.matches(passRegex) && otpSent != null && otpSent) {
            ra.addFlashAttribute("msg", "Password must be 8+ chars with Upper, Lower, Number & Special char!");
            ra.addFlashAttribute("type", "error");
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("email", emailReset);
            return "redirect:/user/reset_password";
        }

        boolean isValid = userService.verifyOtp(email, otp);
        System.out.println("OTP Status: " + isValid);
        if (isValid) {
            userService.updatePassword(email, password);
            ra.addFlashAttribute("msg", "Password updated successfully! Please login.");
            ra.addFlashAttribute("type", "success");
            session.removeAttribute("otpSent");
            session.removeAttribute("resetEmail");
            return "redirect:/user/do-logout?status=password_updated";
        } else {
            ra.addFlashAttribute("msg", "Invalid or Expired OTP!");
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("email", email);
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/reset_password";
        }
    }

    @GetMapping("/export-contacts")
    public void exportContactsHandler(@RequestParam("format") String format,
            Principal principal, HttpServletResponse response, RedirectAttributes ra) {

        User user = userService.getUserByEmail(principal.getName());
        List<Contact> contacts = contactService.findByUser(user.getEmail());

        if (format.equals("excel")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            response.setHeader("Content-Disposition", "attachment; filename=Contacts.xlsx");
            contactService.exportToExcel(contacts, response);
        } else {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=Contacts.pdf");
            contactService.exportToPdf(contacts, response, user);
        }

    }

    @GetMapping("/email_blast")
    public String emailBlastHandler(Principal principal, Model model) {

        User user = userService.getUserByEmail(principal.getName());

        Long nofcontacts = contactService.noOfAllContacts(user.getEmail());
        model.addAttribute("totalContacts", nofcontacts);
        model.addAttribute("title", "Send Bulk Email | SCM");
        return "/user/email_blast";
    }

    @PostMapping("/send-email-blast")
    public String getMethodName(
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            @RequestParam(value = "attachment", required = false) MultipartFile file,
            Principal principal, RedirectAttributes ra) {

        try {
            contactService.processEmailBlast(principal.getName(), subject, message, file);

            ra.addFlashAttribute("msg", "Blast sequence initiated!");
            ra.addFlashAttribute("type", "success");

        } catch (Exception e) {
            if ("BLAST_ALREADY_IN_PROGRESS".equals(e.getMessage())) {
                ra.addFlashAttribute("msg", "Please wait! Another blast is already running.");
            } else {
                ra.addFlashAttribute("msg", "Blast Failed: " + e.getMessage());
            }
            ra.addFlashAttribute("type", "error");

        }
        return "redirect:/user/dashboard_home";

    }

    @PostMapping("/delete-google-contacts")
    public String deleteGoogleContacts(Principal principal, RedirectAttributes ra) {

        User user = userService.getUserByEmail(principal.getName());

        int rowsUpdated = contactService.deleteAllGoogleContacts(user.getId());
        log.info("Deleted Contcat : {}", rowsUpdated);

        if (rowsUpdated > 0) {
            ra.addFlashAttribute("msg", "Successfully cleared " + rowsUpdated + " Google contacts");
            ra.addFlashAttribute("type", "success");
        } else {
            ra.addFlashAttribute("msg", "No active Google contacts found");
            ra.addFlashAttribute("type", "error");
        }

        return "redirect:/user/dashboard_home";
    }

}