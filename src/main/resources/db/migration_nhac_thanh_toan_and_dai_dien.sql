-- =====================================================================
-- MIGRATION: BỔ SUNG LỊCH SỬ NHẮC THANH TOÁN VÀ SNAPSHOT ĐẠI DIỆN HÓA ĐƠN
-- Dự án: Quản lý phòng trọ (Spring Boot 3.3.4 + MySQL + Hibernate validate)
-- LƯU Ý QUAN TRỌNG:
-- - Không sử dụng CREATE TABLE IF NOT EXISTS để tránh che giấu lỗi cấu trúc.
-- - Không backfill người đại diện cho hóa đơn cũ (giữ NULL để phân biệt rõ lịch sử).
-- - Thực thi tuần tự các bước dưới đây trên MySQL trước khi khởi động ứng dụng.
-- =====================================================================

USE quan_ly_phong_tro;

-- ---------------------------------------------------------------------
-- 1. BẢNG MỚI: nhac_thanh_toan
-- Lưu lịch sử mỗi lần chủ trọ nhắc người thuê thanh toán hóa đơn.
-- Khóa ngoại:
--   - hoa_don_id -> hoa_don(id): ON DELETE RESTRICT (không cho xóa HĐ nếu đã có vết nhắc nợ)
--   - nguoi_dung_id -> nguoi_dung(id): ON DELETE RESTRICT (bảo toàn audit log người nhắc)
-- Index:
--   - idx_nhac_tt_hd_time (hoa_don_id, thoi_gian_nhac DESC) tối ưu B-Tree scan
-- ---------------------------------------------------------------------
CREATE TABLE nhac_thanh_toan (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    hoa_don_id          INT NOT NULL,
    nguoi_dung_id       INT NOT NULL,
    hinh_thuc_lien_he   TINYINT NOT NULL COMMENT '0=GOI_DIEN, 1=TIN_NHAN, 2=TRUC_TIEP',
    noi_dung            VARCHAR(500) NOT NULL,
    thoi_gian_nhac      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nhac_tt_hoa_don FOREIGN KEY (hoa_don_id) 
        REFERENCES hoa_don(id) ON DELETE RESTRICT,
    CONSTRAINT fk_nhac_tt_nguoi_dung FOREIGN KEY (nguoi_dung_id) 
        REFERENCES nguoi_dung(id) ON DELETE RESTRICT,
    INDEX idx_nhac_tt_hd_time (hoa_don_id, thoi_gian_nhac DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. BỔ SUNG CỘT: nguoi_dai_dien_id TRONG BẢNG hoa_don
-- Lưu snapshot người đại diện hợp đồng tại thời điểm lập hóa đơn.
-- Quy tắc an toàn:
--   - Cột cho phép NULL để tương thích 100% với dữ liệu hóa đơn cũ đã có.
--   - Hóa đơn cũ có nguoi_dai_dien_id = NULL (thể hiện rõ giới hạn dữ liệu).
--   - Hóa đơn mới bắt buộc Service gán người đại diện hợp lệ lúc tạo.
--   - Khóa ngoại ON DELETE RESTRICT: tuyệt đối không dùng CASCADE để tránh mất hóa đơn tài chính.
-- ---------------------------------------------------------------------
ALTER TABLE hoa_don 
ADD COLUMN nguoi_dai_dien_id INT NULL AFTER hop_dong_id;

ALTER TABLE hoa_don 
ADD CONSTRAINT fk_hoa_don_nguoi_dai_dien FOREIGN KEY (nguoi_dai_dien_id) 
    REFERENCES khach_thue(id) ON DELETE RESTRICT;

CREATE INDEX idx_hoa_don_dai_dien ON hoa_don (nguoi_dai_dien_id);

-- ---------------------------------------------------------------------
-- 3. CÂU LỆNH KIỂM TRA SAU KHI CHẠY (CHỈ ĐỌC)
-- ---------------------------------------------------------------------
-- SHOW CREATE TABLE nhac_thanh_toan;
-- DESCRIBE hoa_don;
-- SELECT id, hop_dong_id, nguoi_dai_dien_id, thang_nam FROM hoa_don LIMIT 10;

