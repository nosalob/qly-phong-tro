package com.qlytro.controller;

import com.qlytro.dto.request.NhacThanhToanRequest;
import com.qlytro.dto.response.HoaDonQuaHanResponse;
import com.qlytro.dto.response.HoaDonResponse;
import com.qlytro.dto.response.NhacThanhToanResponse;
import com.qlytro.entity.enums.HinhThucNhac;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.security.CustomUserDetails;
import com.qlytro.service.impl.HoaDonService;
import com.qlytro.service.impl.HopDongService;
import com.qlytro.service.impl.NhacThanhToanService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class HoaDonController {

    private final HoaDonService hoaDonService;
    private final HopDongService hopDongService;
    private final com.qlytro.service.impl.ToaNhaService toaNhaService;
    private final com.qlytro.service.impl.PhongService phongService;
    private final NhacThanhToanService nhacThanhToanService;

    public HoaDonController(
            HoaDonService hoaDonService,
            HopDongService hopDongService,
            com.qlytro.service.impl.ToaNhaService toaNhaService,
            com.qlytro.service.impl.PhongService phongService,
            NhacThanhToanService nhacThanhToanService) {
        this.hoaDonService = hoaDonService;
        this.hopDongService = hopDongService;
        this.toaNhaService = toaNhaService;
        this.phongService = phongService;
        this.nhacThanhToanService = nhacThanhToanService;
    }

    // ===== LANDLORD =====
    @GetMapping("/landlord/hoa-don/qua-han")
    public String landlordQuaHan(
            @RequestParam(name = "toaNhaId", required = false) Integer toaNhaId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<HoaDonQuaHanResponse> pageResult = hoaDonService.findHoaDonQuaHanPage(toaNhaId, pageable);

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("danhSachToaNha", toaNhaService.findAll());
        model.addAttribute("toaNhaId", toaNhaId);
        model.addAttribute("pageTitle", "Hóa đơn Quá hạn");
        model.addAttribute("pageContent", "landlord/hoa-don/qua-han");

        return "layout/base";
    }

    @GetMapping("/landlord/hoa-don")
    public String landlordList(
            @RequestParam(name = "toaNhaId", required = false) Integer toaNhaId,
            @RequestParam(name = "phongId", required = false) Integer phongId,
            @RequestParam(name = "tuThang", required = false) String tuThang,
            @RequestParam(name = "denThang", required = false) String denThang,
            @RequestParam(name = "trangThai", required = false) String trangThai,
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
    public String landlordDetail(@PathVariable(name = "id") Integer id, Model model) {
        HoaDonResponse hoaDon = hoaDonService.findById(id);
        List<NhacThanhToanResponse> lichSuNhac = nhacThanhToanService.findByHoaDonId(id);

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("lichSuNhac", lichSuNhac);
        model.addAttribute("nhacRequest", new NhacThanhToanRequest());
        model.addAttribute("hinhThucList", HinhThucNhac.values());
        model.addAttribute("pageTitle", "Chi tiết Hóa đơn");
        model.addAttribute("pageContent", "landlord/hoa-don/detail");
        return "layout/base";
    }

    @PostMapping("/landlord/hoa-don/{id}/nhac-thanh-toan")
    public String ghiNhanNhacThanhToan(
            @PathVariable(name = "id") Integer id,
            @Valid @ModelAttribute("nhacRequest") NhacThanhToanRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/landlord/hoa-don/" + id;
        }

        try {
            nhacThanhToanService.ghiNhanNhacThanhToan(id, request, userDetails.getId());
            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    "Đã ghi nhận lịch sử nhắc thanh toán thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    "Lỗi ghi nhận nhắc nhở: " + e.getMessage());
        }

        return "redirect:/landlord/hoa-don/" + id;
    }

    @PostMapping("/landlord/hoa-don/tao")
    public String taoHoaDon(
            @RequestParam(name = "hopDongId", required = false) Integer hopDongId,
            @RequestParam(name = "phongId", required = false) Integer phongId,
            @RequestParam(name = "thangNam") String thangNam,
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

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    "Hóa đơn tháng " + thangNam + " đã được tạo bởi tiến trình khác.");
            return "redirect:/landlord/hoa-don";
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
            @PathVariable(name = "id") Integer id,
            RedirectAttributes redirectAttributes) {

        hoaDonService.xacNhanThanhToan(id);
        redirectAttributes.addFlashAttribute(
                "successMsg", "Đã xác nhận thanh toán!");

        return "redirect:/landlord/hoa-don/" + id;
    }

    @PostMapping("/landlord/hoa-don/{id}/tu-choi")
    public String tuChoiThanhToan(
            @PathVariable(name = "id") Integer id,
            @RequestParam(name = "lyDoTuChoi", required = false) String lyDoTuChoi,
            RedirectAttributes redirectAttributes) {

        hoaDonService.tuChoiThanhToan(id, lyDoTuChoi);
        redirectAttributes.addFlashAttribute(
                "successMsg",
                "Đã từ chối thanh toán. Hóa đơn được chuyển về trạng thái Chưa thanh toán.");

        return "redirect:/landlord/hoa-don/" + id;
    }

    @PostMapping("/landlord/hoa-don/{id}/xoa")
    public String xoa(
            @PathVariable(name = "id") Integer id,
            RedirectAttributes redirectAttributes) {

        hoaDonService.deleteById(id);
        redirectAttributes.addFlashAttribute(
                "successMsg", "Đã xóa hóa đơn.");

        return "redirect:/landlord/hoa-don";
    }
}