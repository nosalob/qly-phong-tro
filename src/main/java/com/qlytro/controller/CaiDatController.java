package com.qlytro.controller;

import com.qlytro.entity.enums.LoaiTinhPhi;
import com.qlytro.service.impl.BangGiaDienNuocService;
import com.qlytro.service.impl.PhiDichVuService;
import com.qlytro.service.impl.ThietBiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
public class CaiDatController {

    private final BangGiaDienNuocService bangGiaService;
    private final PhiDichVuService phiDichVuService;
    private final ThietBiService thietBiService;

    public CaiDatController(BangGiaDienNuocService bangGiaService,
            PhiDichVuService phiDichVuService,
            ThietBiService thietBiService) {
        this.bangGiaService = bangGiaService;
        this.phiDichVuService = phiDichVuService;
        this.thietBiService = thietBiService;
    }

    // ===== BANG GIA =====
    @GetMapping("/landlord/bang-gia")
    public String bangGia(Model model) {
        try {
            model.addAttribute("bangGia", bangGiaService.layBangGia());
        } catch (Exception e) {
            model.addAttribute("bangGia", null);
        }
        model.addAttribute("pageTitle", "Bảng giá Điện / Nước");
        model.addAttribute("pageContent", "landlord/bang-gia");
        return "layout/base";
    }

    @PostMapping("/landlord/bang-gia")
    public String capNhatBangGia(@RequestParam(name = "donGiaDien") BigDecimal donGiaDien,
            @RequestParam(name = "donGiaNuoc") BigDecimal donGiaNuoc,
            RedirectAttributes redirectAttributes) {
        bangGiaService.capNhat(donGiaDien, donGiaNuoc);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật bảng giá thành công!");
        return "redirect:/landlord/bang-gia";
    }

    // ===== PHI DICH VU =====
    @GetMapping("/landlord/phi-dich-vu")
    public String phiDichVu(Model model) {
        model.addAttribute("danhSach", phiDichVuService.findAll());
        model.addAttribute("pageTitle", "Phí dịch vụ");
        model.addAttribute("pageContent", "landlord/phi-dich-vu");
        return "layout/base";
    }

    @PostMapping("/landlord/phi-dich-vu/them")
    public String themPhiDichVu(@RequestParam(name = "tenPhi") String tenPhi,
            @RequestParam(name = "soTien") BigDecimal soTien,
            @RequestParam(name = "loaiTinh", defaultValue = "THEO_PHONG") LoaiTinhPhi loaiTinh,
            RedirectAttributes redirectAttributes) {
        phiDichVuService.create(tenPhi, soTien, loaiTinh);
        redirectAttributes.addFlashAttribute("successMsg", "Thêm phí dịch vụ thành công!");
        return "redirect:/landlord/phi-dich-vu";
    }

    @PostMapping("/landlord/phi-dich-vu/{id}/toggle")
    public String togglePhi(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        var phi = phiDichVuService.toggleTrangThai(id);
        String msg = Boolean.TRUE.equals(phi.getDangApDung()) ? "Đã kích hoạt phí." : "Đã tắt phí.";
        redirectAttributes.addFlashAttribute("successMsg", msg);
        return "redirect:/landlord/phi-dich-vu";
    }

    @PostMapping("/landlord/phi-dich-vu/{id}/xoa")
    public String xoaPhi(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        phiDichVuService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Xóa phí dịch vụ thành công!");
        return "redirect:/landlord/phi-dich-vu";
    }

    // ===== THIET BI =====
    @GetMapping("/landlord/thiet-bi")
    public String thietBi(Model model) {
        model.addAttribute("danhSach", thietBiService.findAll());
        model.addAttribute("pageTitle", "Quản lý Thiết bị");
        model.addAttribute("pageContent", "landlord/thiet-bi");
        return "layout/base";
    }

    @PostMapping("/landlord/thiet-bi/them")
    public String themThietBi(@RequestParam(name = "tenThietBi") String tenThietBi,
            RedirectAttributes redirectAttributes) {
        try {
            thietBiService.create(tenThietBi);
            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    "Thêm thiết bị '" + tenThietBi + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    e.getMessage());
        }

        return "redirect:/landlord/thiet-bi";
    }

    @PostMapping("/landlord/thiet-bi/{id}/xoa")
    public String xoaThietBi(@PathVariable(name = "id") Integer id,
            RedirectAttributes redirectAttributes) {
        try {
            thietBiService.deleteById(id);
            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    "Đã xóa thiết bị.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    e.getMessage());
        }

        return "redirect:/landlord/thiet-bi";
    }
}