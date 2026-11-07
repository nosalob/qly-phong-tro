package com.qlytro.entity.enums;

/**
 * Hinh thuc lien he khi chu tro nhac nguoi thue thanh toan hoa don.
 * Map voi cot hinh_thuc_lien_he (TINYINT) trong bang nhac_thanh_toan.
 * 0 = Goi dien, 1 = Tin nhan, 2 = Gap truc tiep
 */
public enum HinhThucNhac {
    GOI_DIEN(0, "Gọi điện"),
    TIN_NHAN(1, "Tin nhắn"),
    TRUC_TIEP(2, "Gặp trực tiếp");

    private final int value;
    private final String label;

    HinhThucNhac(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static HinhThucNhac fromValue(int value) {
        for (HinhThucNhac h : values()) {
            if (h.value == value) {
                return h;
            }
        }
        throw new IllegalArgumentException("Hình thức nhắc không hợp lệ: " + value);
    }
}

