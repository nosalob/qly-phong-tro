package com.qlytro.controller;

import com.qlytro.dto.request.HopDongRequest;
import com.qlytro.entity.enums.TrangThaiHopDong;
import com.qlytro.service.impl.HopDongService;
import com.qlytro.service.impl.KhachThueService;
import com.qlytro.service.impl.PhongService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/landlord/hop-dong")
public class HopDongController {

    private final HopDongService hopDongService;
    private final PhongService phongService;
    private final KhachThueService khachThueService;

    public HopDongController(HopDongService hopDongService,
                             PhongService phongService,
                             KhachThueService khachThueService) {
        this.hopDongService = hopDongService;
        this.phongService = phongService;
        this.khachThueService = khachThueService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("danhSach", hopDongService.findAll());
        model.addAttribute("pageTitle", "Quản lý Hợp đồng");

        // SỬA 1: Trả về layout/base
        model.addAttribute("pageContent", "landlord/hop-dong/list");
        return "layout/base";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("hopDong", hopDongService.findById(id));
        model.addAttribute("pageTitle", "Chi tiết Hợp đồng");

        // SỬA 2: Trả về layout/base
        model.addAttribute("pageContent", "landlord/hop-dong/detail");
        return "layout/base";
    }

    @GetMapping("/them")
    public String themForm(Model model) {
        model.addAttribute("request", new HopDongRequest());
        model.addAttribute("danhSachPhong", phongService.findAll());
        model.addAttribute("danhSachKhach", khachThueService.findAll());
        model.addAttribute("pageTitle", "Ký Hợp đồng mới");

        // SỬA 3: Trả về layout/base
        model.addAttribute("pageContent", "landlord/hop-dong/form");
        return "layout/base";
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("request") HopDongRequest request,
                       BindingResult result, Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("danhSachPhong", phongService.findAll());
            model.addAttribute("danhSachKhach", khachThueService.findAll());
            model.addAttribute("pageTitle", "Ký Hợp đồng mới");

            // SỬA 4: Khi validate lỗi, nạp lại form trong layout/base
            model.addAttribute("pageContent", "landlord/hop-dong/form");
            return "layout/base";
        }
        try {
            var hd = hopDongService.create(request);
            redirectAttributes.addFlashAttribute("successMsg", "Ký hợp đồng thành công!");
            return "redirect:/landlord/hop-dong/" + hd.getId();
        } catch (com.qlytro.exception.BusinessException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("danhSachPhong", phongService.findAll());
            model.addAttribute("danhSachKhach", khachThueService.findAll());
            model.addAttribute("pageTitle", "Ký Hợp đồng mới");
            model.addAttribute("pageContent", "landlord/hop-dong/form");
            return "layout/base";
        }
    }

    @PostMapping("/{id}/ket-thuc")
    public String ketThuc(@PathVariable Integer id,
                          @RequestParam(defaultValue = "1") Integer loai,
                          RedirectAttributes redirectAttributes) {
        TrangThaiHopDong trangThai = loai == 2
                ? TrangThaiHopDong.KET_THUC_BAT_THUONG
                : TrangThaiHopDong.KET_THUC_BINH_THUONG;
        hopDongService.ketThuc(id, trangThai);
        redirectAttributes.addFlashAttribute("successMsg", "Đã kết thúc hợp đồng.");
        return "redirect:/landlord/hop-dong/" + id;
    }

    @PostMapping("/{id}/them-nguoi-o")
    public String themNguoiOCung(@PathVariable Integer id,
                                 @ModelAttribute com.qlytro.dto.request.KhachThueRequest khachRequest,
                                 RedirectAttributes redirectAttributes) {
        try {
            hopDongService.themNguoiOCung(id, khachRequest);
            redirectAttributes.addFlashAttribute("successMsg", "Đã thêm người ở cùng vào hợp đồng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi thêm người ở cùng: " + e.getMessage());
        }
        return "redirect:/landlord/hop-dong/" + id;
    }

    @PostMapping("/{id}/doi-dai-dien")
    public String doiDaiDien(@PathVariable Integer id,
                             @RequestParam Integer khachThueId,
                             RedirectAttributes redirectAttributes) {
        try {
            hopDongService.doiNguoiDaiDien(id, khachThueId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã chuyển đổi người đại diện hợp đồng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi đổi người đại diện: " + e.getMessage());
        }
        return "redirect:/landlord/hop-dong/" + id;
    }

    @PostMapping("/{id}/nguoi-roi-di")
    public String nguoiRoiDi(@PathVariable Integer id,
                             @RequestParam Integer khachThueId,
                             RedirectAttributes redirectAttributes) {
        try {
            hopDongService.nguoiRoiDi(id, khachThueId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã ghi nhận người thuê rời phòng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi cập nhật: " + e.getMessage());
        }
        return "redirect:/landlord/hop-dong/" + id;
    }
}