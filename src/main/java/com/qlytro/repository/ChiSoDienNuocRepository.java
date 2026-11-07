package com.qlytro.repository;

import com.qlytro.entity.ChiSoDienNuoc;
import com.qlytro.entity.enums.TrangThaiChiSo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Tìm chỉ số mới nhất của phòng để lấy chỉ số cũ cho tháng tiếp theo.
     */
    Optional<ChiSoDienNuoc> findTopByPhong_IdOrderByThangNamDesc(Integer phongId);

    /**
     * Lọc theo phòng, tháng và phân trang.
     * Bản ghi chưa chốt được ưu tiên hiển thị trước.
     * Trong mỗi nhóm, tháng mới nhất hiển thị trước.
     */
    @Query(
            value = """
                    SELECT cs
                    FROM ChiSoDienNuoc cs
                    WHERE (:phongId IS NULL OR cs.phong.id = :phongId)
                      AND (:thangNam IS NULL OR cs.thangNam = :thangNam)
                    ORDER BY
                        CASE
                            WHEN cs.trangThai = :daChot THEN 1
                            ELSE 0
                        END ASC,
                        cs.thangNam DESC
                    """,
            countQuery = """
                    SELECT COUNT(cs)
                    FROM ChiSoDienNuoc cs
                    WHERE (:phongId IS NULL OR cs.phong.id = :phongId)
                      AND (:thangNam IS NULL OR cs.thangNam = :thangNam)
                    """
    )
    Page<ChiSoDienNuoc> findByFilters(
            @Param("phongId") Integer phongId,
            @Param("thangNam") String thangNam,
            @Param("daChot") TrangThaiChiSo daChot,
            Pageable pageable
    );
}