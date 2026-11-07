package com.qlytro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDonQuaHanResponse {
    private Integer id;
    private Integer hopDongId;
    private String thangNam;
    private String soPhong;
    private String tenToaNha;
    private String tenKhachDaiDien;
    private String sdtKhachDaiDien;
    private boolean coSnapshotDaiDien;
    private String ghiChuDaiDien;
    private BigDecimal tongTien;
    private LocalDate hanThanhToan;
    private long soNgayQuaHan;
    private long soLanNhac;
    private LocalDateTime thoiGianNhacCuoi;
}

