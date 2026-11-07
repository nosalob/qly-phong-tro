package com.qlytro.service.impl;

import com.qlytro.dto.response.CongViecResponse;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.entity.enums.TrangThaiHopDong;
import com.qlytro.entity.enums.TrangThaiPhong;
import com.qlytro.entity.enums.TrangThaiSuCo;
import com.qlytro.repository.HoaDonRepository;
import com.qlytro.repository.HopDongRepository;
import com.qlytro.repository.PhongRepository;
import com.qlytro.repository.SuCoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class CongViecService {

    private final HoaDonRepository hoaDonRepository;
    private final SuCoRepository suCoRepository;
    private final PhongRepository phongRepository;
    private final HopDongRepository hopDongRepository;

    public CongViecService(HoaDonRepository hoaDonRepository,
                           SuCoRepository suCoRepository,
                           PhongRepository phongRepository,
                           HopDongRepository hopDongRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.suCoRepository = suCoRepository;
        this.phongRepository = phongRepository;
        this.hopDongRepository = hopDongRepository;
    }

    public CongViecResponse getTongHopCongViec() {
        long soHoaDonChoDuyet = hoaDonRepository.countByTrangThai(TrangThaiHoaDon.CHO_DUYET);
        long soSuCoChuaXuLy = suCoRepository.countByTrangThaiNot(TrangThaiSuCo.HOAN_THANH);
        long soPhongTrong = phongRepository.countByTrangThai(TrangThaiPhong.TRONG);
        long soHopDongSapHetHan = hopDongRepository.countSapHetHan(TrangThaiHopDong.ACTIVE, LocalDate.now().plusDays(30));

        LocalDate today = LocalDate.now();
        String thangNamMoc = today.getDayOfMonth() >= 6
                ? java.time.YearMonth.from(today).toString()
                : java.time.YearMonth.from(today).minusMonths(1).toString();
        long soHoaDonQuaHan = hoaDonRepository.countHoaDonQuaHan(TrangThaiHoaDon.CHUA_THANH_TOAN, thangNamMoc);

        return CongViecResponse.builder()
                .soHoaDonChoDuyet(soHoaDonChoDuyet)
                .soSuCoChuaXuLy(soSuCoChuaXuLy)
                .soPhongTrong(soPhongTrong)
                .soHopDongSapHetHan(soHopDongSapHetHan)
                .soHoaDonQuaHan(soHoaDonQuaHan)
                .build();
    }
}

