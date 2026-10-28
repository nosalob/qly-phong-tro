package com.qlytro.controller;

import com.qlytro.exception.BusinessException;
import com.qlytro.security.CustomUserDetails;
import com.qlytro.service.impl.FileStorageService;
import com.qlytro.service.impl.HoaDonService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tenant/hoa-don")
public class TenantHoaDonController {

    private final HoaDonService hoaDonService;
    private final FileStorageService fileStorageService;

    public TenantHoaDonController(HoaDonService hoaDonService,
                                  FileStorageService fileStorageService) {
        this.hoaDonService = hoaDonService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // Filter hoa don theo nguoi dung dang dang nhap
        model.addAttribute("danhSach", hoaDonService.findByNguoiDungId(userDetails.getId()));
        model.addAttribute("pageTitle", "Hóa đơn của tôi");
        model.addAttribute("pageContent", "tenant/hoa-don/list");
        return "layout/base";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model) {
        model.addAttribute("hoaDon", hoaDonService.findByIdOfNguoiDung(id, userDetails.getId()));
        model.addAttribute("pageTitle", "Chi tiết Hóa đơn");
        model.addAttribute("pageContent", "tenant/hoa-don/detail");
        return "layout/base";
    }

    /**
     * Khach thue nop tien kem anh bien lai chuyen khoan -> chuyen trang thai sang CHO_DUYET.
     * Chu tro se xac nhan hoac tu choi o trang landlord/hoa-don/{id}.
     */
    @PostMapping("/{id}/nop-tien")
    public String nopTien(@PathVariable Integer id,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Vui lòng chọn ảnh biên lai chuyển khoản trước khi bấm nộp tiền.");
            return "redirect:/tenant/hoa-don/" + id;
        }

        try {
            // 1. Kiểm tra nghiệp vụ trước
            hoaDonService.validateKhachNopTien(id, userDetails.getId());

            // 2. Validate và lưu file
            String anhChuyenKhoan = fileStorageService.storeFile(file);

            // 3. Thực hiện lưu vào CSDL, nếu lỗi thì xóa file đã ghi
            try {
                hoaDonService.khachNopTien(id, anhChuyenKhoan, userDetails.getId());
            } catch (Exception e) {
                fileStorageService.deleteFile(anhChuyenKhoan);
                throw e;
            }

            redirectAttributes.addFlashAttribute("successMsg",
                    "Đã tải ảnh biên lai và gửi thông báo thanh toán thành công! Chủ trọ sẽ duyệt sớm nhất.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/tenant/hoa-don/" + id;
    }
}
