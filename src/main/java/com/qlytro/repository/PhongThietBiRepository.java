package com.qlytro.repository;

import com.qlytro.entity.PhongThietBi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhongThietBiRepository extends JpaRepository<PhongThietBi, Integer> {
    List<PhongThietBi> findByPhong_Id(Integer phongId);
    boolean existsByPhong_IdAndThietBi_Id(Integer phongId, Integer thietBiId);
    boolean existsByThietBi_Id(Integer thietBiId);
    long countByThietBi_Id(Integer thietBiId);
    Optional<PhongThietBi> findByPhong_IdAndThietBi_Id(Integer phongId, Integer thietBiId);
    void deleteByPhong_IdAndThietBi_Id(Integer phongId, Integer thietBiId);
}
