package com.qlytro.dto.response;

import com.qlytro.entity.enums.TrangThaiHoaDon;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HoaDonResponse {
    private Integer id;
    private Integer hopDongId;
    private String soPhong;
    private String tenToaNha;
    private String tenKhachDaiDien;
    private String sdtKhachDaiDien;
    private boolean coSnapshotDaiDien;
    private String ghiChuDaiDien;
    private String thangNam;
    private BigDecimal tongTien;
    private TrangThaiHoaDon trangThai;
    private String trangThaiLabel;
    private String anhChuyenKhoan;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayThanhToan;
    private String lyDoTuChoi;
    private java.time.LocalDate hanThanhToan;
    private boolean quaHan;
    private long soNgayQuaHan;
    private List<HoaDonChiTietResponse> danhSachChiTiet;
    private List<NhacThanhToanResponse> danhSachNhac;
}
