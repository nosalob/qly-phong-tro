package com.qlytro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongViecResponse {
    private long soHoaDonChoDuyet;
    private long soSuCoChuaXuLy;
    private long soPhongTrong;
    private long soHopDongSapHetHan;
    private long soHoaDonQuaHan;
}

