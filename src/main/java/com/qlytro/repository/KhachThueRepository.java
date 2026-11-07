package com.qlytro.repository;

import com.qlytro.entity.KhachThue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachThueRepository extends JpaRepository<KhachThue, Integer> {

    boolean existsByCccd(String cccd);

    boolean existsByCccdAndIdNot(String cccd, Integer id);

    Optional<KhachThue> findByCccd(String cccd);

    Optional<KhachThue> findByNguoiDung_Id(Integer nguoiDungId);

    @Query("""
            SELECT kt FROM KhachThue kt
            WHERE LOWER(kt.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR kt.cccd LIKE CONCAT('%', :keyword, '%')
            """)
    Page<KhachThue> search(@Param("keyword") String keyword, Pageable pageable);
}