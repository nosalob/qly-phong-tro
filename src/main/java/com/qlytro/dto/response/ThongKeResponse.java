package com.qlytro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongKeResponse {
    private ThongKePhongResponse phong;
    private TaiChinhResponse taiChinh;
    private List<BieuDoDoanhThuItem> bieuDoDoanhThu;
    private TinhTrangHoaDonResponse tinhTrangHoaDon;
    private TongQuanNhanhResponse tongQuanNhanh;
}

