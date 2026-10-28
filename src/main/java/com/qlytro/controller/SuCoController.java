package com.qlytro.controller;

import com.qlytro.dto.request.SuCoRequest;
import com.qlytro.entity.ThietBi;
import com.qlytro.entity.enums.NguyenNhanSuCo;
import com.qlytro.security.CustomUserDetails;
import com.qlytro.service.impl.PhongService;
import com.qlytro.service.impl.SuCoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
public class SuCoController {

    private final SuCoService suCoService;
    private final PhongService phongService;

    public SuCoController(SuCoService suCoService,
                          PhongService phongService) {
        this.suCoService = suCoService;
        this.phongService = phongService;
    }

    // ===== LANDLORD =====
    @GetMapping("/landlord/su-co")
    public String landlordList(Model model) {
        model.addAttribute("danhSach", suCoService.findAll());
        model.addAttribute("pageTitle", "Quản lý Sự cố");
        model.addAttribute("pageContent", "landlord/su-co/list");
        return "layout/base";
    }

    @GetMapping("/landlord/su-co/{id}")
    public String landlordDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("suCo", suCoService.findById(id));
        model.addAttribute("nguyenNhanList", NguyenNhanSuCo.values());
        model.addAttribute("pageTitle", "Chi tiết Sự cố");
        model.addAttribute("pageContent", "landlord/su-co/detail");
        return "layout/base";
    }

    @GetMapping("/landlord/su-co/them")
    public String landlordThemForm(@RequestParam(required = false) Integer phongId, Model model) {
        SuCoRequest req = new SuCoRequest();
        req.setPhongId(phongId);
        List<ThietBi> danhSachThietBi = (phongId == null)
                ? List.<ThietBi>of()
                : suCoService.thietBiCuaPhong(phongId);
        model.addAttribute("request", req);
        model.addAttribute("danhSachPhong", phongService.findAll());
        model.addAttribute("danhSachThietBi", danhSachThietBi);
        model.addAttribute("pageTitle", "Ghi nhận Sự cố");
        model.addAttribute("pageContent", "landlord/su-co/form");
        return "layout/base";
    }

    @PostMapping("/landlord/su-co/them")
    public String landlordThem(@Valid @ModelAttribute("request") SuCoRequest request,
                                BindingResult result, Model model,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            List<ThietBi> danhSachThietBi = (request.getPhongId() == null)
                    ? List.<ThietBi>of()
                    : suCoService.thietBiCuaPhong(request.getPhongId());
            model.addAttribute("danhSachPhong", phongService.findAll());
            model.addAttribute("danhSachThietBi", danhSachThietBi);
            model.addAttribute("pageTitle", "Ghi nhận Sự cố");
            model.addAttribute("pageContent", "landlord/su-co/form");
            return "layout/base";
        }
        var sc = suCoService.taoSuCoByLandlord(request, userDetails.getId());
        redirectAttributes.addFlashAttribute("successMsg", "Ghi nhận sự cố thành công!");
        return "redirect:/landlord/su-co/" + sc.getId();
    }

    @PostMapping("/landlord/su-co/{id}/cap-nhat")
    public String capNhatTrangThai(@PathVariable Integer id,
                                   @RequestParam Integer trangThai,
                                   RedirectAttributes redirectAttributes) {
        suCoService.capNhatTrangThai(id, trangThai);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật trạng thái sự cố thành công!");
        return "redirect:/landlord/su-co/" + id;
    }

    @PostMapping("/landlord/su-co/{id}/hoan-thanh")
    public String hoanThanh(@PathVariable Integer id,
                             @RequestParam Integer nguyenNhan,
                             RedirectAttributes redirectAttributes) {
        suCoService.hoanThanh(id, nguyenNhan);
        redirectAttributes.addFlashAttribute("successMsg", "Hoàn thành sự cố!");
        if (nguyenNhan != null && nguyenNhan == 1) { // DO_NGUOI_O_LAM_HONG
            return "redirect:/landlord/boi-thuong/them?suCoId=" + id;
        }
        return "redirect:/landlord/su-co/" + id;
    }

    // ===== TENANT =====
    @GetMapping("/tenant/su-co")
    public String tenantList(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // Filter su co chi hien thi nhung su co do user nay bao
        model.addAttribute("danhSach", suCoService.findByNguoiBao(userDetails.getId()));
        model.addAttribute("pageTitle", "Sự cố của tôi");
        model.addAttribute("pageContent", "tenant/su-co/list");
        return "layout/base";
    }

    @GetMapping("/tenant/su-co/bao")
    public String tenantBaoForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("request", new SuCoRequest());
        model.addAttribute("danhSachThietBi", suCoService.thietBiCuaPhongHienTai(userDetails.getId()));
        model.addAttribute("pageTitle", "Báo sự cố");
        model.addAttribute("pageContent", "tenant/su-co/form");
        return "layout/base";
    }

    @PostMapping("/tenant/su-co/bao")
    public String tenantBao(@Valid @ModelAttribute("request") SuCoRequest request,
                             BindingResult result, Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("danhSachThietBi", suCoService.thietBiCuaPhongHienTai(userDetails.getId()));
            model.addAttribute("pageTitle", "Báo sự cố");
            model.addAttribute("pageContent", "tenant/su-co/form");
            return "layout/base";
        }
        suCoService.taoSuCoByTenant(request, userDetails.getId());
        redirectAttributes.addFlashAttribute("successMsg", "Báo sự cố thành công! Chủ trọ sẽ xử lý sớm.");
        return "redirect:/tenant/su-co";
    }
}
