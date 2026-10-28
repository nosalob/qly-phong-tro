package com.qlytro.controller;

import com.qlytro.entity.HopDong;
import com.qlytro.entity.HopDongKhachThue;
import com.qlytro.entity.KhachThue;
import com.qlytro.repository.HoaDonRepository;
import com.qlytro.repository.HopDongKhachThueRepository;
import com.qlytro.repository.HopDongRepository;
import com.qlytro.repository.KhachThueRepository;
import com.qlytro.repository.SuCoRepository;
import com.qlytro.security.CustomUserDetails;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/tenant")
public class TenantController {

    private final KhachThueRepository khachThueRepository;
    private final HopDongRepository hopDongRepository;
    private final HoaDonRepository hoaDonRepository;
    private final SuCoRepository suCoRepository;
    private final HopDongKhachThueRepository hdktRepository;
    private final com.qlytro.service.impl.HopDongService hopDongService;

    public TenantController(KhachThueRepository khachThueRepository,
                            HopDongRepository hopDongRepository,
                            HoaDonRepository hoaDonRepository,
                            SuCoRepository suCoRepository,
                            HopDongKhachThueRepository hdktRepository,
                            com.qlytro.service.impl.HopDongService hopDongService) {
        this.khachThueRepository = khachThueRepository;
        this.hopDongRepository = hopDongRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.suCoRepository = suCoRepository;
        this.hdktRepository = hdktRepository;
        this.hopDongService = hopDongService;
    }

    @GetMapping({"/dashboard", "/"})
    @Transactional(readOnly = true)
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("pageTitle", "Trang chủ");

        if (userDetails != null) {
            model.addAttribute("nguoiDung", userDetails.getNguoiDung());

            // Tim khach thue lien ket voi user dang dang nhap
            KhachThue khachThue = khachThueRepository
                    .findByNguoiDung_Id(userDetails.getId())
                    .orElse(null);

            if (khachThue != null) {
                // Tim hop dong active, neu khong co thi lay hop dong moi nhat
                HopDong hopDong = hopDongRepository.findActiveByKhachThueId(khachThue.getId())
                        .orElseGet(() -> {
                            var list = hopDongRepository.findByKhachThueIdLatestFirst(khachThue.getId());
                            return list.isEmpty() ? null : list.get(0);
                        });

                if (hopDong != null) {
                    var hopDongRes = hopDongService.findById(hopDong.getId());
                    model.addAttribute("hopDong", hopDongRes);
                    model.addAttribute("soPhong", hopDongRes.getSoPhong());
                    model.addAttribute("tenToaNha", hopDongRes.getTenToaNha());
                    model.addAttribute("danhSachDangO", hopDongRes.getDanhSachDangO());
                    model.addAttribute("danhSachDaRoiDi", hopDongRes.getDanhSachDaRoiDi());

                    // Canh bao no coc neu hop dong da ket thuc va tienCocConLai < 0
                    if (!com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE.equals(hopDongRes.getTrangThai())
                            && hopDongRes.getTienCocConLai() != null
                            && hopDongRes.getTienCocConLai().compareTo(java.math.BigDecimal.ZERO) < 0) {
                        model.addAttribute("noCocMsg", "Hợp đồng phòng " + hopDongRes.getSoPhong()
                                + " đã thanh lý. Tổng chi phí vượt quá tiền cọc, bạn còn thiếu nợ chủ trọ: "
                                + String.format("%,d", hopDongRes.getTienCocConLai().abs().longValue())
                                + " đ. Vui lòng kiểm tra và hoàn tất thanh toán các hóa đơn / bồi thường.");
                    }

                    // Dem hoa don chua thanh toan
                    long hoaDonChuaTT = hoaDonRepository
                            .findByHopDong_IdOrderByThangNamDesc(hopDong.getId())
                            .stream()
                            .filter(hd -> TrangThaiHoaDon.CHUA_THANH_TOAN.equals(hd.getTrangThai()))
                            .count();
                    model.addAttribute("soHoaDonChuaTT", hoaDonChuaTT);
                }

                // Dem su co dang xu ly cua user nay
                long suCoDangXuLy = suCoRepository.findByNguoiBao_Id(userDetails.getId())
                        .stream()
                        .filter(sc -> !com.qlytro.entity.enums.TrangThaiSuCo.HOAN_THANH.equals(sc.getTrangThai()))
                        .count();
                model.addAttribute("soSuCoDangXuLy", suCoDangXuLy);
            }
        }

        // Neu dashboard cua tenant dung layout/base chung:
        model.addAttribute("pageContent", "tenant/dashboard");
        return "layout/base";
    }

    @GetMapping("/hop-dong")
    @Transactional(readOnly = true)
    public String hopDong(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("pageTitle", "Hợp đồng của tôi");
        if (userDetails != null) {
            KhachThue khachThue = khachThueRepository
                    .findByNguoiDung_Id(userDetails.getId())
                    .orElse(null);

            if (khachThue != null) {
                HopDong hopDong = hopDongRepository.findActiveByKhachThueId(khachThue.getId())
                        .orElseGet(() -> {
                            var list = hopDongRepository.findByKhachThueIdLatestFirst(khachThue.getId());
                            return list.isEmpty() ? null : list.get(0);
                        });

                if (hopDong != null) {
                    var hopDongRes = hopDongService.findById(hopDong.getId());
                    model.addAttribute("hopDong", hopDongRes);
                    model.addAttribute("danhSachDangO", hopDongRes.getDanhSachDangO());
                    model.addAttribute("danhSachDaRoiDi", hopDongRes.getDanhSachDaRoiDi());
                    model.addAttribute("danhSachNguoiO", hopDongRes.getDanhSachDangO()); // Cho tuong thich cu
                } else {
                    model.addAttribute("infoMsg", "Bạn chưa có hợp đồng thuê phòng nào.");
                }
            } else {
                model.addAttribute("infoMsg", "Tài khoản của bạn chưa được liên kết với hồ sơ khách thuê.");
            }
        }
        model.addAttribute("pageContent", "tenant/hop-dong/detail");
        return "layout/base";
    }
}