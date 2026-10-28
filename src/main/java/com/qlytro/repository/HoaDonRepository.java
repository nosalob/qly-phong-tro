package com.qlytro.repository;

import com.qlytro.entity.HoaDon;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    List<HoaDon> findByHopDong_Id(Integer hopDongId);
    Optional<HoaDon> findByHopDong_IdAndThangNam(Integer hopDongId, String thangNam);
    boolean existsByHopDong_IdAndThangNam(Integer hopDongId, String thangNam);
    List<HoaDon> findByTrangThai(TrangThaiHoaDon trangThai);
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
}

