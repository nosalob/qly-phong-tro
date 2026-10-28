package com.qlytro.repository;

import com.qlytro.entity.SuCo;
import com.qlytro.entity.enums.TrangThaiSuCo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SuCoRepository extends JpaRepository<SuCo, Integer> {
    List<SuCo> findByPhong_Id(Integer phongId);
    List<SuCo> findByTrangThai(TrangThaiSuCo trangThai);
    List<SuCo> findByNguoiBao_Id(Integer nguoiDungId);
    List<SuCo> findByPhong_IdAndTrangThai(Integer phongId, TrangThaiSuCo trangThai);
    boolean existsByThietBi_Id(Integer thietBiId);
    boolean existsByPhong_Id(Integer phongId);
}
