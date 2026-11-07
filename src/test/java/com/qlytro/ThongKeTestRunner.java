package com.qlytro;

import com.qlytro.dto.response.ThongKeResponse;
import com.qlytro.entity.HoaDon;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.entity.enums.TrangThaiPhong;
import com.qlytro.repository.HoaDonRepository;
import com.qlytro.repository.HopDongKhachThueRepository;
import com.qlytro.repository.PhongRepository;
import com.qlytro.service.impl.ThongKeService;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ThongKeTestRunner {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("BẮT ĐẦU CHẠY KIỂM THỬ CHỨC NĂNG THỐNG KÊ");
        System.out.println("=================================================");

        testThongKePhong();
        testThongKePhong_DuLieuRong();
        testTaiChinh();
        testTinhTrangHoaDon_PhanLoaiLoaiTru();
        testBieuDoDoanhThu();
        testTongQuanNhanh();
        testBoLocToaNha();

        System.out.println("=================================================");
        System.out.printf("KẾT QUẢ: ĐÃ CHẠY %d TESTS | THÀNH CÔNG: %d | THẤT BẠI: %d%n",
                passCount + failCount, passCount, failCount);
        System.out.println("=================================================");

        if (failCount > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            passCount++;
            System.out.println("[PASS] " + testName);
        } else {
            failCount++;
            System.err.println("[FAIL] " + testName);
        }
    }

    private static void testThongKePhong() {
        PhongRepository phongRepo = mockRepo(PhongRepository.class, Map.of(
                "countPhongGroupByTrangThai", args -> List.of(
                        new Object[]{TrangThaiPhong.DANG_THUE, 85L},
                        new Object[]{TrangThaiPhong.TRONG, 25L},
                        new Object[]{TrangThaiPhong.NGUNG_SU_DUNG, 10L}
                )
        ));
        HoaDonRepository hdRepo = mockRepo(HoaDonRepository.class, Map.of());
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        ThongKeService service = new ThongKeService(phongRepo, hdRepo, hdktRepo);
        ThongKeResponse res = service.getThongKe(null, "thang-nay", null, null);

        assertTrue(res.getPhong().getTongSoPhong() == 120, "Tổng số phòng = 120");
        assertTrue(res.getPhong().getDangChoThue() == 85, "Phòng đang cho thuê = 85");
        assertTrue(res.getPhong().getDangChoThuePct() == 70.8, "Tỷ lệ đang cho thuê = 70.8%");
        assertTrue(res.getPhong().getPhongTrong() == 25, "Phòng trống = 25");
        assertTrue(res.getPhong().getPhongTrongPct() == 20.8, "Tỷ lệ phòng trống = 20.8%");
        assertTrue(res.getPhong().getNgungSuDung() == 10, "Phòng ngừng sử dụng = 10");
        assertTrue(res.getPhong().getNgungSuDungPct() == 8.3, "Tỷ lệ phòng ngừng sử dụng = 8.3%");
    }

    private static void testThongKePhong_DuLieuRong() {
        PhongRepository phongRepo = mockRepo(PhongRepository.class, Map.of(
                "countPhongGroupByTrangThai", args -> Collections.emptyList()
        ));
        HoaDonRepository hdRepo = mockRepo(HoaDonRepository.class, Map.of());
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        ThongKeService service = new ThongKeService(phongRepo, hdRepo, hdktRepo);
        ThongKeResponse res = service.getThongKe(null, "thang-nay", null, null);

        assertTrue(res.getPhong().getTongSoPhong() == 0, "Dữ liệu phòng rỗng: Tổng số phòng = 0");
        assertTrue(res.getPhong().getDangChoThuePct() == 0.0, "Dữ liệu phòng rỗng: Không lỗi chia 0, Pct = 0.0");
    }

    private static void testTaiChinh() {
        PhongRepository phongRepo = mockRepo(PhongRepository.class, Map.of());
        HoaDonRepository hdRepo = mockRepo(HoaDonRepository.class, Map.of(
                "sumDoanhThuDaThu", args -> new BigDecimal("98500000"),
                "sumCongNoHienTai", args -> new BigDecimal("24700000"),
                "countHoaDonChuaThanhToan", args -> 8L
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        ThongKeService service = new ThongKeService(phongRepo, hdRepo, hdktRepo);
        ThongKeResponse res = service.getThongKe(null, "thang-nay", null, null);

        assertTrue(res.getTaiChinh().getDoanhThuDaThu().compareTo(new BigDecimal("98500000")) == 0,
                "Doanh thu đã thu = 98.500.000 đ");
        assertTrue(res.getTaiChinh().getCongNoHienTai().compareTo(new BigDecimal("24700000")) == 0,
                "Công nợ hiện tại = 24.700.000 đ");
        assertTrue(res.getTaiChinh().getSoHoaDonChuaThanhToan() == 8,
                "Số hóa đơn chưa thanh toán = 8");
        assertTrue(res.getTaiChinh().getDoanhThuDaThuFormatted().contains("98.500.000"),
                "Định dạng tiền doanh thu đúng VNĐ");
        assertTrue(res.getTaiChinh().getCongNoHienTaiFormatted().contains("24.700.000"),
                "Định dạng tiền công nợ đúng VNĐ");
    }

    private static void testTinhTrangHoaDon_PhanLoaiLoaiTru() {
        LocalDate today = LocalDate.now();
        String pastThangNam = YearMonth.from(today).minusMonths(2).toString();
        String futureThangNam = YearMonth.from(today).plusMonths(2).toString();

        List<Object[]> rows = List.of(
                new Object[]{TrangThaiHoaDon.DA_THANH_TOAN, pastThangNam, 82L},
                new Object[]{TrangThaiHoaDon.CHO_DUYET, pastThangNam, 6L},
                new Object[]{TrangThaiHoaDon.CHUA_THANH_TOAN, pastThangNam, 6L},  // quá hạn vì là tháng cũ
                new Object[]{TrangThaiHoaDon.CHUA_THANH_TOAN, futureThangNam, 25L} // chưa quá hạn vì tương lai
        );

        PhongRepository phongRepo = mockRepo(PhongRepository.class, Map.of());
        HoaDonRepository hdRepo = mockRepo(HoaDonRepository.class, Map.of(
                "countHoaDonGroupByTrangThaiAndThangNam", args -> rows
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        ThongKeService service = new ThongKeService(phongRepo, hdRepo, hdktRepo);
        ThongKeResponse res = service.getThongKe(null, "thang-nay", null, null);

        assertTrue(res.getTinhTrangHoaDon().getDaThanhToanCount() == 82, "Đã thanh toán count = 82");
        assertTrue(res.getTinhTrangHoaDon().getChoDuyetCount() == 6, "Chờ duyệt count = 6");
        assertTrue(res.getTinhTrangHoaDon().getChuaThanhToanCount() == 25, "Chưa thanh toán (chưa quá hạn) count = 25");
        assertTrue(res.getTinhTrangHoaDon().getQuaHanCount() == 6, "Quá hạn count = 6");
        assertTrue(res.getTinhTrangHoaDon().getTongSo() == 119, "Tổng số hóa đơn = 119");

        double totalPct = res.getTinhTrangHoaDon().getDaThanhToanPct()
                + res.getTinhTrangHoaDon().getChoDuyetPct()
                + res.getTinhTrangHoaDon().getChuaThanhToanPct()
                + res.getTinhTrangHoaDon().getQuaHanPct();
        assertTrue(totalPct >= 99.0 && totalPct <= 101.0, "Tổng 4 nhóm tỷ lệ đạt 100%");
    }

    private static void testBieuDoDoanhThu() {
        YearMonth now = YearMonth.now();
        List<Object[]> congNoRows = new ArrayList<>();
        congNoRows.add(new Object[]{now.toString(), new BigDecimal("10000000")});
        HoaDon hd1 = HoaDon.builder()
                .tongTien(new BigDecimal("98500000"))
                .ngayThanhToan(now.atDay(10).atTime(10, 0))
                .trangThai(TrangThaiHoaDon.DA_THANH_TOAN)
                .build();

        PhongRepository phongRepo = mockRepo(PhongRepository.class, Map.of());
        HoaDonRepository hdRepo = mockRepo(HoaDonRepository.class, Map.of(
                "sumCongNoByThangNams", args -> congNoRows,
                "findDaThanhToanInPeriod", args -> List.of(hd1)
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        ThongKeService service = new ThongKeService(phongRepo, hdRepo, hdktRepo);
        ThongKeResponse res = service.getThongKe(null, "thang-nay", null, null);

        assertTrue(res.getBieuDoDoanhThu().size() == 6, "Biểu đồ gồm 6 tháng gần nhất");
        assertTrue("98.5M".equals(res.getBieuDoDoanhThu().get(5).getDaThuShort()), "Tháng hiện tại đã thu = 98.5M");
        assertTrue("10M".equals(res.getBieuDoDoanhThu().get(5).getCongNoShort()), "Tháng hiện tại công nợ = 10M");
    }

    private static void testTongQuanNhanh() {
        PhongRepository phongRepo = mockRepo(PhongRepository.class, Map.of());
        HoaDonRepository hdRepo = mockRepo(HoaDonRepository.class, Map.of(
                "sumDoanhThuDaThu", args -> new BigDecimal("98500000"),
                "countTotalHoaDon", args -> 120L
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of(
                "countKhachThueHienTai", args -> 92L
        ));

        ThongKeService service = new ThongKeService(phongRepo, hdRepo, hdktRepo);
        ThongKeResponse res = service.getThongKe(null, "thang-nay", null, null);

        assertTrue(res.getTongQuanNhanh().getTongHoaDon() == 120, "Tổng hóa đơn = 120");
        assertTrue(res.getTongQuanNhanh().getKhachThueHienTai() == 92, "Khách thuê hiện tại = 92");
        assertTrue("98.5M đ".equals(res.getTongQuanNhanh().getDoanhThuThangNayShort()),
                "Doanh thu tháng này định dạng ngắn = 98.5M đ");
        assertTrue(res.getTongQuanNhanh().getNgayCapNhat() != null && !res.getTongQuanNhanh().getNgayCapNhat().isEmpty(),
                "Có ngày cập nhật dạng dd/MM/yyyy");
    }

    private static void testBoLocToaNha() {
        final Integer[] capturedToaNhaId = new Integer[1];
        PhongRepository phongRepo = mockRepo(PhongRepository.class, Map.of(
                "countPhongGroupByTrangThai", args -> {
                    capturedToaNhaId[0] = (Integer) args[0];
                    return Collections.emptyList();
                }
        ));
        HoaDonRepository hdRepo = mockRepo(HoaDonRepository.class, Map.of());
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        ThongKeService service = new ThongKeService(phongRepo, hdRepo, hdktRepo);
        service.getThongKe(5, "thang-nay", null, null);

        assertTrue(capturedToaNhaId[0] != null && capturedToaNhaId[0] == 5,
                "Bộ lọc tòa nhà ID = 5 được truyền chính xác tới Repository");
    }

    @SuppressWarnings("unchecked")
    private static <T> T mockRepo(Class<T> iface, Map<String, java.util.function.Function<Object[], Object>> handlers) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (proxy, method, args) -> {
            var handler = handlers.get(method.getName());
            if (handler != null) {
                return handler.apply(args);
            }
            if (method.getReturnType().equals(boolean.class)) return false;
            if (method.getReturnType().equals(int.class)) return 0;
            if (method.getReturnType().equals(long.class)) return 0L;
            if (method.getReturnType().equals(Optional.class)) return Optional.empty();
            if (method.getReturnType().equals(List.class)) return Collections.emptyList();
            if (method.getReturnType().equals(BigDecimal.class)) return BigDecimal.ZERO;
            return null;
        });
    }
}
