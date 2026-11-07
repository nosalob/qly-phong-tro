package com.qlytro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TongQuanNhanhResponse {
    private BigDecimal doanhThuThangNay;
    private String doanhThuThangNayShort;
    private Double doanhThuGrowth;

    private long tongHoaDon;
    private Double tongHoaDonGrowth;

    private long khachThueHienTai;
    private Double khachThueGrowth;

    private String ngayCapNhat;
}

