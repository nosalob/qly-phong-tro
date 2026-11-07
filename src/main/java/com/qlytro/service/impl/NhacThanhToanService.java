package com.qlytro.service.impl;

import com.qlytro.dto.request.NhacThanhToanRequest;
import com.qlytro.dto.response.NhacThanhToanResponse;
import com.qlytro.entity.HoaDon;
import com.qlytro.entity.NguoiDung;
import com.qlytro.entity.NhacThanhToan;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.HoaDonRepository;
import com.qlytro.repository.NguoiDungRepository;
import com.qlytro.repository.NhacThanhToanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NhacThanhToanService {

    private final NhacThanhToanRepository nhacThanhToanRepository;
    private final HoaDonRepository hoaDonRepository;
    private final NguoiDungRepository nguoiDungRepository;

    public NhacThanhToanService(NhacThanhToanRepository nhacThanhToanRepository,
                                HoaDonRepository hoaDonRepository,
                                NguoiDungRepository nguoiDungRepository) {
        this.nhacThanhToanRepository = nhacThanhToanRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Transactional(readOnly = true)
    public List<NhacThanhToanResponse> findByHoaDonId(Integer hoaDonId) {
        return nhacThanhToanRepository.findByHoaDonIdWithNguoiDung(hoaDonId).stream()
                .map(this::toResponse)
                .toList();
    }

    public NhacThanhToanResponse ghiNhanNhacThanhToan(Integer hoaDonId, NhacThanhToanRequest request, Integer nguoiDungId) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn", hoaDonId));

        if (!TrangThaiHoaDon.CHUA_THANH_TOAN.equals(hoaDon.getTrangThai())) {
            throw new BusinessException("Chỉ được ghi nhận nhắc nhở đối với hóa đơn chưa thanh toán.");
        }

        NguoiDung nguoiDung = nguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", nguoiDungId));

        NhacThanhToan entity = NhacThanhToan.builder()
                .hoaDon(hoaDon)
                .nguoiDung(nguoiDung)
                .hinhThucLienHe(request.getHinhThuc())
                .noiDung(request.getNoiDung().trim())
                .build();

        entity = nhacThanhToanRepository.save(entity);

        // Giữ nguyên trạng thái CHUA_THANH_TOAN của hóa đơn
        return toResponse(entity);
    }

    private NhacThanhToanResponse toResponse(NhacThanhToan entity) {
        return NhacThanhToanResponse.builder()
                .id(entity.getId())
                .tenNguoiNhac(entity.getNguoiDung().getHoTen())
                .hinhThuc(entity.getHinhThucLienHe())
                .hinhThucLabel(entity.getHinhThucLienHe().getLabel())
                .noiDung(entity.getNoiDung())
                .thoiGianNhac(entity.getThoiGianNhac())
                .build();
    }
}

