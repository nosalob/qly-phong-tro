package com.qlytro.service.strategy;

import com.qlytro.entity.HoaDonChiTiet;
import com.qlytro.entity.enums.LoaiKhoanChiTiet;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

/**
 * Chien luoc tinh tien nuoc.
 * Cong thuc: (chi_so_nuoc_moi - chi_so_nuoc_cu) * don_gia_nuoc
 * Gia tri don_gia duoc COPY vao hoa_don_chi_tiet tai thoi diem tinh (khong tham chieu ngoc).
 */
@Order(3)
@Component
public class TinhTienNuocStrategy implements TinhTienStrategy {

    @Override
    public List<HoaDonChiTiet> tinh(TinhTienContext context) {
        var chiSo = context.getChiSoDienNuoc();
        var bangGia = context.getBangGiaDienNuoc();

        if (chiSo == null || bangGia == null || chiSo.getChiSoNuocMoi() == null || chiSo.getChiSoNuocCu() == null) {
            return List.of();
        }

        BigDecimal soLuong = chiSo.getChiSoNuocMoi().subtract(chiSo.getChiSoNuocCu());
        BigDecimal donGia = bangGia.getDonGiaNuoc();
        BigDecimal thanhTien = soLuong.multiply(donGia);

        var chiTiet = HoaDonChiTiet.builder()
                .loaiKhoan(LoaiKhoanChiTiet.TIEN_NUOC)
                .chiSoDienNuoc(chiSo)
                .noiDung(String.format("Tiền nước tháng %s (%s - %s = %s khối)",
                        chiSo.getThangNam(),
                        chiSo.getChiSoNuocCu().stripTrailingZeros().toPlainString(),
                        chiSo.getChiSoNuocMoi().stripTrailingZeros().toPlainString(),
                        soLuong.stripTrailingZeros().toPlainString()))
                .soLuong(soLuong)
                .donGia(donGia)
                .thanhTien(thanhTien)
                .build();
        return List.of(chiTiet);
    }
}
