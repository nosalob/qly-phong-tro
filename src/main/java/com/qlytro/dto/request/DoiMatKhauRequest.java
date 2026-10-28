package com.qlytro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoiMatKhauRequest {

    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String matKhauCu;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
    private String matKhauMoi;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String xacNhanMatKhau;
}
