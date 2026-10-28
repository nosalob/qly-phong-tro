package com.qlytro.controller;

import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.service.impl.HoaDonService;
import com.qlytro.service.impl.HopDongService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HoaDonController {

    private final HoaDonService hoaDonService;
    private final HopDongService hopDongService;
    private final com.qlytro.service.impl.ToaNhaService toaNhaService;
    private final com.qlytro.service.impl.PhongService phongService;

    public HoaDonController(
            HoaDonService hoaDonService,
            HopDongService hopDongService,
            com.qlytro.service.impl.ToaNhaService toaNhaService,
            com.qlytro.service.impl.PhongService phongService) {
        this.hoaDonService = hoaDonService;
        this.hopDongService = hopDongService;
        this.toaNhaService = toaNhaService;
        this.phongService = phongService;
    }

    // ===== LANDLORD =====
    @GetMapping("/landlord/hoa-don")
    public String landlordList(
            @RequestParam(required = false) Integer toaNhaId,
            @RequestParam(required = false) Integer phongId,
            @RequestParam(required = false) String tuThang,
            @RequestParam(required = false) String denThang,
            @RequestParam(required = false) String trangThai,
            Model model) {

        if (tuThang != null && tuThang.isBlank()) {
            tuThang = null;
        }
        if (denThang != null && denThang.isBlank()) {
            denThang = null;
        }

        TrangThaiHoaDon trangThaiEnum = null;
        if (trangThai != null && !trangThai.isBlank()) {
            try {
                trangThaiEnum = TrangThaiHoaDon.valueOf(trangThai.trim());
            } catch (IllegalArgumentException e) {
                trangThaiEnum = null;
                trangThai = null;
            }
        }

        model.addAttribute("danhSach",
                hoaDonService.findAllFiltered(
                        toaNhaId, phongId, tuThang, denThang, trangThaiEnum));

        model.addAttribute("danhSachHopDong", hopDongService.findAll().stream()
                .filter(hd -> hd.getTrangThai() != null
                        && "ACTIVE".equalsIgnoreCase(hd.getTrangThai().name()))
                .toList());

        model.addAttribute("danhSachToaNha", toaNhaService.findAll());
        model.addAttribute("danhSachPhong", phongService.findAll());

        model.addAttribute("toaNhaId", toaNhaId);
        model.addAttribute("phongId", phongId);
        model.addAttribute("tuThang", tuThang);
        model.addAttribute("denThang", denThang);
        model.addAttribute("trangThai", trangThai);

        model.addAttribute("thangHienTai", java.time.YearMonth.now().toString());
        model.addAttribute("pageTitle", "Quản lý Hóa đơn");
        model.addAttribute("pageContent", "landlord/hoa-don/list");

        return "layout/base";
    }

    @GetMapping("/landlord/hoa-don/{id}")
    public String landlordDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("hoaDon", hoaDonService.findById(id));
        model.addAttribute("pageTitle", "Chi tiết Hóa đơn");
        model.addAttribute("pageContent", "landlord/hoa-don/detail");
        return "layout/base";
    }

    @PostMapping("/landlord/hoa-don/tao")
    public String taoHoaDon(
            @RequestParam(required = false) Integer hopDongId,
            @RequestParam(required = false) Integer phongId,
            @RequestParam String thangNam,
            RedirectAttributes redirectAttributes) {
        try {
            // Nếu không có hopDongId nhưng có phongId thì tìm hợp đồng đang hiệu lực
            if (hopDongId == null && phongId != null) {
                var dsHopDong = hopDongService.findAll();

                for (var hdItem : dsHopDong) {
                    if (hdItem.getPhongId() != null
                            && hdItem.getPhongId().equals(phongId)
                            && hdItem.getTrangThai() != null
                            && "ACTIVE".equalsIgnoreCase(hdItem.getTrangThai().name())) {
                        hopDongId = hdItem.getId();
                        break;
                    }
                }
            }

            if (hopDongId == null) {
                redirectAttributes.addFlashAttribute(
                        "errorMsg",
                        "Không tìm thấy hợp đồng đang hiệu lực cho phòng này!");
                return "redirect:/landlord/hoa-don";
            }

            var hd = hoaDonService.taoHoaDon(hopDongId, thangNam);

            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    "Tạo hóa đơn tháng " + thangNam + " thành công!");

            return "redirect:/landlord/hoa-don/" + hd.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    "Lỗi tạo hóa đơn: " + e.getMessage());

            return phongId != null && hopDongId == null
                    ? "redirect:/landlord/chi-so"
                    : "redirect:/landlord/hoa-don";
        }
    }

    @PostMapping("/landlord/hoa-don/{id}/xac-nhan")
    public String xacNhanThanhToan(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        hoaDonService.xacNhanThanhToan(id);
        redirectAttributes.addFlashAttribute(
                "successMsg", "Đã xác nhận thanh toán!");

        return "redirect:/landlord/hoa-don/" + id;
    }

    @PostMapping("/landlord/hoa-don/{id}/tu-choi")
    public String tuChoiThanhToan(
            @PathVariable Integer id,
            @RequestParam(required = false) String lyDoTuChoi,
            RedirectAttributes redirectAttributes) {

        hoaDonService.tuChoiThanhToan(id, lyDoTuChoi);
        redirectAttributes.addFlashAttribute(
                "successMsg",
                "Đã từ chối thanh toán. Hóa đơn được chuyển về trạng thái Chưa thanh toán.");

        return "redirect:/landlord/hoa-don/" + id;
    }

    @PostMapping("/landlord/hoa-don/{id}/xoa")
    public String xoa(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        hoaDonService.deleteById(id);
        redirectAttributes.addFlashAttribute(
                "successMsg", "Đã xóa hóa đơn.");

        return "redirect:/landlord/hoa-don";
    }
}