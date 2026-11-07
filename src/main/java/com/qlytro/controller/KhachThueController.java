package com.qlytro.controller;

import com.qlytro.dto.request.KhachThueRequest;
import com.qlytro.dto.request.NguoiDungRequest;
import com.qlytro.service.impl.KhachThueService;
import com.qlytro.service.impl.NguoiDungService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Controller
@RequestMapping("/landlord/khach-thue")
public class KhachThueController {

    private final KhachThueService khachThueService;
    private final NguoiDungService nguoiDungService;

    public KhachThueController(KhachThueService khachThueService,
            NguoiDungService nguoiDungService) {
        this.khachThueService = khachThueService;
        this.nguoiDungService = nguoiDungService;
    }

    @GetMapping
    public String list(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        int pageSize = 10;

        Page<com.qlytro.dto.response.KhachThueResponse> pageData = khachThueService.search(
                keyword.trim(),
                PageRequest.of(Math.max(page, 0), pageSize));

        model.addAttribute("danhSach", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Quản lý Khách thuê");
        model.addAttribute("pageContent", "landlord/khach-thue/list");

        return "layout/base";
    }

    @GetMapping("/them")
    public String themForm(Model model) {
        model.addAttribute("khachRequest", new KhachThueRequest());
        model.addAttribute("taoTaiKhoan", false);
        model.addAttribute("danhSachTaiKhoan", nguoiDungService.findAll());
        model.addAttribute("pageTitle", "Thêm Khách thuê");

        // SỬA 2: Trả về layout/base cho form thêm
        model.addAttribute("pageContent", "landlord/khach-thue/form");
        return "layout/base";
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("khachRequest") KhachThueRequest request,
            BindingResult result,
            @RequestParam(name = "taoTaiKhoan", defaultValue = "false") boolean taoTaiKhoan,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "password", required = false) String password,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("taoTaiKhoan", taoTaiKhoan);
            model.addAttribute("danhSachTaiKhoan", nguoiDungService.findAll());
            model.addAttribute("pageTitle", "Thêm Khách thuê");

            // SỬA 3: Khi lỗi validate thì trả lại form trong base
            model.addAttribute("pageContent", "landlord/khach-thue/form");
            return "layout/base";
        }

        try {
            // Neu chon tao kem tai khoan
            if (taoTaiKhoan && username != null && !username.isBlank()) {
                NguoiDungRequest ndReq = new NguoiDungRequest();
                ndReq.setUsername(username);
                ndReq.setPassword(password);
                ndReq.setHoTen(request.getHoTen());
                ndReq.setSdt(request.getSdt());
                ndReq.setVaiTro(0); // KHACH_THUE
                var nd = nguoiDungService.create(ndReq);
                request.setNguoiDungId(nd.getId());
            }

            khachThueService.create(request);
            redirectAttributes.addFlashAttribute("successMsg", "Thêm khách thuê thành công!");
            return "redirect:/landlord/khach-thue";
        } catch (com.qlytro.exception.BusinessException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("taoTaiKhoan", taoTaiKhoan);
            model.addAttribute("danhSachTaiKhoan", nguoiDungService.findAll());
            model.addAttribute("pageTitle", "Thêm Khách thuê");
            model.addAttribute("pageContent", "landlord/khach-thue/form");
            return "layout/base";
        }
    }

    @GetMapping("/sua/{id}")
    public String suaForm(@PathVariable(name = "id") Integer id, Model model) {
        var kt = khachThueService.findById(id);
        KhachThueRequest req = new KhachThueRequest();
        req.setHoTen(kt.getHoTen());
        req.setCccd(kt.getCccd());
        req.setNgaySinh(kt.getNgaySinh());
        req.setSdt(kt.getSdt());
        req.setNguoiDungId(kt.getNguoiDungId());
        model.addAttribute("khachRequest", req);
        model.addAttribute("id", id);
        model.addAttribute("khach", kt);
        model.addAttribute("danhSachTaiKhoan", nguoiDungService.findAll());
        model.addAttribute("pageTitle", "Sửa thông tin Khách thuê");

        // SỬA 4: Trả về layout/base cho form sửa
        model.addAttribute("pageContent", "landlord/khach-thue/form");
        return "layout/base";
    }

    @PostMapping("/sua/{id}")
    public String sua(@PathVariable(name = "id") Integer id,
            @Valid @ModelAttribute("khachRequest") KhachThueRequest request,
            BindingResult result, Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("danhSachTaiKhoan", nguoiDungService.findAll());
            model.addAttribute("pageTitle", "Sửa Khách thuê");

            // SỬA 5: Khi sửa bị lỗi validate
            model.addAttribute("pageContent", "landlord/khach-thue/form");
            return "layout/base";
        }
        try {
            khachThueService.update(id, request);
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật thông tin khách thuê thành công!");
            return "redirect:/landlord/khach-thue";
        } catch (com.qlytro.exception.BusinessException ex) {
            model.addAttribute("id", id);
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("danhSachTaiKhoan", nguoiDungService.findAll());
            model.addAttribute("pageTitle", "Sửa Khách thuê");
            model.addAttribute("pageContent", "landlord/khach-thue/form");
            return "layout/base";
        }
    }

    @PostMapping("/xoa/{id}")
    public String xoa(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        khachThueService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Xóa khách thuê thành công!");
        return "redirect:/landlord/khach-thue";
    }
}