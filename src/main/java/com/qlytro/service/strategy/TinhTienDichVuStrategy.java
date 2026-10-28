package com.qlytro.service.strategy;

import com.qlytro.entity.HoaDonChiTiet;
import com.qlytro.entity.PhiDichVu;
import com.qlytro.entity.enums.LoaiKhoanChiTiet;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Chien luoc tinh phi dich vu.
 * Tao 1 dong chi tiet cho MOI loai phi dang_ap_dung=true.
 * (VD: phi rac, phi internet, phi gui xe — moi cai 1 dong rieng).
 */
@Order(4)
@Component
public class TinhTienDichVuStrategy implements TinhTienStrategy {

    @Override
    public List<HoaDonChiTiet> tinh(TinhTienContext context) {
        List<PhiDichVu> danhSachPhi = context.getDanhSachPhiDichVu();
        List<HoaDonChiTiet> result = new ArrayList<>();

        for (PhiDichVu phi : danhSachPhi) {
            // Chi tinh phi dang duoc ap dung
            if (Boolean.TRUE.equals(phi.getDangApDung())) {
                var chiTiet = HoaDonChiTiet.builder()
                        .loaiKhoan(LoaiKhoanChiTiet.PHI_DICH_VU)
                        .noiDung(phi.getTenPhi())
                        .soLuong(null)
                        .donGia(null)
                        .thanhTien(phi.getSoTien())
                        .build();
                result.add(chiTiet);
            }
        }
        return result;
    }
}
