package com.qlytro.repository;

import com.qlytro.entity.HopDongKhachThue;
import com.qlytro.entity.KhachThue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HopDongKhachThueRepository extends JpaRepository<HopDongKhachThue, Integer> {

    List<HopDongKhachThue> findByHopDong_Id(Integer hopDongId);

    @Query("""
            SELECT hdkt FROM HopDongKhachThue hdkt
            JOIN FETCH hdkt.khachThue kt
            WHERE hdkt.hopDong.id = :hopDongId
            ORDER BY hdkt.laDaiDien DESC, hdkt.id ASC
            """)
    List<HopDongKhachThue> findByHopDongIdWithKhachThue(@Param("hopDongId") Integer hopDongId);

    List<HopDongKhachThue> findByHopDong_IdAndDangO(Integer hopDongId, Boolean dangO);

    Optional<HopDongKhachThue> findByHopDong_IdAndKhachThue_Id(Integer hopDongId, Integer khachThueId);

    Optional<HopDongKhachThue> findByHopDong_IdAndLaDaiDienTrue(Integer hopDongId);

    boolean existsByHopDong_IdAndKhachThue_Id(Integer hopDongId, Integer khachThueId);
    boolean existsByKhachThue_Id(Integer khachThueId);

    /** Tim tat ca hop dong cua 1 khach thue. */
    List<HopDongKhachThue> findByKhachThue_Id(Integer khachThueId);

    /**
     * Dem so khach co la_dai_dien=true trong 1 hop dong.
     * Rang buoc: con so nay phai LUON = 1 (validate trong Service).
     */
    @Query("SELECT COUNT(h) FROM HopDongKhachThue h WHERE h.hopDong.id = :hopDongId AND h.laDaiDien = true")
    long countDaiDienByHopDongId(@Param("hopDongId") Integer hopDongId);

    /**
     * Tim KhachThue tu nguoi_dung_id (qua lien ket nguoi_dung trong khach_thue).
     * Dung de lay danh sach hoa don, chi so cua tenant dang dang nhap.
     */
    @Query("SELECT DISTINCT h.khachThue FROM HopDongKhachThue h WHERE h.khachThue.nguoiDung.id = :nguoiDungId")
    Optional<KhachThue> findKhachThueByNguoiDungId(@Param("nguoiDungId") Integer nguoiDungId);
}

