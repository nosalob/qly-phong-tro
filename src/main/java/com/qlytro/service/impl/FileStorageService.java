package com.qlytro.service.impl;

import com.qlytro.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Kiểm tra và lưu file ảnh upload lên server.
     * Đặt tên file mới hoàn toàn UUID + extension hợp lệ.
     * Trả về đường dẫn truy cập web (ví dụ: "/uploads/abc-xyz.jpg").
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn file tải lên.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Dung lượng file không được vượt quá 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("File tải lên phải là hình ảnh hợp lệ (chỉ chấp nhận jpg, jpeg, png, webp, gif).");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Định dạng file không được hỗ trợ. Chỉ chấp nhận các đuôi: jpg, jpeg, png, webp, gif.");
        }

        // Kiểm tra magic bytes thực tế của file để chặn file script/exe đổi đuôi
        validateImageMagicBytes(file, extension.toLowerCase(Locale.ROOT));

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String newFilename = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
            Path targetLocation = uploadPath.resolve(newFilename).normalize();

            // Đảm bảo không bị path traversal
            if (!targetLocation.startsWith(uploadPath.toAbsolutePath().normalize()) &&
                !targetLocation.toAbsolutePath().normalize().startsWith(uploadPath.toAbsolutePath().normalize())) {
                throw new BusinessException("Đường dẫn lưu trữ file không hợp lệ.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + newFilename;
        } catch (IOException ex) {
            throw new BusinessException("Không thể lưu file: " + ex.getMessage());
        }
    }

    /**
     * Xóa file đã ghi nếu bước tiếp theo (lưu DB) thất bại.
     */
    public void deleteFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        try {
            String filename = filePath;
            if (filename.startsWith("/uploads/")) {
                filename = filename.substring("/uploads/".length());
            } else if (filename.startsWith("uploads/")) {
                filename = filename.substring("uploads/".length());
            }
            Path path = Paths.get(uploadDir).resolve(filename).normalize();
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // Không làm gián đoạn luồng lỗi chính
        }
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Kiểm tra byte đầu (magic bytes) để đảm bảo nội dung thực sự là ảnh.
     */
    private void validateImageMagicBytes(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 4) {
                throw new BusinessException("Nội dung file không hợp lệ.");
            }

            boolean isValid = switch (extension) {
                case "jpg", "jpeg" -> (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF);
                case "png" -> (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47);
                case "gif" -> (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8');
                case "webp" -> (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P');
                default -> false;
            };

            if (!isValid) {
                throw new BusinessException("Nội dung file không khớp với định dạng ảnh (chỉ chấp nhận ảnh hợp lệ).");
            }
        } catch (IOException e) {
            throw new BusinessException("Không thể đọc nội dung file: " + e.getMessage());
        }
    }
}
