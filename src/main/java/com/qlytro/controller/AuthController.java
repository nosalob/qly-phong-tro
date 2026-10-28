package com.qlytro.controller;

import com.qlytro.dto.request.DoiMatKhauRequest;
import com.qlytro.exception.BusinessException;
import com.qlytro.security.CustomUserDetails;
import com.qlytro.service.impl.NguoiDungService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final NguoiDungService nguoiDungService;

    public AuthController(NguoiDungService nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            boolean isLandlord = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
            return isLandlord ? "redirect:/landlord/dashboard" : "redirect:/tenant/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            boolean isLandlord = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
            return isLandlord ? "redirect:/landlord/dashboard" : "redirect:/tenant/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/doi-mat-khau")
    public String doiMatKhauForm(@org.springframework.web.bind.annotation.RequestParam(required = false) Boolean required,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new DoiMatKhauRequest());
        }
        boolean isRequired = Boolean.TRUE.equals(required) || (userDetails != null && userDetails.isMustChangePassword());
        model.addAttribute("isRequired", isRequired);
        model.addAttribute("pageTitle", "Đổi mật khẩu");
        model.addAttribute("pageContent", "auth/doi-mat-khau");
        return "layout/base";
    }

    @PostMapping("/doi-mat-khau")
    public String doiMatKhau(@Valid @ModelAttribute("request") DoiMatKhauRequest request,
                             BindingResult result,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isRequired", userDetails != null && userDetails.isMustChangePassword());
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("pageContent", "auth/doi-mat-khau");
            return "layout/base";
        }

        try {
            nguoiDungService.doiMatKhau(userDetails.getId(), request);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Đổi mật khẩu thành công! Bạn có thể tiếp tục sử dụng hệ thống với mật khẩu mới.");

            boolean isLandlord = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
            return isLandlord ? "redirect:/landlord/dashboard" : "redirect:/tenant/dashboard";
        } catch (BusinessException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("isRequired", userDetails != null && userDetails.isMustChangePassword());
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("pageContent", "auth/doi-mat-khau");
            return "layout/base";
        }
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
