package com.qlytro.dto.response;

import com.qlytro.entity.enums.HinhThucNhac;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhacThanhToanResponse {
    private Integer id;
    private String tenNguoiNhac;
    private HinhThucNhac hinhThuc;
    private String hinhThucLabel;
    private String noiDung;
    private LocalDateTime thoiGianNhac;
}

