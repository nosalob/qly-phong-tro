package com.qlytro.config;

import com.qlytro.entity.NguoiDung;
import com.qlytro.entity.BangGiaDienNuoc;
import com.qlytro.entity.enums.TrangThaiNguoiDung;
import com.qlytro.entity.enums.VaiTroNguoiDung;
import com.qlytro.repository.BangGiaDienNuocRepository;
import com.qlytro.repository.NguoiDungRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;

/**
 * Khoi tao du lieu mac dinh khi ung dung chay lan dau.
 * - Tai khoan chu tro mac dinh: admin / admin@123
 * - Bang gia dien nuoc mac dinh neu chua co.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @org.springframework.beans.factory.annotation.Value("${app.admin.initial-password:admin@123}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initData(NguoiDungRepository nguoiDungRepo,
                                      BangGiaDienNuocRepository bangGiaRepo,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // --- Tai khoan chu tro mac dinh ---
            if (!nguoiDungRepo.existsByUsername("admin")) {
                NguoiDung admin = NguoiDung.builder()
                        .username("admin")
                        .password(passwordEncoder.encode(adminPassword))
                        .hoTen("Chủ Trọ Admin")
                        .vaiTro(VaiTroNguoiDung.CHU_TRO)
                        .trangThai(TrangThaiNguoiDung.ACTIVE)
                        .mustChangePassword(false)
                        .build();
                nguoiDungRepo.save(admin);
                log.info("✅ Đã tạo tài khoản admin mặc định: admin");
            }

            // --- Bang gia dien nuoc mac dinh ---
            if (bangGiaRepo.count() == 0) {
                BangGiaDienNuoc bangGia = BangGiaDienNuoc.builder()
                        .donGiaDien(new BigDecimal("4000.00"))   // 4,000 đ/số
                        .donGiaNuoc(new BigDecimal("35000.00"))  // 35,000 đ/khối
                        .build();
                bangGiaRepo.save(bangGia);
                log.info("✅ Đã tạo bảng giá điện nước mặc định");
            }
        };
    }
}
