package com.qlytro.controller;

import com.qlytro.dto.request.ToaNhaRequest;
import com.qlytro.dto.response.ToaNhaResponse;
import com.qlytro.entity.enums.TrangThaiPhong;
import com.qlytro.entity.enums.TrangThaiToaNha;
import com.qlytro.service.impl.PhongService;
import com.qlytro.service.impl.ToaNhaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ToaNhaController {

    private final ToaNhaService toaNhaService;
    private final PhongService phongService;

    public ToaNhaController(ToaNhaService toaNhaService, PhongService phongService) {
        this.toaNhaService = toaNhaService;
        this.phongService = phongService;
    }

    /**
     * Trang Quản lý tập trung Tòa nhà & Phòng
     */
    @GetMapping("/landlord/quan-ly")
    public String quanLy(
            @RequestParam(name = "toaNhaId", required = false) Integer toaNhaId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "trangThai", required = false) Integer trangThai,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        var dsToaNha = toaNhaService.findAll();
        model.addAttribute("danhSachToaNha", dsToaNha);

        ToaNhaResponse selectedToaNha = null;
        if (toaNhaId != null) {
            selectedToaNha = dsToaNha.stream()
                    .filter(tn -> tn.getId().equals(toaNhaId))
                    .findFirst()
                    .orElse(null);
        }
        // Neu toaNhaId == null, selectedToaNha = null (Che do: Tat ca toa nha)
        model.addAttribute("selectedToaNha", selectedToaNha);
        model.addAttribute("toaNhaId", selectedToaNha != null ? selectedToaNha.getId() : null);

        int validatedPage = Math.max(1, page);
        int validatedSize = (size <= 0) ? 10 : size;

        var pageData = phongService.findPhongQuanLy(
                selectedToaNha != null ? selectedToaNha.getId() : null,
                keyword,
                trangThai,
                validatedPage,
                validatedSize
        );
        model.addAttribute("pageData", pageData);
        model.addAttribute("danhSachPhong", pageData.getContent());
        model.addAttribute("currentPage", validatedPage);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());

        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("trangThaiList", TrangThaiPhong.values());
        model.addAttribute("size", validatedSize);
        model.addAttribute("pageTitle", "Quản lý");
        model.addAttribute("activeMenu", "quanLy");
        model.addAttribute("pageContent", "landlord/quan-ly/index");

        return "layout/base";
    }

    @GetMapping("/landlord/toa-nha")
    public String list(Model model) {
        model.addAttribute("danhSach", toaNhaService.findAll());
        model.addAttribute("pageTitle", "Quản lý Tòa nhà");
        model.addAttribute("pageContent", "landlord/toa-nha/list");
        return "layout/base";
    }

    @GetMapping("/landlord/toa-nha/them")
    public String themForm(Model model) {
        model.addAttribute("request", new ToaNhaRequest());
        model.addAttribute("trangThaiList", TrangThaiToaNha.values());
        model.addAttribute("pageTitle", "Thêm Tòa nhà");
        model.addAttribute("pageContent", "landlord/toa-nha/form");
        return "layout/base";
    }

    @PostMapping("/landlord/toa-nha/them")
    public String them(@Valid @ModelAttribute("request") ToaNhaRequest request,
                       BindingResult result, Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("trangThaiList", TrangThaiToaNha.values());
            model.addAttribute("pageTitle", "Thêm Tòa nhà");
            model.addAttribute("pageContent", "landlord/toa-nha/form");
            return "layout/base";
        }
        var created = toaNhaService.create(request);
        redirectAttributes.addFlashAttribute("successMsg", "Thêm tòa nhà thành công!");
        return "redirect:/landlord/quan-ly?toaNhaId=" + created.getId();
    }

    @GetMapping("/landlord/toa-nha/sua/{id}")
    public String suaForm(@PathVariable(name = "id") Integer id, Model model) {
        var existing = toaNhaService.findById(id);
        ToaNhaRequest request = new ToaNhaRequest();
        request.setTenToaNha(existing.getTenToaNha());
        request.setDiaChi(existing.getDiaChi());
        request.setTrangThai(existing.getTrangThai().getValue());
        model.addAttribute("request", request);
        model.addAttribute("id", id);
        model.addAttribute("trangThaiList", TrangThaiToaNha.values());
        model.addAttribute("pageTitle", "Sửa Tòa nhà");
        model.addAttribute("pageContent", "landlord/toa-nha/form");
        return "layout/base";
    }

    @PostMapping("/landlord/toa-nha/sua/{id}")
    public String sua(@PathVariable(name = "id") Integer id,
                      @Valid @ModelAttribute("request") ToaNhaRequest request,
                      BindingResult result, Model model,
                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("trangThaiList", TrangThaiToaNha.values());
            model.addAttribute("pageTitle", "Sửa Tòa nhà");
            model.addAttribute("pageContent", "landlord/toa-nha/form");
            return "layout/base";
        }
        toaNhaService.update(id, request);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật tòa nhà thành công!");
        return "redirect:/landlord/quan-ly?toaNhaId=" + id;
    }

    @PostMapping("/landlord/toa-nha/xoa/{id}")
    public String xoa(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        toaNhaService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Xóa tòa nhà thành công!");
        return "redirect:/landlord/quan-ly";
    }
}
