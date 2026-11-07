package com.qlytro.repository;

import com.qlytro.entity.HopDong;
import com.qlytro.entity.enums.TrangThaiHopDong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HopDongRepository extends JpaRepository<HopDong, Integer> {

    List<HopDong> findByPhong_Id(Integer phongId);

    /** Tim hop dong dang ACTIVE cua 1 phong (dung khi tao hoa don / kiem tra). */
    Optional<HopDong> findByPhong_IdAndTrangThai(Integer phongId, TrangThaiHopDong trangThai);

    List<HopDong> findByTrangThai(TrangThaiHopDong trangThai);

    /**
     * Tim hop dong ACTIVE cua 1 khach thue (qua bang trung gian hdkt).
     * Dung trong SuCoService de lay phong_id tu tai khoan dang dang nhap.
     */
    @Query("""
            SELECT hd FROM HopDong hd
            JOIN FETCH hd.phong p
            JOIN FETCH p.toaNha tn
            JOIN HopDongKhachThue hdkt ON hdkt.hopDong = hd
            WHERE hdkt.khachThue.id = :khachThueId
              AND hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE
              AND hdkt.dangO = true
            ORDER BY hd.id DESC
            """)
    List<HopDong> findAllActiveByKhachThueId(@Param("khachThueId") Integer khachThueId);

    default Optional<HopDong> findActiveByKhachThueId(Integer khachThueId) {
        List<HopDong> list = findAllActiveByKhachThueId(khachThueId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Lay tat ca hop dong (ca active lan da ket thuc) cua khach thue, uu tien moi nhat truoc. */
    @Query("""
            SELECT hd FROM HopDong hd
            JOIN FETCH hd.phong p
            JOIN FETCH p.toaNha tn
            JOIN HopDongKhachThue hdkt ON hdkt.hopDong = hd
            WHERE hdkt.khachThue.id = :khachThueId
            ORDER BY hd.id DESC
            """)
    List<HopDong> findByKhachThueIdLatestFirst(@Param("khachThueId") Integer khachThueId);

    boolean existsByPhong_IdAndTrangThai(Integer phongId, TrangThaiHopDong trangThai);
    boolean existsByPhong_Id(Integer phongId);

    @Query("""
            SELECT hd FROM HopDong hd
            JOIN FETCH hd.phong p
            WHERE p.toaNha.id = :toaNhaId
              AND hd.trangThai = :trangThai
            """)
    List<HopDong> findActiveByToaNhaId(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("trangThai") TrangThaiHopDong trangThai
    );

    default List<HopDong> findActiveByToaNhaId(Integer toaNhaId) {
        return findActiveByToaNhaId(toaNhaId, TrangThaiHopDong.ACTIVE);
    }

    @Query("""
            SELECT COUNT(hd) FROM HopDong hd
            WHERE hd.trangThai = :trangThai
              AND hd.ngayKetThuc IS NOT NULL
              AND hd.ngayKetThuc <= :hanChot
            """)
    long countSapHetHan(
            @Param("trangThai") TrangThaiHopDong trangThai,
            @Param("hanChot") LocalDate hanChot
    );

    @Query("""
            SELECT hd FROM HopDong hd
            JOIN FETCH hd.phong p
            JOIN FETCH p.toaNha tn
            WHERE hd.trangThai = :trangThai
              AND hd.ngayKetThuc IS NOT NULL
              AND hd.ngayKetThuc <= :hanChot
            ORDER BY hd.ngayKetThuc ASC, hd.id ASC
            """)
    List<HopDong> findSapHetHan(
            @Param("trangThai") TrangThaiHopDong trangThai,
            @Param("hanChot") LocalDate hanChot
    );

    @Query("""
            SELECT hd FROM HopDong hd
            JOIN FETCH hd.phong p
            WHERE p.id IN :phongIds
              AND hd.trangThai = :trangThai
            """)
    List<HopDong> findActiveByPhongIds(
            @Param("phongIds") List<Integer> phongIds,
            @Param("trangThai") TrangThaiHopDong trangThai
    );

    @Query(
        value = """
            SELECT hd FROM HopDong hd
            JOIN FETCH hd.phong p
            JOIN FETCH p.toaNha tn
            WHERE (:filter = 'ALL'
                   OR (:filter = 'ACTIVE' AND hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE)
                   OR (:filter = 'SAP_HET_HAN' AND hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE AND hd.ngayKetThuc IS NOT NULL AND hd.ngayKetThuc <= :hanChot)
                   OR (:filter = 'DA_KET_THUC' AND (hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.KET_THUC_BINH_THUONG OR hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.KET_THUC_BAT_THUONG))
                  )
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(p.soPhong) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(tn.tenToaNha) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR EXISTS (
                       SELECT 1 FROM HopDongKhachThue hdkt
                       WHERE hdkt.hopDong = hd
                         AND hdkt.laDaiDien = true
                         AND LOWER(hdkt.khachThue.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   )
                  )
            ORDER BY
                CASE hd.trangThai
                    WHEN com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE THEN 0
                    WHEN com.qlytro.entity.enums.TrangThaiHopDong.KET_THUC_BINH_THUONG THEN 1
                    WHEN com.qlytro.entity.enums.TrangThaiHopDong.KET_THUC_BAT_THUONG THEN 2
                    ELSE 3
                END ASC,
                hd.ngayBatDau DESC,
                hd.id DESC
            """,
        countQuery = """
            SELECT COUNT(hd) FROM HopDong hd
            JOIN hd.phong p
            JOIN p.toaNha tn
            WHERE (:filter = 'ALL'
                   OR (:filter = 'ACTIVE' AND hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE)
                   OR (:filter = 'SAP_HET_HAN' AND hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE AND hd.ngayKetThuc IS NOT NULL AND hd.ngayKetThuc <= :hanChot)
                   OR (:filter = 'DA_KET_THUC' AND (hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.KET_THUC_BINH_THUONG OR hd.trangThai = com.qlytro.entity.enums.TrangThaiHopDong.KET_THUC_BAT_THUONG))
                  )
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(p.soPhong) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(tn.tenToaNha) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR EXISTS (
                       SELECT 1 FROM HopDongKhachThue hdkt
                       WHERE hdkt.hopDong = hd
                         AND hdkt.laDaiDien = true
                         AND LOWER(hdkt.khachThue.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   )
                  )
            """
    )
    Page<HopDong> findHopDongFilterAndPage(
            @Param("filter") String filter,
            @Param("hanChot") LocalDate hanChot,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
