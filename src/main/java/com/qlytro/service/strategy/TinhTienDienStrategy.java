package com.qlytro.service.strategy;

import com.qlytro.entity.HoaDonChiTiet;
import com.qlytro.entity.enums.LoaiKhoanChiTiet;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

/**
 * Chien luoc tinh tien dien.
 * Cong thuc: (chi_so_dien_moi - chi_so_dien_cu) * don_gia_dien
 * Gia tri don_gia duoc COPY vao hoa_don_chi_tiet tai thoi diem tinh (khong tham chieu ngoc).
 */
@Order(2)
@Component
public class TinhTienDienStrategy implements TinhTienStrategy {

    @Override
    public List<HoaDonChiTiet> tinh(TinhTienContext context) {
        var chiSo = context.getChiSoDienNuoc();
        var bangGia = context.getBangGiaDienNuoc();

        if (chiSo == null || bangGia == null || chiSo.getChiSoDienMoi() == null || chiSo.getChiSoDienCu() == null) {
            return List.of();
        }

        BigDecimal soLuong = chiSo.getChiSoDienMoi().subtract(chiSo.getChiSoDienCu());
        BigDecimal donGia = bangGia.getDonGiaDien();
        BigDecimal thanhTien = soLuong.multiply(donGia);

        var chiTiet = HoaDonChiTiet.builder()
                .loaiKhoan(LoaiKhoanChiTiet.TIEN_DIEN)
                .chiSoDienNuoc(chiSo)
                .noiDung(String.format("Tiền điện tháng %s (%s - %s = %s số)",
                        chiSo.getThangNam(),
                        chiSo.getChiSoDienCu().stripTrailingZeros().toPlainString(),
                        chiSo.getChiSoDienMoi().stripTrailingZeros().toPlainString(),
                        soLuong.stripTrailingZeros().toPlainString()))
                .soLuong(soLuong)
                .donGia(donGia)
                .thanhTien(thanhTien)
                .build();
        return List.of(chiTiet);
    }
}
