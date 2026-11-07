package com.qlytro.entity;

import com.qlytro.entity.enums.TrangThaiHoaDon;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hoa_don",
       uniqueConstraints = @UniqueConstraint(name = "uq_hoa_don",
                                              columnNames = {"hop_dong_id", "thang_nam"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Gan voi hop dong (khong phai phong) vi tien phong phu thuoc vao hop_dong.tien_thue. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hop_dong_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_hoa_don_hop_dong"))
    private HopDong hopDong;

    /**
     * Snapshot nguoi dai dien tai thoi diem lap hoa don.
     * NULL doi voi hoa don cu truoc migration (du lieu lich su).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dai_dien_id",
                foreignKey = @ForeignKey(name = "fk_hoa_don_nguoi_dai_dien"))
    private KhachThue nguoiDaiDien;

    /** Format: 'YYYY-MM', VD: 2026-09 */
    @Column(name = "thang_nam", nullable = false, length = 7)
    private String thangNam;

    @Column(name = "tong_tien", nullable = false, precision = 12, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private TrangThaiHoaDon trangThai = TrangThaiHoaDon.CHUA_THANH_TOAN;

    /** NULLABLE: Anh chuyen khoan khi khach nop. */
    @Column(name = "anh_chuyen_khoan", length = 255)
    private String anhChuyenKhoan;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    /** NULLABLE: Chi co gia tri khi trang_thai = DA_THANH_TOAN. */
    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }
}
