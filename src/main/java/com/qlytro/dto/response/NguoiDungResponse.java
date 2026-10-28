package com.qlytro.dto.response;

import com.qlytro.entity.enums.TrangThaiNguoiDung;
import com.qlytro.entity.enums.VaiTroNguoiDung;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NguoiDungResponse {
    private Integer id;
    private String username;
    private String hoTen;
    private String sdt;
    private String email;
    private VaiTroNguoiDung vaiTro;
    private String vaiTroLabel;
    private TrangThaiNguoiDung trangThai;
    private String trangThaiLabel;
    private Boolean mustChangePassword;
    private LocalDateTime ngayTao;
}
