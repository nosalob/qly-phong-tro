package com.qlytro.controller;

import com.qlytro.entity.KhachThue;
import com.qlytro.repository.HopDongRepository;
import com.qlytro.repository.KhachThueRepository;
import com.qlytro.security.CustomUserDetails;
import com.qlytro.service.impl.ChiSoDienNuocService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tenant/chi-so")
public class TenantChiSoController {

    private final ChiSoDienNuocService chiSoService;
    private final KhachThueRepository khachThueRepository;
    private final HopDongRepository hopDongRepository;

    public TenantChiSoController(ChiSoDienNuocService chiSoService,
                                  KhachThueRepository khachThueRepository,
                                  HopDongRepository hopDongRepository) {
        this.chiSoService = chiSoService;
        this.khachThueRepository = khachThueRepository;
        this.hopDongRepository = hopDongRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // Tim khach thue cua tai khoan nay
        KhachThue khachThue = khachThueRepository
                .findByNguoiDung_Id(userDetails.getId())
                .orElse(null);

        if (khachThue == null) {
            model.addAttribute("danhSach", java.util.List.of());
            model.addAttribute("infoMsg", "Tài khoản chưa liên kết hồ sơ khách thuê.");
        } else {
            // Tim hop dong ACTIVE cua khach -> lay phong_id -> lay chi so phong do
            hopDongRepository.findActiveByKhachThueId(khachThue.getId())
                    .ifPresentOrElse(
                            hopDong -> {
                                Integer phongId = hopDong.getPhong().getId();
                                model.addAttribute("danhSach",
                                        chiSoService.findByPhong(phongId));
                                model.addAttribute("soPhong", hopDong.getPhong().getSoPhong());
                                model.addAttribute("tenToaNha",
                                        hopDong.getPhong().getToaNha().getTenToaNha());
                            },
                            () -> {
                                model.addAttribute("danhSach", java.util.List.of());
                                model.addAttribute("infoMsg",
                                        "Bạn không có hợp đồng đang hiệu lực.");
                            }
                    );
        }
        model.addAttribute("pageTitle", "Chỉ số Điện / Nước");
        model.addAttribute("pageContent", "tenant/chi-so/list");
        return "layout/base";
    }
}
