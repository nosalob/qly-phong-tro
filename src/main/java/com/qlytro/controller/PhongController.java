package com.qlytro.controller;

import com.qlytro.dto.request.PhongRequest;
import com.qlytro.entity.enums.TrangThaiPhong;
import com.qlytro.service.impl.PhongService;
import com.qlytro.service.impl.ThietBiService;
import com.qlytro.service.impl.ToaNhaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/landlord/phong")
public class PhongController {

    private final PhongService phongService;
    private final ToaNhaService toaNhaService;
    private final ThietBiService thietBiService;

    public PhongController(PhongService phongService,
                           ToaNhaService toaNhaService,
                           ThietBiService thietBiService) {
        this.phongService = phongService;
        this.toaNhaService = toaNhaService;
        this.thietBiService = thietBiService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer toaNhaId, Model model) {
        if (toaNhaId != null) {
            model.addAttribute("danhSach", phongService.findByToaNha(toaNhaId));
            model.addAttribute("toaNhaFilter", toaNhaService.findById(toaNhaId));
        } else {
            model.addAttribute("danhSach", phongService.findAll());
        }
        model.addAttribute("danhSachToaNha", toaNhaService.findAll());
        model.addAttribute("pageTitle", "Quản lý Phòng");

        // === SỬA Ở ĐÂY 1: Thêm pageContent và trả về layout/base ===
        model.addAttribute("pageContent", "landlord/phong/list");
        return "layout/base";
    }

    @GetMapping("/chi-tiet/{id}")
    public String chiTiet(@PathVariable Integer id, Model model) {
        model.addAttribute("phong", phongService.findById(id));
        model.addAttribute("danhSachThietBi", thietBiService.findAll());
        model.addAttribute("pageTitle", "Chi tiết Phòng");

        // === SỬA Ở ĐÂY 2: View chi tiết phòng và thiết bị ===
        model.addAttribute("pageContent", "landlord/phong/detail");
        return "layout/base";
    }

    @GetMapping("/them")
    public String themForm(@RequestParam(required = false) Integer toaNhaId, Model model) {
        PhongRequest req = new PhongRequest();
        req.setToaNhaId(toaNhaId);
        model.addAttribute("request", req);
        model.addAttribute("danhSachToaNha", toaNhaService.findAll());
        model.addAttribute("trangThaiList", TrangThaiPhong.values());
        model.addAttribute("pageTitle", "Thêm Phòng");

        // === SỬA Ở ĐÂY 3: Mở form thêm phòng ===
        model.addAttribute("pageContent", "landlord/phong/form");
        return "layout/base";
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("request") PhongRequest request,
                       BindingResult result, Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("danhSachToaNha", toaNhaService.findAll());
            model.addAttribute("trangThaiList", TrangThaiPhong.values());
            model.addAttribute("pageTitle", "Thêm Phòng");

            // === SỬA Ở ĐÂY 4: Khi validate lỗi, hiển thị lại form thêm ===
            model.addAttribute("pageContent", "landlord/phong/form");
            return "layout/base";
        }
        phongService.create(request);
        redirectAttributes.addFlashAttribute("successMsg", "Thêm phòng thành công!");
        return "redirect:/landlord/phong";
    }

    @GetMapping("/sua/{id}")
    public String suaForm(@PathVariable Integer id, Model model) {
        var p = phongService.findById(id);
        PhongRequest req = new PhongRequest();
        req.setToaNhaId(p.getToaNhaId());
        req.setSoPhong(p.getSoPhong());
        req.setDienTich(p.getDienTich());
        req.setGiaThueTheoMacDinh(p.getGiaThueTheoMacDinh());
        // KHONG set trangThai vao request — trang thai phong do he thong quan ly,
        // khong cho phep chinh tay qua form sua phong.
        req.setMoTa(p.getMoTa());
        model.addAttribute("request", req);
        model.addAttribute("id", id);
        model.addAttribute("danhSachToaNha", toaNhaService.findAll());
        // KHONG truyen trangThaiList — form sua khong hien dropdown trang thai
        model.addAttribute("phongHienTai", p); // chi de hien thi (read-only)
        model.addAttribute("pageTitle", "Sửa Phòng");
        model.addAttribute("pageContent", "landlord/phong/form");
        return "layout/base";
    }

    @PostMapping("/sua/{id}")
    public String sua(@PathVariable Integer id,
                      @Valid @ModelAttribute("request") PhongRequest request,
                      BindingResult result, Model model,
                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("danhSachToaNha", toaNhaService.findAll());
            model.addAttribute("phongHienTai", phongService.findById(id));
            // KHONG truyen trangThaiList
            model.addAttribute("pageTitle", "Sửa Phòng");
            model.addAttribute("pageContent", "landlord/phong/form");
            return "layout/base";
        }
        phongService.update(id, request);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật phòng thành công!");
        return "redirect:/landlord/phong";
    }

    @PostMapping("/xoa/{id}")
    public String xoa(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        phongService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Xóa phòng thành công!");
        return "redirect:/landlord/phong";
    }

    // Quan ly thiet bi trong phong (Các thao tác này redirect nên giữ nguyên)
    @PostMapping("/{phongId}/them-thiet-bi")
    public String themThietBiVaoPhong(@PathVariable Integer phongId,
                                      @RequestParam Integer thietBiId,
                                      RedirectAttributes redirectAttributes) {
        thietBiService.themVaoPhong(phongId, thietBiId);
        redirectAttributes.addFlashAttribute("successMsg", "Đã thêm thiết bị vào phòng!");
        return "redirect:/landlord/phong/chi-tiet/" + phongId;
    }

    @PostMapping("/{phongId}/xoa-thiet-bi/{thietBiId}")
    public String xoaThietBiKhoiPhong(@PathVariable Integer phongId,
                                      @PathVariable Integer thietBiId,
                                      RedirectAttributes redirectAttributes) {
        thietBiService.xoaKhoiPhong(phongId, thietBiId);
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa thiết bị khỏi phòng!");
        return "redirect:/landlord/phong/chi-tiet/" + phongId;
    }

    @PostMapping("/{id}/ngung-su-dung")
    public String ngungSuDung(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        phongService.ngungSuDung(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã chuyển phòng sang trạng thái Ngừng sử dụng.");
        return "redirect:/landlord/phong/chi-tiet/" + id;
    }

    @PostMapping("/{id}/mo-lai")
    public String moLai(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        phongService.moLai(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã mở lại phòng thành công (trạng thái Trống).");
        return "redirect:/landlord/phong/chi-tiet/" + id;
    }
}