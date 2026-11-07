package com.qlytro.controller;

import com.qlytro.dto.request.ChiSoDienNuocRequest;
import com.qlytro.dto.response.ChiSoDienNuocResponse;
import com.qlytro.service.impl.ChiSoDienNuocService;
import com.qlytro.service.impl.PhongService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/landlord/chi-so")
public class ChiSoDienNuocController {

    private final ChiSoDienNuocService chiSoService;
    private final PhongService phongService;

    public ChiSoDienNuocController(ChiSoDienNuocService chiSoService,
                                   PhongService phongService) {
        this.chiSoService = chiSoService;
        this.phongService = phongService;
    }

    // Danh sách, lọc và phân trang
    @GetMapping
    public String list(
            @RequestParam(name = "phongId", required = false) Integer phongId,
            @RequestParam(name = "thangNam", required = false) String thangNam,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        if (page < 0) {
            page = 0;
        }

        Page<ChiSoDienNuocResponse> pageData =
                chiSoService.findByFilters(
                        phongId,
                        thangNam,
                        PageRequest.of(page, 10)
                );

        model.addAttribute("danhSach", pageData.getContent());
        model.addAttribute("currentPage", pageData.getNumber());
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalElements", pageData.getTotalElements());

        model.addAttribute("phongFilter",
                phongId != null ? phongService.findById(phongId) : null);
        model.addAttribute("thangNamFilter", thangNam);

        model.addAttribute("danhSachPhong", phongService.findAll());
        model.addAttribute("pageTitle", "Chỉ số Điện / Nước");
        model.addAttribute("pageContent", "landlord/chi-so/list");

        return "layout/base";
    }

    // Form nhập mới
    @GetMapping("/nhap")
    public String nhapForm(
            @RequestParam(name = "phongId", required = false) Integer phongId,
            Model model) {

        ChiSoDienNuocRequest req = new ChiSoDienNuocRequest();
        req.setPhongId(phongId);
        req.setThangNam(java.time.YearMonth.now().toString());

        boolean daCoChiSoCu = false;

        if (phongId != null) {
            var latestOpt = chiSoService.findLatestByPhong(phongId);

            if (latestOpt.isPresent()) {
                var latest = latestOpt.get();

                req.setChiSoDienCu(latest.getChiSoDienMoi());
                req.setChiSoNuocCu(latest.getChiSoNuocMoi());
                daCoChiSoCu = true;
            } else {
                req.setChiSoDienCu(java.math.BigDecimal.ZERO);
                req.setChiSoNuocCu(java.math.BigDecimal.ZERO);
            }
        }

        model.addAttribute("request", req);
        model.addAttribute("daCoChiSoCu", daCoChiSoCu);
        model.addAttribute("isEdit", false);
        model.addAttribute("danhSachPhong", phongService.findAll());
        model.addAttribute("pageTitle", "Nhập chỉ số điện/nước");
        model.addAttribute("pageContent", "landlord/chi-so/form");

        return "layout/base";
    }

    // Lưu chỉ số mới
    @PostMapping("/nhap")
    public String nhap(
            @Valid @ModelAttribute("request") ChiSoDienNuocRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("danhSachPhong", phongService.findAll());
            model.addAttribute("daCoChiSoCu", false);
            model.addAttribute("isEdit", false);
            model.addAttribute("pageTitle", "Nhập chỉ số điện/nước");
            model.addAttribute("pageContent", "landlord/chi-so/form");

            return "layout/base";
        }

        chiSoService.create(request);

        redirectAttributes.addFlashAttribute(
                "successMsg", "Nhập chỉ số thành công!");

        return "redirect:/landlord/chi-so";
    }

    // Form sửa chỉ số
    @GetMapping("/{id}/sua")
    public String suaForm(@PathVariable("id") Integer id, Model model) {

        ChiSoDienNuocResponse data = chiSoService.findById(id);

        ChiSoDienNuocRequest req = new ChiSoDienNuocRequest();
        req.setPhongId(data.getPhongId());
        req.setThangNam(data.getThangNam());
        req.setChiSoDienCu(data.getChiSoDienCu());
        req.setChiSoDienMoi(data.getChiSoDienMoi());
        req.setChiSoNuocCu(data.getChiSoNuocCu());
        req.setChiSoNuocMoi(data.getChiSoNuocMoi());
        req.setGhiChu(data.getGhiChu());

        model.addAttribute("request", req);
        model.addAttribute("id", id);
        model.addAttribute("isEdit", true);
        model.addAttribute("daCoChiSoCu", false);
        model.addAttribute("danhSachPhong", phongService.findAll());
        model.addAttribute("pageTitle", "Sửa chỉ số điện/nước");
        model.addAttribute("pageContent", "landlord/chi-so/form");

        return "layout/base";
    }

    // Cập nhật chỉ số
    @PostMapping("/{id}/sua")
    public String sua(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("request") ChiSoDienNuocRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("isEdit", true);
            model.addAttribute("daCoChiSoCu", false);
            model.addAttribute("danhSachPhong", phongService.findAll());
            model.addAttribute("pageTitle", "Sửa chỉ số điện/nước");
            model.addAttribute("pageContent", "landlord/chi-so/form");

            return "layout/base";
        }

        chiSoService.update(id, request);

        redirectAttributes.addFlashAttribute(
                "successMsg", "Cập nhật chỉ số thành công!");

        return "redirect:/landlord/chi-so";
    }

    // Chốt chỉ số
    @PostMapping("/{id}/chot")
    public String chotChiSo(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        chiSoService.chotChiSo(id);

        redirectAttributes.addFlashAttribute(
                "successMsg", "Chốt chỉ số thành công!");

        return "redirect:/landlord/chi-so";
    }

    // Xóa chỉ số
    @PostMapping("/{id}/xoa")
    public String xoa(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        chiSoService.deleteById(id);

        redirectAttributes.addFlashAttribute(
                "successMsg", "Xóa chỉ số thành công!");

        return "redirect:/landlord/chi-so";
    }
}