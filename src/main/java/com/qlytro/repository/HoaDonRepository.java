package com.qlytro.repository;

import com.qlytro.entity.HoaDon;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    List<HoaDon> findByHopDong_Id(Integer hopDongId);
    Optional<HoaDon> findByHopDong_IdAndThangNam(Integer hopDongId, String thangNam);
    boolean existsByHopDong_IdAndThangNam(Integer hopDongId, String thangNam);
    List<HoaDon> findByTrangThai(TrangThaiHoaDon trangThai);
    long countByTrangThai(TrangThaiHoaDon trangThai);
    List<HoaDon> findByHopDong_IdOrderByThangNamDesc(Integer hopDongId);

    @Query("SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h WHERE h.trangThai = :trangThai AND h.thangNam = :thangNam")
    BigDecimal sumTongTienByTrangThaiAndThangNam(@Param("trangThai") TrangThaiHoaDon trangThai, @Param("thangNam") String thangNam);

    @Query("SELECT h FROM HoaDon h JOIN FETCH h.hopDong hd JOIN FETCH hd.phong p JOIN FETCH p.toaNha WHERE h.trangThai <> :trangThai")
    List<HoaDon> findByTrangThaiNotFetchHopDongAndPhong(@Param("trangThai") TrangThaiHoaDon trangThai);

    List<HoaDon> findByTrangThaiNot(TrangThaiHoaDon trangThai);
    // 1. Lay tat ca hoa don, uu tien cho duyet va chua thanh toan
    @Query("""
        SELECT h FROM HoaDon h
        ORDER BY
            CASE h.trangThai
                WHEN com.qlytro.entity.enums.TrangThaiHoaDon.CHO_DUYET THEN 0
                WHEN com.qlytro.entity.enums.TrangThaiHoaDon.CHUA_THANH_TOAN THEN 1
                WHEN com.qlytro.entity.enums.TrangThaiHoaDon.DA_THANH_TOAN THEN 2
                ELSE 3
            END,
            h.thangNam DESC,
            h.id DESC
        """)
    List<HoaDon> findAllOrderByTrangThaiAndThangNam();
    // 2. Loc hoa don theo toa nha, phong, khoang thang va trang thai
    @Query("""
        SELECT h FROM HoaDon h
        JOIN FETCH h.hopDong hd
        JOIN FETCH hd.phong p
        JOIN FETCH p.toaNha tn
        WHERE (:toaNhaId IS NULL OR tn.id = :toaNhaId)
          AND (:phongId IS NULL OR p.id = :phongId)
          AND (:tuThang IS NULL OR :tuThang = '' OR h.thangNam >= :tuThang)
          AND (:denThang IS NULL OR :denThang = '' OR h.thangNam <= :denThang)
          AND (:trangThai IS NULL OR h.trangThai = :trangThai)
        ORDER BY
            CASE h.trangThai
                WHEN com.qlytro.entity.enums.TrangThaiHoaDon.CHO_DUYET THEN 0
                WHEN com.qlytro.entity.enums.TrangThaiHoaDon.CHUA_THANH_TOAN THEN 1
                WHEN com.qlytro.entity.enums.TrangThaiHoaDon.DA_THANH_TOAN THEN 2
                ELSE 3
            END,
            h.thangNam DESC,
            h.id DESC
        """)
    List<HoaDon> findAllFiltered(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("phongId") Integer phongId,
            @Param("tuThang") String tuThang,
            @Param("denThang") String denThang,
            @Param("trangThai") TrangThaiHoaDon trangThai
    );

    @Query(value = """
        SELECT h FROM HoaDon h
        JOIN FETCH h.hopDong hd
        JOIN FETCH hd.phong p
        JOIN FETCH p.toaNha tn
        LEFT JOIN FETCH h.nguoiDaiDien ndd
        WHERE h.trangThai = :trangThai
          AND h.thangNam <= :thangNamMoc
          AND (:toaNhaId IS NULL OR tn.id = :toaNhaId)
        ORDER BY h.thangNam ASC, p.soPhong ASC, h.id ASC
        """,
        countQuery = """
        SELECT COUNT(h) FROM HoaDon h
        JOIN h.hopDong hd
        JOIN hd.phong p
        JOIN p.toaNha tn
        WHERE h.trangThai = :trangThai
          AND h.thangNam <= :thangNamMoc
          AND (:toaNhaId IS NULL OR tn.id = :toaNhaId)
        """)
    Page<HoaDon> findHoaDonQuaHanPage(
            @Param("trangThai") TrangThaiHoaDon trangThai,
            @Param("thangNamMoc") String thangNamMoc,
            @Param("toaNhaId") Integer toaNhaId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(h) FROM HoaDon h
        WHERE h.trangThai = :trangThai
          AND h.thangNam <= :thangNamMoc
        """)
    long countHoaDonQuaHan(
            @Param("trangThai") TrangThaiHoaDon trangThai,
            @Param("thangNamMoc") String thangNamMoc
    );

    @Query("""
        SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h
        JOIN h.hopDong hd
        JOIN hd.phong p
        WHERE h.trangThai = com.qlytro.entity.enums.TrangThaiHoaDon.DA_THANH_TOAN
          AND (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
          AND h.ngayThanhToan >= :tuNgay
          AND h.ngayThanhToan <= :denNgay
    """)
    BigDecimal sumDoanhThuDaThu(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay
    );

    @Query("""
        SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h
        JOIN h.hopDong hd
        JOIN hd.phong p
        WHERE h.trangThai = com.qlytro.entity.enums.TrangThaiHoaDon.CHUA_THANH_TOAN
          AND (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
    """)
    BigDecimal sumCongNoHienTai(@Param("toaNhaId") Integer toaNhaId);

    @Query("""
        SELECT COUNT(h) FROM HoaDon h
        JOIN h.hopDong hd
        JOIN hd.phong p
        WHERE h.trangThai = com.qlytro.entity.enums.TrangThaiHoaDon.CHUA_THANH_TOAN
          AND (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
    """)
    long countHoaDonChuaThanhToan(@Param("toaNhaId") Integer toaNhaId);

    @Query("""
        SELECT h FROM HoaDon h
        JOIN FETCH h.hopDong hd
        JOIN FETCH hd.phong p
        WHERE h.trangThai = com.qlytro.entity.enums.TrangThaiHoaDon.DA_THANH_TOAN
          AND (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
          AND h.ngayThanhToan >= :tuNgay
          AND h.ngayThanhToan <= :denNgay
    """)
    List<HoaDon> findDaThanhToanInPeriod(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay
    );

    @Query("""
        SELECT h.thangNam, COALESCE(SUM(h.tongTien), 0) FROM HoaDon h
        JOIN h.hopDong hd
        JOIN hd.phong p
        WHERE h.trangThai = com.qlytro.entity.enums.TrangThaiHoaDon.CHUA_THANH_TOAN
          AND (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
          AND h.thangNam IN :thangNams
        GROUP BY h.thangNam
    """)
    List<Object[]> sumCongNoByThangNams(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("thangNams") List<String> thangNams
    );

    @Query("""
        SELECT h.trangThai, h.thangNam, COUNT(h) FROM HoaDon h
        JOIN h.hopDong hd
        JOIN hd.phong p
        WHERE (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
          AND (:tuThang IS NULL OR :tuThang = '' OR h.thangNam >= :tuThang)
          AND (:denThang IS NULL OR :denThang = '' OR h.thangNam <= :denThang)
        GROUP BY h.trangThai, h.thangNam
    """)
    List<Object[]> countHoaDonGroupByTrangThaiAndThangNam(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("tuThang") String tuThang,
            @Param("denThang") String denThang
    );

    @Query("""
        SELECT COUNT(h) FROM HoaDon h
        JOIN h.hopDong hd
        JOIN hd.phong p
        WHERE (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
          AND (:tuThang IS NULL OR :tuThang = '' OR h.thangNam >= :tuThang)
          AND (:denThang IS NULL OR :denThang = '' OR h.thangNam <= :denThang)
    """)
    long countTotalHoaDon(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("tuThang") String tuThang,
            @Param("denThang") String denThang
    );
}

