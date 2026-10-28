package com.qlytro.controller;

import com.qlytro.exception.BusinessException;
import com.qlytro.security.CustomUserDetails;
import com.qlytro.service.impl.BoiThuongThietHaiService;
import com.qlytro.service.impl.FileStorageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tenant/boi-thuong")
public class TenantBoiThuongController {

    private final BoiThuongThietHaiService boiThuongService;
    private final FileStorageService fileStorageService;

    public TenantBoiThuongController(BoiThuongThietHaiService boiThuongService,
                                     FileStorageService fileStorageService) {
        this.boiThuongService = boiThuongService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("danhSach", boiThuongService.findByNguoiDungId(userDetails.getId()));
        model.addAttribute("pageTitle", "Bồi thường của tôi");
        model.addAttribute("pageContent", "tenant/boi-thuong/list");
        return "layout/base";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model) {
        model.addAttribute("boiThuong", boiThuongService.findByIdOfNguoiDung(id, userDetails.getId()));
        model.addAttribute("pageTitle", "Chi tiết Bồi thường");
        model.addAttribute("pageContent", "tenant/boi-thuong/detail");
        return "layout/base";
    }

    @PostMapping("/{id}/nop-tien")
    public String nopTien(@PathVariable Integer id,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            // 1. Kiểm tra nghiệp vụ trước
            boiThuongService.validateKhachNopTien(id, userDetails.getId());

            // 2. Validate và lưu file
            String anhChuyenKhoan = null;
            if (file != null && !file.isEmpty()) {
                anhChuyenKhoan = fileStorageService.storeFile(file);
            }

            // 3. Thực hiện lưu vào CSDL, nếu lỗi thì xóa file đã ghi
            try {
                boiThuongService.khachNopTien(id, anhChuyenKhoan, userDetails.getId());
            } catch (Exception e) {
                if (anhChuyenKhoan != null) {
                    fileStorageService.deleteFile(anhChuyenKhoan);
                }
                throw e;
            }

            redirectAttributes.addFlashAttribute("successMsg",
                    "Đã gửi báo cáo thanh toán bồi thường. Chủ trọ sẽ kiểm tra và xác nhận sớm nhất!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/tenant/boi-thuong/" + id;
    }
}
