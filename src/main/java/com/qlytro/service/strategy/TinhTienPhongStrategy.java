package com.qlytro.service.strategy;

import com.qlytro.entity.HoaDonChiTiet;
import com.qlytro.entity.enums.LoaiKhoanChiTiet;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Chien luoc tinh tien phong.
 * Lay tien_thue truc tiep tu hop_dong (da copy tu gia niem yet luc ky hop dong).
 * Quy tac ngay 20/21:
 * - Hoa don thang dau tien cua hop dong (thang hoa don trung voi thang cua ngayBatDau):
 *   + Neu ngayBatDau tu ngay 1 den 20: tinh du tien phong.
 *   + Neu ngayBatDau tu ngay 21 tro di: KHONG tinh tien phong thang nay (tra ve List.of()).
 * - Cac thang tiep theo: tinh du tien phong binh thuong.
 */
@Order(1)
@Component
public class TinhTienPhongStrategy implements TinhTienStrategy {

    @Override
    public List<HoaDonChiTiet> tinh(TinhTienContext context) {
        var hopDong = context.getHopDong();
        String thangHienTaiStr = getThangHienTai(context);

        if (hopDong.getNgayBatDau() != null) {
            LocalDate ngayBatDau = hopDong.getNgayBatDau();
            YearMonth thangBatDau = YearMonth.from(ngayBatDau);
            YearMonth thangHoaDon = YearMonth.parse(thangHienTaiStr);

            // Kiem tra neu la hoa don thang dau tien va vao o tu ngay 21 tro di -> mien tien phong
            if (thangHoaDon.equals(thangBatDau) && ngayBatDau.getDayOfMonth() >= 21) {
                return List.of();
            }
        }

        var chiTiet = HoaDonChiTiet.builder()
                .loaiKhoan(LoaiKhoanChiTiet.TIEN_PHONG)
                .noiDung("Tiền thuê phòng tháng " + thangHienTaiStr)
                .soLuong(null)
                .donGia(null)
                .thanhTien(hopDong.getTienThue())
                .build();
        return List.of(chiTiet);
    }

    private String getThangHienTai(TinhTienContext context) {
        if (context.getThangNam() != null) {
            return context.getThangNam();
        }
        if (context.getChiSoDienNuoc() != null) {
            return context.getChiSoDienNuoc().getThangNam();
        }
        return java.time.YearMonth.now().toString();
    }
}

