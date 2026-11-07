package com.qlytro.entity;

import com.qlytro.entity.enums.HinhThucNhac;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nhac_thanh_toan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhacThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoa_don_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_nhac_tt_hoa_don"))
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_dung_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_nhac_tt_nguoi_dung"))
    private NguoiDung nguoiDung;

    @Column(name = "hinh_thuc_lien_he", nullable = false)
    private HinhThucNhac hinhThucLienHe;

    @Column(name = "noi_dung", nullable = false, length = 500)
    private String noiDung;

    @Column(name = "thoi_gian_nhac", nullable = false, updatable = false)
    private LocalDateTime thoiGianNhac;

    @PrePersist
    protected void onCreate() {
        if (thoiGianNhac == null) {
            thoiGianNhac = LocalDateTime.now();
        }
    }
}

