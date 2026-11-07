package com.qlytro.repository;

import com.qlytro.entity.NhacThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NhacThanhToanRepository extends JpaRepository<NhacThanhToan, Integer> {

    @Query("SELECT n FROM NhacThanhToan n " +
           "JOIN FETCH n.nguoiDung " +
           "WHERE n.hoaDon.id = :hoaDonId " +
           "ORDER BY n.thoiGianNhac DESC")
    List<NhacThanhToan> findByHoaDonIdWithNguoiDung(@Param("hoaDonId") Integer hoaDonId);

    @Query("SELECT n FROM NhacThanhToan n " +
           "JOIN FETCH n.nguoiDung " +
           "WHERE n.hoaDon.id IN :hoaDonIds " +
           "ORDER BY n.thoiGianNhac DESC")
    List<NhacThanhToan> findByHoaDonIdInWithNguoiDung(@Param("hoaDonIds") Collection<Integer> hoaDonIds);
}

