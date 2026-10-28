package com.qlytro.entity;

import com.qlytro.entity.enums.TrangThaiNguoiDung;
import com.qlytro.entity.enums.VaiTroNguoiDung;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Luon luu dang BCrypt hash, KHONG luu plain text. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "sdt", length = 15)
    private String sdt;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "vai_tro", nullable = false)
    private VaiTroNguoiDung vaiTro;

    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private TrangThaiNguoiDung trangThai = TrangThaiNguoiDung.ACTIVE;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }
}
