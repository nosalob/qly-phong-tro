package com.qlytro.controller;

import com.qlytro.dto.request.NguoiDungRequest;
import com.qlytro.entity.enums.VaiTroNguoiDung;
import com.qlytro.service.impl.NguoiDungService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/landlord/nguoi-dung")
public class NguoiDungController {

    private final NguoiDungService nguoiDungService;

    public NguoiDungController(NguoiDungService nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new org.springframework.beans.propertyeditors.StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("danhSach", nguoiDungService.findAll());
        model.addAttribute("pageTitle", "Quản lý Tài khoản");
        model.addAttribute("pageContent", "landlord/nguoi-dung/list");
        return "layout/base";
    }

    @GetMapping("/them")
    public String themForm(Model model) {
        model.addAttribute("request", new NguoiDungRequest());
        model.addAttribute("vaiTroList", VaiTroNguoiDung.values());
        model.addAttribute("pageTitle", "Tạo Tài khoản");
        model.addAttribute("pageContent", "landlord/nguoi-dung/form");
        return "layout/base";
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("request") NguoiDungRequest request,
                       BindingResult result, Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vaiTroList", VaiTroNguoiDung.values());
            model.addAttribute("pageTitle", "Tạo Tài khoản");
            model.addAttribute("pageContent", "landlord/nguoi-dung/form");
            return "layout/base";
        }
        try {
            nguoiDungService.create(request);
            redirectAttributes.addFlashAttribute("successMsg", "Tạo tài khoản thành công!");
            return "redirect:/landlord/nguoi-dung";
        } catch (com.qlytro.exception.BusinessException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("vaiTroList", VaiTroNguoiDung.values());
            model.addAttribute("pageTitle", "Tạo Tài khoản");
            model.addAttribute("pageContent", "landlord/nguoi-dung/form");
            return "layout/base";
        }
    }

    @PostMapping("/{id}/khoa")
    public String khoa(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        nguoiDungService.khoaTaiKhoan(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã khóa tài khoản.");
        return "redirect:/landlord/nguoi-dung";
    }

    @PostMapping("/{id}/mo-khoa")
    public String moKhoa(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        nguoiDungService.moKhoa(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã mở khóa tài khoản.");
        return "redirect:/landlord/nguoi-dung";
    }

    @PostMapping("/{id}/reset-mat-khau")
    public String resetMatKhau(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        nguoiDungService.resetMatKhau(id);
        redirectAttributes.addFlashAttribute("successMsg",
                "Đã reset mật khẩu tài khoản về mặc định (123456). Tài khoản sẽ bắt buộc phải đổi mật khẩu khi đăng nhập!");
        return "redirect:/landlord/nguoi-dung";
    }
}