package com.qlytro.service;

import com.qlytro.service.impl.HoaDonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HoaDonServiceTest {

    @Test
    @DisplayName("Quy tắc mốc quá hạn: trước ngày 06 chưa quá hạn kỳ hiện tại, từ ngày 06 quá hạn kỳ hiện tại")
    void testGetThangNamMocQuaHan() {
        // Ngày 05/11/2026 -> mốc là 2026-10 (tháng 11 chưa quá hạn)
        LocalDate day5 = LocalDate.of(2026, 11, 5);
        assertEquals("2026-10", HoaDonService.getThangNamMocQuaHan(day5));

        // Ngày 06/11/2026 -> mốc là 2026-11 (tháng 11 quá hạn)
        LocalDate day6 = LocalDate.of(2026, 11, 6);
        assertEquals("2026-11", HoaDonService.getThangNamMocQuaHan(day6));

        // Ngày 01/11/2026 -> mốc là 2026-10
        LocalDate day1 = LocalDate.of(2026, 11, 1);
        assertEquals("2026-10", HoaDonService.getThangNamMocQuaHan(day1));
    }
}
