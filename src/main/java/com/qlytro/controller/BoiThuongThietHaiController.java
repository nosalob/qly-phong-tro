package com.qlytro.controller;

import com.qlytro.dto.request.BoiThuongThietHaiRequest;
import com.qlytro.entity.enums.NguonPhatHienBoiThuong;
import com.qlytro.service.impl.BoiThuongThietHaiService;
import com.qlytro.service.impl.HopDongService;
import com.qlytro.service.impl.SuCoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/landlord/boi-thuong")
public class BoiThuongThietHaiController {

    private final BoiThuongThietHaiService boiThuongService;
    private final HopDongService hopDongService;
    private final SuCoService suCoService;

    public BoiThuongThietHaiController(BoiThuongThietHaiService boiThuongService,
                                       HopDongService hopDongService,
                                       SuCoService suCoService) {
        this.boiThuongService = boiThuongService;
        this.hopDongService = hopDongService;
        this.suCoService = suCoService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("danhSach", boiThuongService.findAll());
        model.addAttribute("pageTitle", "Bồi thường thiệt hại");
        model.addAttribute("pageContent", "landlord/boi-thuong/list");
        return "layout/base";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("boiThuong", boiThuongService.findById(id));
        model.addAttribute("pageTitle", "Chi tiết Bồi thường");
        model.addAttribute("pageContent", "landlord/boi-thuong/detail");
        return "layout/base";
    }

    @GetMapping("/them")
    public String themForm(@RequestParam(required = false) Integer suCoId,
                           @RequestParam(required = false) Integer hopDongId,
                           Model model) {
        BoiThuongThietHaiRequest req = new BoiThuongThietHaiRequest();
        if (suCoId != null) {
            req.setSuCoId(suCoId);
            try {
                var suCo = suCoService.findById(suCoId);
                req.setMoTa("Bồi thường thiệt hại do sự cố #" + suCoId + ": " + suCo.getMoTa());
                // Tim hop dong active cua phong nay
                var dsHopDong = hopDongService.findAll();
                for (var hd : dsHopDong) {
                    if (hd.getPhongId().equals(suCo.getPhongId()) && "ACTIVE".equalsIgnoreCase(hd.getTrangThai().name())) {
                        req.setHopDongId(hd.getId());
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        if (hopDongId != null) {
            req.setHopDongId(hopDongId);
        }
        if (req.getNguonPhatHien() == null) {
            req.setNguonPhatHien(0); // Mac dinh TRONG_LUC_O
        }

        model.addAttribute("request", req);
        model.addAttribute("danhSachHopDong", hopDongService.findAll().stream()
                .filter(hd -> "ACTIVE".equalsIgnoreCase(hd.getTrangThai().name()))
                .toList());
        model.addAttribute("danhSachSuCo", suCoService.findAll());
        model.addAttribute("nguonPhatHienList", NguonPhatHienBoiThuong.values());
        model.addAttribute("pageTitle", "Tạo yêu cầu Bồi thường");
        model.addAttribute("pageContent", "landlord/boi-thuong/form");
        return "layout/base";
    }

    @PostMapping("/them")
    public String them(@Valid @ModelAttribute("request") BoiThuongThietHaiRequest request,
                       BindingResult result, Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("danhSachHopDong", hopDongService.findAll().stream()
                    .filter(hd -> "ACTIVE".equalsIgnoreCase(hd.getTrangThai().name()))
                    .toList());
            model.addAttribute("danhSachSuCo", suCoService.findAll());
            model.addAttribute("nguonPhatHienList", NguonPhatHienBoiThuong.values());
            model.addAttribute("pageTitle", "Tạo yêu cầu Bồi thường");
            model.addAttribute("pageContent", "landlord/boi-thuong/form");
            return "layout/base";
        }

        try {
            var bt = boiThuongService.create(request);
            redirectAttributes.addFlashAttribute("successMsg", "Ghi nhận yêu cầu bồi thường thành công!");
            return "redirect:/landlord/boi-thuong/" + bt.getId();
        } catch (com.qlytro.exception.BusinessException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("danhSachHopDong", hopDongService.findAll().stream()
                    .filter(hd -> "ACTIVE".equalsIgnoreCase(hd.getTrangThai().name()))
                    .toList());
            model.addAttribute("danhSachSuCo", suCoService.findAll());
            model.addAttribute("nguonPhatHienList", NguonPhatHienBoiThuong.values());
            model.addAttribute("pageTitle", "Tạo yêu cầu Bồi thường");
            model.addAttribute("pageContent", "landlord/boi-thuong/form");
            return "layout/base";
        }
    }

    @PostMapping("/{id}/xac-nhan")
    public String xacNhanThanhToan(@PathVariable Integer id,
                                   RedirectAttributes redirectAttributes) {
        boiThuongService.xacNhanThanhToan(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã xác nhận thanh toán khoản bồi thường!");
        return "redirect:/landlord/boi-thuong/" + id;
    }

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable Integer id,
                      RedirectAttributes redirectAttributes) {
        boiThuongService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa yêu cầu bồi thường.");
        return "redirect:/landlord/boi-thuong";
    }
}
