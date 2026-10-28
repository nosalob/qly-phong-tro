package com.qlytro.security;

import com.qlytro.entity.NguoiDung;
import com.qlytro.repository.NguoiDungRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor bat buoc nguoi dung phai doi mat khau neu tai khoan
 * co co must_change_password = true.
 */
@Component
public class ForceChangePasswordInterceptor implements HandlerInterceptor {

    private final NguoiDungRepository nguoiDungRepository;

    public ForceChangePasswordInterceptor(NguoiDungRepository nguoiDungRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            String uri = request.getRequestURI();

            // Cho phep truy cap cac trang xu ly doi mat khau, dang xuat va tai nguyen tinh
            if (uri.startsWith("/doi-mat-khau") || uri.startsWith("/logout")
                    || uri.startsWith("/css/") || uri.startsWith("/js/")
                    || uri.startsWith("/images/") || uri.startsWith("/uploads/")) {
                return true;
            }

            // Kiem tra trang thai moi nhat tu DB
            NguoiDung nd = nguoiDungRepository.findById(userDetails.getId()).orElse(null);
            if (nd != null && Boolean.TRUE.equals(nd.getMustChangePassword())) {
                response.sendRedirect("/doi-mat-khau?required=true");
                return false;
            }
        }
        return true;
    }
}
