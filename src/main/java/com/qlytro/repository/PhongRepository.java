package com.qlytro.repository;

import com.qlytro.entity.Phong;
import com.qlytro.entity.enums.TrangThaiPhong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhongRepository extends JpaRepository<Phong, Integer> {

    List<Phong> findByToaNha_Id(Integer toaNhaId);

    List<Phong> findByToaNha_IdAndTrangThai(Integer toaNhaId, TrangThaiPhong trangThai);

    List<Phong> findByTrangThai(TrangThaiPhong trangThai);

    long countByTrangThai(TrangThaiPhong trangThai);

    boolean existsByToaNha_IdAndSoPhong(Integer toaNhaId, String soPhong);

    boolean existsByToaNha_IdAndSoPhongAndIdNot(Integer toaNhaId, String soPhong, Integer id);

    Optional<Phong> findByToaNha_IdAndSoPhong(Integer toaNhaId, String soPhong);

    @Query("SELECT COUNT(p) FROM Phong p WHERE p.toaNha.id = :toaNhaId")
    long countByToaNhaId(@Param("toaNhaId") Integer toaNhaId);

    @Query(
        value = """
            SELECT p FROM Phong p
            JOIN FETCH p.toaNha tn
            WHERE (:toaNhaId IS NULL OR tn.id = :toaNhaId)
              AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.soPhong) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:trangThai IS NULL OR p.trangThai = :trangThai)
            ORDER BY LENGTH(p.soPhong) ASC, p.soPhong ASC, p.id ASC
            """,
        countQuery = """
            SELECT COUNT(p) FROM Phong p
            WHERE (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
              AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.soPhong) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:trangThai IS NULL OR p.trangThai = :trangThai)
            """
    )
    Page<Phong> findPhongFilterAndPage(
            @Param("toaNhaId") Integer toaNhaId,
            @Param("keyword") String keyword,
            @Param("trangThai") TrangThaiPhong trangThai,
            Pageable pageable
    );

    @Query("""
        SELECT p.trangThai, COUNT(p) FROM Phong p
        WHERE (:toaNhaId IS NULL OR p.toaNha.id = :toaNhaId)
        GROUP BY p.trangThai
    """)
    List<Object[]> countPhongGroupByTrangThai(@Param("toaNhaId") Integer toaNhaId);
}
