package com.qlytro.dto.response;

import com.qlytro.entity.enums.TrangThaiCoc;
import com.qlytro.entity.enums.TrangThaiHopDong;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HopDongResponse {
    private Integer id;
    private Integer phongId;
    private String soPhong;
    private String tenToaNha;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private BigDecimal tienThue;
    private BigDecimal tienCoc;
    private BigDecimal tienCocConLai;
    private TrangThaiHopDong trangThai;
    private String trangThaiLabel;
    private TrangThaiCoc trangThaiCoc;
    private String trangThaiCocLabel;
    private LocalDateTime ngayTao;
    private List<KhachThueResponse> danhSachKhach;
    private KhachThueResponse daiDien;

    public List<KhachThueResponse> getDanhSachDangO() {
        if (danhSachKhach == null) return java.util.List.of();
        return danhSachKhach.stream().filter(k -> Boolean.TRUE.equals(k.getDangO())).toList();
    }

    public List<KhachThueResponse> getDanhSachDaRoiDi() {
        if (danhSachKhach == null) return java.util.List.of();
        return danhSachKhach.stream().filter(k -> !Boolean.TRUE.equals(k.getDangO())).toList();
    }
}
