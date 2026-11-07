package com.qlytro.controller;

import com.qlytro.dto.response.ThongKeResponse;
import com.qlytro.entity.ToaNha;
import com.qlytro.repository.ToaNhaRepository;
import com.qlytro.service.impl.ThongKeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@PreAuthorize("hasRole('LANDLORD')")
public class ThongKeController {

    private final ThongKeService thongKeService;
    private final ToaNhaRepository toaNhaRepository;

    public ThongKeController(ThongKeService thongKeService, ToaNhaRepository toaNhaRepository) {
        this.thongKeService = thongKeService;
        this.toaNhaRepository = toaNhaRepository;
    }

    @GetMapping("/landlord/thong-ke")
    public String thongKe(
            @RequestParam(name = "toaNhaId", required = false) Integer toaNhaId,
            @RequestParam(name = "thoiGian", defaultValue = "thang-nay") String thoiGian,
            @RequestParam(name = "tuThang", required = false) String tuThang,
            @RequestParam(name = "denThang", required = false) String denThang,
            Model model
    ) {
        ThongKeResponse thongKe = thongKeService.getThongKe(toaNhaId, thoiGian, tuThang, denThang);
        List<ToaNha> dsToaNha = toaNhaRepository.findAll();

        model.addAttribute("thongKe", thongKe);
        model.addAttribute("dsToaNha", dsToaNha);
        model.addAttribute("selectedToaNhaId", toaNhaId);
        model.addAttribute("selectedThoiGian", thoiGian);
        model.addAttribute("tuThang", tuThang);
        model.addAttribute("denThang", denThang);

        model.addAttribute("pageTitle", "Thống kê");
        model.addAttribute("activeMenu", "thongKe");
        model.addAttribute("pageContent", "landlord/thong-ke/index");
        return "layout/base";
    }
}

