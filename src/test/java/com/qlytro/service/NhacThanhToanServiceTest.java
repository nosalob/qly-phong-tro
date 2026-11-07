package com.qlytro.service;

import com.qlytro.dto.request.NhacThanhToanRequest;
import com.qlytro.dto.response.NhacThanhToanResponse;
import com.qlytro.entity.HoaDon;
import com.qlytro.entity.NguoiDung;
import com.qlytro.entity.NhacThanhToan;
import com.qlytro.entity.enums.HinhThucNhac;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.exception.BusinessException;
import com.qlytro.repository.HoaDonRepository;
import com.qlytro.repository.NguoiDungRepository;
import com.qlytro.repository.NhacThanhToanRepository;
import com.qlytro.service.impl.NhacThanhToanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NhacThanhToanServiceTest {

    @Mock
    private NhacThanhToanRepository nhacThanhToanRepository;

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private NguoiDungRepository nguoiDungRepository;

    private NhacThanhToanService nhacThanhToanService;

    private HoaDon hoaDonChuaTT;
    private HoaDon hoaDonDaTT;
    private NguoiDung chuTro;

    @BeforeEach
    void setUp() {
        nhacThanhToanService = new NhacThanhToanService(
                nhacThanhToanRepository,
                hoaDonRepository,
                nguoiDungRepository
        );

        hoaDonChuaTT = HoaDon.builder()
                .id(10)
                .trangThai(TrangThaiHoaDon.CHUA_THANH_TOAN)
                .thangNam("2026-11")
                .build();

        hoaDonDaTT = HoaDon.builder()
                .id(11)
                .trangThai(TrangThaiHoaDon.DA_THANH_TOAN)
                .thangNam("2026-10")
                .build();

        chuTro = NguoiDung.builder()
                .id(1)
                .username("admin")
                .hoTen("Chủ Trọ Nguyễn")
                .build();
    }

    @Test
    @DisplayName("Ghi nhận nhắc nợ: Cho phép khi hóa đơn CHUA_THANH_TOAN và không đổi trạng thái HĐ")
    void testGhiNhanNhacThanhToan_ChuaThanhToan_ThanhCong() {
        NhacThanhToanRequest request = NhacThanhToanRequest.builder()
                .hinhThuc(HinhThucNhac.GOI_DIEN)
                .noiDung("Đã gọi điện cho khách hẹn chiều nay chuyển khoản")
                .build();

        when(hoaDonRepository.findById(10)).thenReturn(Optional.of(hoaDonChuaTT));
        when(nguoiDungRepository.findById(1)).thenReturn(Optional.of(chuTro));

        NhacThanhToan savedEntity = NhacThanhToan.builder()
                .id(100)
                .hoaDon(hoaDonChuaTT)
                .nguoiDung(chuTro)
                .hinhThucLienHe(HinhThucNhac.GOI_DIEN)
                .noiDung("Đã gọi điện cho khách hẹn chiều nay chuyển khoản")
                .thoiGianNhac(LocalDateTime.now())
                .build();

        when(nhacThanhToanRepository.save(any(NhacThanhToan.class))).thenReturn(savedEntity);

        NhacThanhToanResponse response = nhacThanhToanService.ghiNhanNhacThanhToan(10, request, 1);

        assertNotNull(response);
        assertEquals("Chủ Trọ Nguyễn", response.getTenNguoiNhac());
        assertEquals(HinhThucNhac.GOI_DIEN, response.getHinhThuc());
        assertEquals("Gọi điện", response.getHinhThucLabel());
        assertEquals("Đã gọi điện cho khách hẹn chiều nay chuyển khoản", response.getNoiDung());

        // Đảm bảo trạng thái hóa đơn KHÔNG bị thay đổi
        assertEquals(TrangThaiHoaDon.CHUA_THANH_TOAN, hoaDonChuaTT.getTrangThai());
        verify(hoaDonRepository, never()).save(any(HoaDon.class));

        ArgumentCaptor<NhacThanhToan> captor = ArgumentCaptor.forClass(NhacThanhToan.class);
        verify(nhacThanhToanRepository).save(captor.capture());
        assertEquals(HinhThucNhac.GOI_DIEN, captor.getValue().getHinhThucLienHe());
        assertEquals("Đã gọi điện cho khách hẹn chiều nay chuyển khoản", captor.getValue().getNoiDung());
    }

    @Test
    @DisplayName("Ghi nhận nhắc nợ: Chặn khi hóa đơn đã ở trạng thái DA_THANH_TOAN")
    void testGhiNhanNhacThanhToan_DaThanhToan_NemLoi() {
        NhacThanhToanRequest request = NhacThanhToanRequest.builder()
                .hinhThuc(HinhThucNhac.TIN_NHAN)
                .noiDung("Nhắc nợ")
                .build();

        when(hoaDonRepository.findById(11)).thenReturn(Optional.of(hoaDonDaTT));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                nhacThanhToanService.ghiNhanNhacThanhToan(11, request, 1)
        );
        assertTrue(ex.getMessage().contains("Chỉ được ghi nhận nhắc nhở đối với hóa đơn chưa thanh toán"));
        verify(nhacThanhToanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy lịch sử nhắc nhở theo hóa đơn ID")
    void testFindByHoaDonId() {
        NhacThanhToan entity = NhacThanhToan.builder()
                .id(1)
                .hoaDon(hoaDonChuaTT)
                .nguoiDung(chuTro)
                .hinhThucLienHe(HinhThucNhac.TRUC_TIEP)
                .noiDung("Đã gặp trực tiếp tại phòng")
                .thoiGianNhac(LocalDateTime.now())
                .build();

        when(nhacThanhToanRepository.findByHoaDonIdWithNguoiDung(10)).thenReturn(List.of(entity));

        List<NhacThanhToanResponse> result = nhacThanhToanService.findByHoaDonId(10);
        assertEquals(1, result.size());
        assertEquals(HinhThucNhac.TRUC_TIEP, result.get(0).getHinhThuc());
        assertEquals("Trực tiếp", result.get(0).getHinhThucLabel());
    }
}

