package com.qlytro.controller;

import com.qlytro.service.impl.CongViecService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/landlord/cong-viec")
public class CongViecController {

    private final CongViecService congViecService;

    public CongViecController(CongViecService congViecService) {
        this.congViecService = congViecService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("congViec", congViecService.getTongHopCongViec());
        model.addAttribute("pageTitle", "Công việc cần xử lý");
        model.addAttribute("activeMenu", "congViec");
        model.addAttribute("pageContent", "landlord/cong-viec/index");
        return "layout/base";
    }
}

