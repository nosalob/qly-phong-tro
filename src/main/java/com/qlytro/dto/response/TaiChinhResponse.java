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
public class TaiChinhResponse {
    private BigDecimal doanhThuDaThu;
    private String doanhThuDaThuFormatted;
    private Double phanTramSoVoiKyTruoc;
    private BigDecimal congNoHienTai;
    private String congNoHienTaiFormatted;
    private long soHoaDonChuaThanhToan;
}

