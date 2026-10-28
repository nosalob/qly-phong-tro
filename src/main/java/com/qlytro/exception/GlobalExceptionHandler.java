package com.qlytro.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler cho Thymeleaf MVC.
 * Bat cac exception nghiep vu va chuyen sang trang loi dep.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Không tìm thấy");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model) {
        log.warn("Business error: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Lỗi nghiệp vụ");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/business";
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public String handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex, Model model) {
        model.addAttribute("errorTitle", "Không tìm thấy");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public String handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex, Model model) {
        log.error("Data integrity violation: ", ex);
        model.addAttribute("errorTitle", "Ràng buộc dữ liệu");
        model.addAttribute("errorMessage", "Thao tác không thể thực hiện do ràng buộc dữ liệu hoặc đang có dữ liệu liên quan tham chiếu.");
        return "error/business";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Unhandled exception", ex);
        model.addAttribute("errorTitle", "Lỗi hệ thống");
        model.addAttribute("errorMessage", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
        return "error/500";
    }
}
