package com.qlytro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinhTrangHoaDonResponse {
    private long daThanhToanCount;
    private double daThanhToanPct;

    private long chuaThanhToanCount; // CHUA_THANH_TOAN chưa quá hạn
    private double chuaThanhToanPct;

    private long choDuyetCount;
    private double choDuyetPct;

    private long quaHanCount;        // CHUA_THANH_TOAN quá hạn
    private double quaHanPct;

    private long tongSo;
}

