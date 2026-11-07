package com.qlytro.dto.response;

import com.qlytro.entity.enums.TrangThaiPhong;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhongQuanLyResponse {
    private Integer id;
    private Integer toaNhaId;
    private String tenToaNha;
    private String soPhong;
    private BigDecimal dienTich;
    private BigDecimal giaThueTheoMacDinh;
    private TrangThaiPhong trangThai;
    private String trangThaiLabel;
    
    @Builder.Default
    private String tenNguoiThueDaiDien = "—";
    
    @Builder.Default
    private String ngayHetHanHopDong = "—";
    
    private Integer hopDongId;
    
    @Builder.Default
    private boolean canhBaoTrungHopDong = false;
}

