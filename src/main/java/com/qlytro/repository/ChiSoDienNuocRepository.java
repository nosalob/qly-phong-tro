package com.qlytro.repository;

import com.qlytro.entity.ChiSoDienNuoc;
import com.qlytro.entity.enums.TrangThaiChiSo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChiSoDienNuocRepository extends JpaRepository<ChiSoDienNuoc, Integer> {

    Optional<ChiSoDienNuoc> findByPhong_IdAndThangNam(Integer phongId, String thangNam);

    List<ChiSoDienNuoc> findByPhong_IdOrderByThangNamDesc(Integer phongId);

    List<ChiSoDienNuoc> findByTrangThai(TrangThaiChiSo trangThai);

    boolean existsByPhong_IdAndThangNam(Integer phongId, String thangNam);
    boolean existsByPhong_Id(Integer phongId);

    /** Tim chi so moi nhat cua phong de lay chi so cu cho thang tiep theo. */
    Optional<ChiSoDienNuoc> findTopByPhong_IdOrderByThangNamDesc(Integer phongId);
}
