package com.qlytro.dto.request;

import com.qlytro.entity.enums.HinhThucNhac;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhacThanhToanRequest {

    @NotNull(message = "Vui lòng chọn hình thức nhắc")
    private HinhThucNhac hinhThuc;

    @NotBlank(message = "Nội dung nhắc không được để trống")
    @Size(max = 500, message = "Nội dung nhắc không được vượt quá 500 ký tự")
    private String noiDung;
}

