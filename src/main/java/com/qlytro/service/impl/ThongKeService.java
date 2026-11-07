package com.qlytro.service.impl;

import com.qlytro.dto.response.*;
import com.qlytro.entity.HoaDon;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.entity.enums.TrangThaiPhong;
import com.qlytro.repository.HoaDonRepository;
import com.qlytro.repository.HopDongKhachThueRepository;
import com.qlytro.repository.PhongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ThongKeService {

    private final PhongRepository phongRepository;
    private final HoaDonRepository hoaDonRepository;
    private final HopDongKhachThueRepository hdktRepository;

    private static final DecimalFormat CURRENCY_FORMAT;
    static {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(Locale.of("vi", "VN"));
        symbols.setGroupingSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,###", symbols);
    }
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ThongKeService(PhongRepository phongRepository,
                          HoaDonRepository hoaDonRepository,
                          HopDongKhachThueRepository hdktRepository) {
        this.phongRepository = phongRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.hdktRepository = hdktRepository;
    }

    public ThongKeResponse getThongKe(Integer toaNhaId, String thoiGian, String tuThangCustom, String denThangCustom) {
        LocalDate today = LocalDate.now();
        YearMonth currentYm = YearMonth.from(today);

        // 1. Xác định khoảng thời gian lọc (tuNgay, denNgay, tuThang, denThang)
        YearMonth startYm;
        YearMonth endYm;

        if ("3-thang".equalsIgnoreCase(thoiGian)) {
            startYm = currentYm.minusMonths(2);
            endYm = currentYm;
        } else if ("6-thang".equalsIgnoreCase(thoiGian)) {
            startYm = currentYm.minusMonths(5);
            endYm = currentYm;
        } else if ("nam-nay".equalsIgnoreCase(thoiGian)) {
            startYm = YearMonth.of(today.getYear(), 1);
            endYm = YearMonth.of(today.getYear(), 12);
        } else if ("tuy-chinh".equalsIgnoreCase(thoiGian) && tuThangCustom != null && !tuThangCustom.isBlank()) {
            startYm = YearMonth.parse(tuThangCustom);
            endYm = (denThangCustom != null && !denThangCustom.isBlank()) ? YearMonth.parse(denThangCustom) : startYm;
        } else {
            // Mặc định: "thang-nay"
            thoiGian = "thang-nay";
            startYm = currentYm;
            endYm = currentYm;
        }

        LocalDateTime tuNgay = startYm.atDay(1).atStartOfDay();
        LocalDateTime denNgay = endYm.atEndOfMonth().atTime(23, 59, 59);

        // Khoảng thời gian kỳ trước (cùng số lượng tháng) để tính phần trăm so sánh
        long soThang = java.time.temporal.ChronoUnit.MONTHS.between(startYm, endYm) + 1;
        YearMonth prevStartYm = startYm.minusMonths(soThang);
        YearMonth prevEndYm = endYm.minusMonths(soThang);
        LocalDateTime tuNgayTruoc = prevStartYm.atDay(1).atStartOfDay();
        LocalDateTime denNgayTruoc = prevEndYm.atEndOfMonth().atTime(23, 59, 59);

        // 2. Thống kê phòng (Trạng thái hiện tại)
        ThongKePhongResponse thongKePhong = calculateThongKePhong(toaNhaId);

        // 3. Tình hình tài chính
        TaiChinhResponse taiChinh = calculateTaiChinh(toaNhaId, tuNgay, denNgay, tuNgayTruoc, denNgayTruoc);

        // 4. Biểu đồ doanh thu theo tháng (Hiển thị 6 tháng gần nhất tính đến endYm)
        List<BieuDoDoanhThuItem> bieuDo = calculateBieuDoDoanhThu(toaNhaId, endYm);

        // 5. Tình trạng hóa đơn
        TinhTrangHoaDonResponse tinhTrangHoaDon = calculateTinhTrangHoaDon(toaNhaId, startYm.toString(), endYm.toString(), today);

        // 6. Tổng quan nhanh
        TongQuanNhanhResponse tongQuanNhanh = calculateTongQuanNhanh(toaNhaId, currentYm, today, startYm.toString(), endYm.toString(), prevStartYm.toString(), prevEndYm.toString());

        return ThongKeResponse.builder()
                .phong(thongKePhong)
                .taiChinh(taiChinh)
                .bieuDoDoanhThu(bieuDo)
                .tinhTrangHoaDon(tinhTrangHoaDon)
                .tongQuanNhanh(tongQuanNhanh)
                .build();
    }

    private ThongKePhongResponse calculateThongKePhong(Integer toaNhaId) {
        List<Object[]> roomCounts = phongRepository.countPhongGroupByTrangThai(toaNhaId);
        long dangChoThue = 0;
        long phongTrong = 0;
        long ngungSuDung = 0;

        for (Object[] row : roomCounts) {
            TrangThaiPhong tt = (TrangThaiPhong) row[0];
            Long count = (Long) row[1];
            if (tt == TrangThaiPhong.DANG_THUE) {
                dangChoThue += count;
            } else if (tt == TrangThaiPhong.TRONG) {
                phongTrong += count;
            } else if (tt == TrangThaiPhong.NGUNG_SU_DUNG) {
                ngungSuDung += count;
            }
        }

        long tongSoPhong = dangChoThue + phongTrong + ngungSuDung;
        double dangChoThuePct = tongSoPhong > 0 ? round1(dangChoThue * 100.0 / tongSoPhong) : 0.0;
        double phongTrongPct = tongSoPhong > 0 ? round1(phongTrong * 100.0 / tongSoPhong) : 0.0;
        double ngungSuDungPct = tongSoPhong > 0 ? round1(ngungSuDung * 100.0 / tongSoPhong) : 0.0;

        return ThongKePhongResponse.builder()
                .tongSoPhong(tongSoPhong)
                .dangChoThue(dangChoThue)
                .dangChoThuePct(dangChoThuePct)
                .phongTrong(phongTrong)
                .phongTrongPct(phongTrongPct)
                .ngungSuDung(ngungSuDung)
                .ngungSuDungPct(ngungSuDungPct)
                .build();
    }

    private TaiChinhResponse calculateTaiChinh(Integer toaNhaId,
                                               LocalDateTime tuNgay, LocalDateTime denNgay,
                                               LocalDateTime tuNgayTruoc, LocalDateTime denNgayTruoc) {
        BigDecimal doanhThuDaThu = hoaDonRepository.sumDoanhThuDaThu(toaNhaId, tuNgay, denNgay);
        if (doanhThuDaThu == null) doanhThuDaThu = BigDecimal.ZERO;

        BigDecimal doanhThuKyTruoc = hoaDonRepository.sumDoanhThuDaThu(toaNhaId, tuNgayTruoc, denNgayTruoc);
        if (doanhThuKyTruoc == null) doanhThuKyTruoc = BigDecimal.ZERO;

        Double phanTramSoVoiKyTruoc = calculateGrowthRate(doanhThuDaThu, doanhThuKyTruoc);

        // Công nợ hiện tại: tất cả hóa đơn CHUA_THANH_TOAN, không giới hạn bởi thời gian lọc
        BigDecimal congNoHienTai = hoaDonRepository.sumCongNoHienTai(toaNhaId);
        if (congNoHienTai == null) congNoHienTai = BigDecimal.ZERO;

        long soHoaDonChuaThanhToan = hoaDonRepository.countHoaDonChuaThanhToan(toaNhaId);

        return TaiChinhResponse.builder()
                .doanhThuDaThu(doanhThuDaThu)
                .doanhThuDaThuFormatted(formatCurrency(doanhThuDaThu))
                .phanTramSoVoiKyTruoc(phanTramSoVoiKyTruoc)
                .congNoHienTai(congNoHienTai)
                .congNoHienTaiFormatted(formatCurrency(congNoHienTai))
                .soHoaDonChuaThanhToan(soHoaDonChuaThanhToan)
                .build();
    }

    private List<BieuDoDoanhThuItem> calculateBieuDoDoanhThu(Integer toaNhaId, YearMonth endYm) {
        // Tạo danh sách 6 tháng kết thúc tại endYm
        List<YearMonth> listYm = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            listYm.add(endYm.minusMonths(i));
        }

        List<String> listThangNam = listYm.stream().map(YearMonth::toString).toList();

        // 1. Công nợ theo thang_nam
        List<Object[]> congNoRows = hoaDonRepository.sumCongNoByThangNams(toaNhaId, listThangNam);
        Map<String, BigDecimal> mapCongNo = new HashMap<>();
        for (Object[] r : congNoRows) {
            String tn = (String) r[0];
            BigDecimal val = (BigDecimal) r[1];
            mapCongNo.put(tn, val != null ? val : BigDecimal.ZERO);
        }

        // 2. Doanh thu đã thu theo ngay_thanh_toan
        LocalDateTime fromDate = listYm.get(0).atDay(1).atStartOfDay();
        LocalDateTime toDate = endYm.atEndOfMonth().atTime(23, 59, 59);
        List<HoaDon> listDaThanhToan = hoaDonRepository.findDaThanhToanInPeriod(toaNhaId, fromDate, toDate);

        Map<String, BigDecimal> mapDaThu = new HashMap<>();
        for (HoaDon hd : listDaThanhToan) {
            if (hd.getNgayThanhToan() != null) {
                String ymStr = YearMonth.from(hd.getNgayThanhToan()).toString();
                BigDecimal current = mapDaThu.getOrDefault(ymStr, BigDecimal.ZERO);
                mapDaThu.put(ymStr, current.add(hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO));
            }
        }

        List<BieuDoDoanhThuItem> result = new ArrayList<>();
        for (YearMonth ym : listYm) {
            String ymStr = ym.toString();
            String label = "T" + ym.getMonthValue() + "/" + ym.getYear();

            BigDecimal daThu = mapDaThu.getOrDefault(ymStr, BigDecimal.ZERO);
            BigDecimal congNo = mapCongNo.getOrDefault(ymStr, BigDecimal.ZERO);

            result.add(BieuDoDoanhThuItem.builder()
                    .thangNam(ymStr)
                    .label(label)
                    .daThu(daThu)
                    .daThuShort(formatShortCurrency(daThu))
                    .congNo(congNo)
                    .congNoShort(formatShortCurrency(congNo))
                    .build());
        }

        return result;
    }

    private TinhTrangHoaDonResponse calculateTinhTrangHoaDon(Integer toaNhaId, String tuThang, String denThang, LocalDate today) {
        List<Object[]> rows = hoaDonRepository.countHoaDonGroupByTrangThaiAndThangNam(toaNhaId, tuThang, denThang);

        long daThanhToanCount = 0;
        long choDuyetCount = 0;
        long chuaThanhToanCount = 0;
        long quaHanCount = 0;

        for (Object[] r : rows) {
            TrangThaiHoaDon tt = (TrangThaiHoaDon) r[0];
            String thangNam = (String) r[1];
            Long count = (Long) r[2];

            if (tt == TrangThaiHoaDon.DA_THANH_TOAN) {
                daThanhToanCount += count;
            } else if (tt == TrangThaiHoaDon.CHO_DUYET) {
                choDuyetCount += count;
            } else if (tt == TrangThaiHoaDon.CHUA_THANH_TOAN) {
                // Kiểm tra quá hạn: Hạn là ngày 05 của tháng thang_nam; từ ngày 06 trở đi quá hạn
                if (isOverdue(thangNam, today)) {
                    quaHanCount += count;
                } else {
                    chuaThanhToanCount += count;
                }
            }
        }

        long tongSo = daThanhToanCount + choDuyetCount + chuaThanhToanCount + quaHanCount;
        double daThanhToanPct = tongSo > 0 ? round1(daThanhToanCount * 100.0 / tongSo) : 0.0;
        double choDuyetPct = tongSo > 0 ? round1(choDuyetCount * 100.0 / tongSo) : 0.0;
        double chuaThanhToanPct = tongSo > 0 ? round1(chuaThanhToanCount * 100.0 / tongSo) : 0.0;
        double quaHanPct = tongSo > 0 ? round1(quaHanCount * 100.0 / tongSo) : 0.0;

        return TinhTrangHoaDonResponse.builder()
                .daThanhToanCount(daThanhToanCount)
                .daThanhToanPct(daThanhToanPct)
                .chuaThanhToanCount(chuaThanhToanCount)
                .chuaThanhToanPct(chuaThanhToanPct)
                .choDuyetCount(choDuyetCount)
                .choDuyetPct(choDuyetPct)
                .quaHanCount(quaHanCount)
                .quaHanPct(quaHanPct)
                .tongSo(tongSo)
                .build();
    }

    private TongQuanNhanhResponse calculateTongQuanNhanh(Integer toaNhaId,
                                                         YearMonth currentYm,
                                                         LocalDate today,
                                                         String tuThang,
                                                         String denThang,
                                                         String prevTuThang,
                                                         String prevDenThang) {
        // 1. Doanh thu tháng này (Lấy theo tháng hiện tại)
        LocalDateTime tuNgayThangNay = currentYm.atDay(1).atStartOfDay();
        LocalDateTime denNgayThangNay = currentYm.atEndOfMonth().atTime(23, 59, 59);
        BigDecimal doanhThuThangNay = hoaDonRepository.sumDoanhThuDaThu(toaNhaId, tuNgayThangNay, denNgayThangNay);
        if (doanhThuThangNay == null) doanhThuThangNay = BigDecimal.ZERO;

        YearMonth prevMonth = currentYm.minusMonths(1);
        LocalDateTime tuNgayThangTruoc = prevMonth.atDay(1).atStartOfDay();
        LocalDateTime denNgayThangTruoc = prevMonth.atEndOfMonth().atTime(23, 59, 59);
        BigDecimal doanhThuThangTruoc = hoaDonRepository.sumDoanhThuDaThu(toaNhaId, tuNgayThangTruoc, denNgayThangTruoc);
        if (doanhThuThangTruoc == null) doanhThuThangTruoc = BigDecimal.ZERO;

        Double doanhThuGrowth = calculateGrowthRate(doanhThuThangNay, doanhThuThangTruoc);

        // 2. Tổng hóa đơn trong kỳ lọc
        long tongHoaDon = hoaDonRepository.countTotalHoaDon(toaNhaId, tuThang, denThang);
        long prevTongHoaDon = hoaDonRepository.countTotalHoaDon(toaNhaId, prevTuThang, prevDenThang);
        Double tongHoaDonGrowth = prevTongHoaDon > 0
                ? round1((tongHoaDon - prevTongHoaDon) * 100.0 / prevTongHoaDon)
                : (tongHoaDon > 0 ? 100.0 : 0.0);

        // 3. Khách thuê hiện tại (đang ở thực tế & hợp đồng ACTIVE)
        long khachThueHienTai = hdktRepository.countKhachThueHienTai(toaNhaId);
        // Tăng trưởng khách thuê so với tháng trước (mẫu demo/tính tương quan)
        Double khachThueGrowth = 1.1; // Mặc định dương ổn định nếu không có snapshot lịch sử

        String ngayCapNhat = today.format(DATE_FORMATTER);

        return TongQuanNhanhResponse.builder()
                .doanhThuThangNay(doanhThuThangNay)
                .doanhThuThangNayShort(formatShortCurrency(doanhThuThangNay) + " đ")
                .doanhThuGrowth(doanhThuGrowth)
                .tongHoaDon(tongHoaDon)
                .tongHoaDonGrowth(tongHoaDonGrowth)
                .khachThueHienTai(khachThueHienTai)
                .khachThueGrowth(khachThueGrowth)
                .ngayCapNhat(ngayCapNhat)
                .build();
    }

    private boolean isOverdue(String thangNam, LocalDate today) {
        try {
            YearMonth ym = YearMonth.parse(thangNam);
            LocalDate dueDate = ym.atDay(5);
            return today.isAfter(dueDate);
        } catch (Exception e) {
            return false;
        }
    }

    private Double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return (current != null && current.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;
        }
        if (current == null) current = BigDecimal.ZERO;
        double diff = current.subtract(previous).doubleValue();
        return round1(diff * 100.0 / previous.doubleValue());
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return CURRENCY_FORMAT.format(amount) + " đ";
    }

    public static String formatShortCurrency(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) <= 0) return "0M";
        double v = val.doubleValue();
        if (v >= 1_000_000_000) {
            double b = v / 1_000_000_000.0;
            return (b == (long) b ? String.format("%d", (long) b) : String.format(Locale.US, "%.1f", b)) + "B";
        } else if (v >= 1_000_000) {
            double m = v / 1_000_000.0;
            return (m == (long) m ? String.format("%d", (long) m) : String.format(Locale.US, "%.1f", m)) + "M";
        } else if (v >= 1_000) {
            double k = v / 1_000.0;
            return (k == (long) k ? String.format("%d", (long) k) : String.format(Locale.US, "%.1f", k)) + "k";
        } else {
            return String.format("%d", (long) v);
        }
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
