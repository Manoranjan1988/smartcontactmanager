package com.smartcontact.exception;

import java.nio.file.AccessDeniedException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailSendException.class)
    public String EmailSendException(EmailSendException ex, RedirectAttributes ra) {
        log.error("Email send Exception: ", ex);
        ra.addFlashAttribute("msg", "Mail Server Down! Try After Some Time");
        ra.addFlashAttribute("type", "error");
        return "redirect:/public/signup";
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(
            MaxUploadSizeExceededException mee,
            RedirectAttributes ra,
            HttpServletRequest request) {

        log.error("Max file upload exception: {} ", mee.getMessage());
        
        ra.addFlashAttribute("msg", "Max 5MB allowed. Please re-fill the form with a smaller image.");
        ra.addFlashAttribute("type", "error");
       
        String uri = request.getRequestURI();
        if(uri.contains("processUpdate")){
            ra.addFlashAttribute("isUpdate", true);
        }

        log.info("URI is: {}",uri);
        Map<String, String> redirectMap = Map.of(
                "processContact", "/user/add-contact",
                "processUpdate", "/user/add-contact",
                "update-profile", "/user/profile_settings",
                "send-email-blast", "/user/email_blast");
        for (Map.Entry<String, String> entry : redirectMap.entrySet()) {
            if (uri.contains(entry.getKey())) {
                log.info("URI value: {}",entry.getValue());  
                return "redirect:"+entry.getValue();
              
            }
        }
        return "redirect:/user/dashboard_home";
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex, RedirectAttributes ra) {

        String parameterName = ex.getName();
        if (parameterName.equals("currentPage")) {
            ra.addFlashAttribute("msg", "Invalid Page Number! Redirecting to home.");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/show-contacts?currentPage=0";
        }
        if (parameterName.equals("cid")) {
            ra.addFlashAttribute("msg", "Unauthorized Access");
            ra.addFlashAttribute("type", "error");
            return "redirect:/user/show-contacts?currentPage=0";
        }

        return "redirect:/user/dashboard_home";
    }

    @ExceptionHandler(ExportException.class)
    public String handleExcelDownload(ExportException ce, RedirectAttributes ra) {
        log.error("Error in creating excel: " + ce.getMessage());

        ra.addFlashAttribute("msg", "Download Failed! Please try again later.");
        ra.addFlashAttribute("type", "error");
        return "redirect:/user/dashboard_home";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handle403(AccessDeniedException ex, Model model) {
        model.addAttribute("msg", "Forbidden Access 403");
        model.addAttribute("type", "warning");
        log.error("Access Denied Exception: ", ex);
        return "error/403";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleResponseStatus(IllegalStateException ex, Model model) {
        model.addAttribute("msg", "Invalid Data Provided (400)!");
        model.addAttribute("type", "warning");
        log.error("Illigial State Exception: ", ex);
        return "error/400";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handle404(NoResourceFoundException ex, Model model) {
        model.addAttribute("msg", "Resource Not Found 404 !");
        model.addAttribute("type", "warning");
        log.error("Resource not found 404: ", ex);
        return "error/404";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handle500(RuntimeException ex, Model model) {
        model.addAttribute("msg", "Internal Server Error 500!");
        model.addAttribute("type", "warning");
        log.error("Runtime Exception: {}", ex);
        ex.printStackTrace();
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, Model model) {
        model.addAttribute("msg", "Something Error !");
        model.addAttribute("type", "error");
        log.info("Genric Error..............");
        log.error("Genric Error Details: ", ex);
        ex.printStackTrace();
        return "error/generic_error";
    }

}
