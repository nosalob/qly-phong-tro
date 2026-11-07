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
public class BieuDoDoanhThuItem {
    private String thangNam;        // "2026-01"
    private String label;           // "T1/2026"
    private BigDecimal daThu;       // Doanh thu thực tế đã thu
    private String daThuShort;      // "85M"
    private BigDecimal congNo;      // Công nợ
    private String congNoShort;     // "12M"
}

