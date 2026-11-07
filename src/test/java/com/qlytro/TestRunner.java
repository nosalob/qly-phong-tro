package com.qlytro;

import com.qlytro.dto.request.NhacThanhToanRequest;
import com.qlytro.dto.response.HoaDonResponse;
import com.qlytro.dto.response.NhacThanhToanResponse;
import com.qlytro.entity.*;
import com.qlytro.entity.enums.HinhThucNhac;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.entity.enums.TrangThaiHopDong;
import com.qlytro.exception.BusinessException;
import com.qlytro.repository.*;
import com.qlytro.service.impl.HoaDonService;
import com.qlytro.service.impl.NhacThanhToanService;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

public class TestRunner {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("BẮT ĐẦU CHẠY KIỂM THỬ CÁC QUY TẮC NGHIỆP VỤ MỚI");
        System.out.println("=================================================");

        testMocQuaHan();
        testHanThanhToanVaNgayQuaHan();
        testChanLapHoaDonThangTiepTheoKhiThangTruocChuaThanhToan();
        testChanLapHoaDonThangTiepTheoKhiThangTruocChoDuyet();
        testChoPhepLapHoaDonThangDauTien();
        testLuuSnapshotNguoiDaiDienVaoHoaDon();
        testChanTaoHoaDonKhiHopDongKhongCoDaiDien();
        testGhiNhanNhacThanhToanThanhCongVaKhongDoiTrangThaiHoaDon();
        testChanGhiNhanNhacThanhToanKhiHoaDonDaThanhToan();

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

    private static void testMocQuaHan() {
        // Ngày 05/11/2026: mốc tính là 2026-10 (hóa đơn tháng 11 chưa quá hạn)
        LocalDate day5 = LocalDate.of(2026, 11, 5);
        String mocDay5 = HoaDonService.getThangNamMocQuaHan(day5);
        assertTrue("2026-10".equals(mocDay5), "Ngày 05 hàng tháng: HĐ tháng hiện tại chưa quá hạn");

        // Ngày 06/11/2026: mốc tính là 2026-11 (hóa đơn tháng 11 quá hạn)
        LocalDate day6 = LocalDate.of(2026, 11, 6);
        String mocDay6 = HoaDonService.getThangNamMocQuaHan(day6);
        assertTrue("2026-11".equals(mocDay6), "Từ ngày 06 hàng tháng: HĐ tháng hiện tại được tính là quá hạn");

        // Ngày 01/11/2026: mốc tính là 2026-10
        LocalDate day1 = LocalDate.of(2026, 11, 1);
        String mocDay1 = HoaDonService.getThangNamMocQuaHan(day1);
        assertTrue("2026-10".equals(mocDay1), "Đầu tháng (ngày 01): HĐ tháng hiện tại chưa quá hạn");
    }

    private static void testHanThanhToanVaNgayQuaHan() {
        YearMonth ym = YearMonth.parse("2026-11");
        LocalDate hanTT = ym.atDay(5);
        assertTrue(hanTT.getDayOfMonth() == 5 && hanTT.getMonthValue() == 11, "Hạn thanh toán luôn là ngày 05 của tháng hóa đơn");

        LocalDate todayOverdue = LocalDate.of(2026, 11, 8);
        long days = java.time.temporal.ChronoUnit.DAYS.between(hanTT, todayOverdue);
        assertTrue(days == 3, "Tính chính xác số ngày quá hạn (ngày 08 so với hạn ngày 05 là 3 ngày)");
    }

    private static void testChanLapHoaDonThangTiepTheoKhiThangTruocChuaThanhToan() {
        HopDong hd = taoHopDongMau();
        HoaDon hdThangTruoc = HoaDon.builder()
                .id(1)
                .hopDong(hd)
                .thangNam("2026-09")
                .trangThai(TrangThaiHoaDon.CHUA_THANH_TOAN)
                .build();

        HopDongRepository hopDongRepo = mockRepo(HopDongRepository.class, Map.of(
                "findById", args -> Optional.of(hd)
        ));
        HoaDonRepository hoaDonRepo = mockRepo(HoaDonRepository.class, Map.of(
                "existsByHopDong_IdAndThangNam", args -> false,
                "findByHopDong_IdAndThangNam", args -> "2026-09".equals(args[1]) ? Optional.of(hdThangTruoc) : Optional.empty()
        ));

        HoaDonService service = taoHoaDonService(hoaDonRepo, hopDongRepo, null, null);

        try {
            service.taoHoaDon(100, "2026-10");
            assertTrue(false, "Chặn lập HĐ tháng tiếp theo khi tháng trước chưa thanh toán (mong đợi ngoại lệ)");
        } catch (BusinessException e) {
            assertTrue(e.getMessage().contains("chưa được thanh toán hoàn tất"),
                    "Chặn lập HĐ tháng tiếp theo khi tháng trước CHUA_THANH_TOAN");
        }
    }

    private static void testChanLapHoaDonThangTiepTheoKhiThangTruocChoDuyet() {
        HopDong hd = taoHopDongMau();
        HoaDon hdThangTruoc = HoaDon.builder()
                .id(1)
                .hopDong(hd)
                .thangNam("2026-09")
                .trangThai(TrangThaiHoaDon.CHO_DUYET)
                .build();

        HopDongRepository hopDongRepo = mockRepo(HopDongRepository.class, Map.of(
                "findById", args -> Optional.of(hd)
        ));
        HoaDonRepository hoaDonRepo = mockRepo(HoaDonRepository.class, Map.of(
                "existsByHopDong_IdAndThangNam", args -> false,
                "findByHopDong_IdAndThangNam", args -> "2026-09".equals(args[1]) ? Optional.of(hdThangTruoc) : Optional.empty()
        ));

        HoaDonService service = taoHoaDonService(hoaDonRepo, hopDongRepo, null, null);

        try {
            service.taoHoaDon(100, "2026-10");
            assertTrue(false, "Chặn lập HĐ tháng tiếp theo khi tháng trước đang CHO_DUYET (mong đợi ngoại lệ)");
        } catch (BusinessException e) {
            assertTrue(e.getMessage().contains("chưa được thanh toán hoàn tất"),
                    "Chặn lập HĐ tháng tiếp theo khi tháng trước CHO_DUYET");
        }
    }

    private static void testChoPhepLapHoaDonThangDauTien() {
        HopDong hd = taoHopDongMau(); // Bắt đầu ngày 2026-09-01
        KhachThue kt = taoKhachThueMau();
        HopDongKhachThue hdkt = HopDongKhachThue.builder().id(1).hopDong(hd).khachThue(kt).laDaiDien(true).dangO(true).build();

        HopDongRepository hopDongRepo = mockRepo(HopDongRepository.class, Map.of(
                "findById", args -> Optional.of(hd)
        ));
        HoaDonRepository hoaDonRepo = mockRepo(HoaDonRepository.class, Map.of(
                "existsByHopDong_IdAndThangNam", args -> false,
                "save", args -> {
                    HoaDon entity = (HoaDon) args[0];
                    if (entity.getId() == null) entity.setId(10);
                    return entity;
                }
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of(
                "findByHopDong_Id", args -> List.of(hdkt)
        ));
        BangGiaDienNuocRepository bangGiaRepo = mockRepo(BangGiaDienNuocRepository.class, Map.of(
                "findTopByOrderByIdAsc", args -> Optional.of(BangGiaDienNuoc.builder().id(1).build())
        ));
        PhiDichVuRepository phiRepo = mockRepo(PhiDichVuRepository.class, Map.of(
                "findByDangApDung", args -> Collections.emptyList()
        ));
        HoaDonChiTietRepository chiTietRepo = mockRepo(HoaDonChiTietRepository.class, Map.of(
                "findByHoaDon_Id", args -> Collections.emptyList(),
                "saveAll", args -> args[0]
        ));

        HoaDonService service = new HoaDonService(
                hoaDonRepo, chiTietRepo, hopDongRepo, hdktRepo,
                null, bangGiaRepo, phiRepo, null, null, Collections.emptyList()
        );

        HoaDonResponse response = service.taoHoaDon(100, "2026-09");
        assertTrue(response != null && "2026-09".equals(response.getThangNam()),
                "Miễn kiểm tra hóa đơn tháng liền trước khi lập hóa đơn tháng đầu tiên của hợp đồng");
        assertTrue(response.isCoSnapshotDaiDien(), "Hóa đơn mới có snapshot đại diện = true");
        assertTrue("Trần Văn Đại Diện".equals(response.getTenKhachDaiDien()), "Lấy đúng tên người đại diện snapshot");
    }

    private static void testLuuSnapshotNguoiDaiDienVaoHoaDon() {
        HopDong hd = taoHopDongMau();
        KhachThue kt = taoKhachThueMau();

        HoaDon hdMoi = HoaDon.builder()
                .id(20)
                .hopDong(hd)
                .nguoiDaiDien(kt)
                .thangNam("2026-09")
                .tongTien(BigDecimal.valueOf(1500000))
                .trangThai(TrangThaiHoaDon.CHUA_THANH_TOAN)
                .build();

        assertTrue(hdMoi.getNguoiDaiDien() != null, "Entity HoaDon lưu tham chiếu trực tiếp đến KhachThue đại diện");
        assertTrue("0912345678".equals(hdMoi.getNguoiDaiDien().getSdt()), "Truy xuất chính xác SĐT người đại diện");
    }

    private static void testChanTaoHoaDonKhiHopDongKhongCoDaiDien() {
        HopDong hd = taoHopDongMau();

        HopDongRepository hopDongRepo = mockRepo(HopDongRepository.class, Map.of(
                "findById", args -> Optional.of(hd)
        ));
        HoaDonRepository hoaDonRepo = mockRepo(HoaDonRepository.class, Map.of(
                "existsByHopDong_IdAndThangNam", args -> false
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of(
                "findByHopDong_Id", args -> Collections.emptyList()
        ));
        BangGiaDienNuocRepository bangGiaRepo = mockRepo(BangGiaDienNuocRepository.class, Map.of(
                "findTopByOrderByIdAsc", args -> Optional.of(BangGiaDienNuoc.builder().id(1).build())
        ));
        PhiDichVuRepository phiRepo = mockRepo(PhiDichVuRepository.class, Map.of(
                "findByDangApDung", args -> Collections.emptyList()
        ));

        HoaDonService service = new HoaDonService(
                hoaDonRepo, null, hopDongRepo, hdktRepo,
                null, bangGiaRepo, phiRepo, null, null, Collections.emptyList()
        );

        try {
            service.taoHoaDon(100, "2026-09");
            assertTrue(false, "Chặn tạo hóa đơn khi hợp đồng chưa có khách thuê đại diện hợp lệ (mong đợi ngoại lệ)");
        } catch (BusinessException e) {
            assertTrue(e.getMessage().contains("chưa có khách thuê đại diện hợp lệ"),
                    "Chặn tạo hóa đơn khi hợp đồng không có đại diện");
        }
    }

    private static void testGhiNhanNhacThanhToanThanhCongVaKhongDoiTrangThaiHoaDon() {
        HoaDon hd = HoaDon.builder()
                .id(10)
                .trangThai(TrangThaiHoaDon.CHUA_THANH_TOAN)
                .thangNam("2026-11")
                .build();
        NguoiDung user = NguoiDung.builder().id(1).username("admin").hoTen("Chủ Nhà A").build();

        HoaDonRepository hoaDonRepo = mockRepo(HoaDonRepository.class, Map.of(
                "findById", args -> Optional.of(hd)
        ));
        NguoiDungRepository userRepo = mockRepo(NguoiDungRepository.class, Map.of(
                "findById", args -> Optional.of(user)
        ));
        NhacThanhToanRepository nhacRepo = mockRepo(NhacThanhToanRepository.class, Map.of(
                "save", args -> {
                    NhacThanhToan n = (NhacThanhToan) args[0];
                    n.setId(101);
                    n.setThoiGianNhac(LocalDateTime.now());
                    return n;
                }
        ));

        NhacThanhToanService nhacService = new NhacThanhToanService(nhacRepo, hoaDonRepo, userRepo);
        NhacThanhToanRequest request = NhacThanhToanRequest.builder()
                .hinhThuc(HinhThucNhac.GOI_DIEN)
                .noiDung("Đã gọi điện cho người thuê nhắc đóng tiền")
                .build();

        NhacThanhToanResponse resp = nhacService.ghiNhanNhacThanhToan(10, request, 1);

        assertTrue(resp != null && "Chủ Nhà A".equals(resp.getTenNguoiNhac()), "Ghi nhận người nhắc đúng danh tính tài khoản đăng nhập");
        assertTrue(HinhThucNhac.GOI_DIEN.equals(resp.getHinhThuc()), "Ghi nhận đúng hình thức nhắc nhở");
        assertTrue(TrangThaiHoaDon.CHUA_THANH_TOAN.equals(hd.getTrangThai()), "Hóa đơn vẫn giữ nguyên trạng thái CHUA_THANH_TOAN sau khi ghi nhận nhắc nợ");
    }

    private static void testChanGhiNhanNhacThanhToanKhiHoaDonDaThanhToan() {
        HoaDon hd = HoaDon.builder()
                .id(11)
                .trangThai(TrangThaiHoaDon.DA_THANH_TOAN)
                .thangNam("2026-10")
                .build();

        HoaDonRepository hoaDonRepo = mockRepo(HoaDonRepository.class, Map.of(
                "findById", args -> Optional.of(hd)
        ));

        NhacThanhToanService nhacService = new NhacThanhToanService(null, hoaDonRepo, null);
        NhacThanhToanRequest request = NhacThanhToanRequest.builder()
                .hinhThuc(HinhThucNhac.TIN_NHAN)
                .noiDung("Nhắc nợ")
                .build();

        try {
            nhacService.ghiNhanNhacThanhToan(11, request, 1);
            assertTrue(false, "Chặn nhắc thanh toán cho hóa đơn đã thanh toán (mong đợi ngoại lệ)");
        } catch (BusinessException e) {
            assertTrue(e.getMessage().contains("Chỉ được ghi nhận nhắc nhở đối với hóa đơn chưa thanh toán"),
                    "Chặn ghi nhận nhắc nợ cho hóa đơn DA_THANH_TOAN");
        }
    }

    private static HopDong taoHopDongMau() {
        ToaNha tn = ToaNha.builder().id(1).tenToaNha("Tòa Nhà A").build();
        Phong p = Phong.builder().id(10).soPhong("101").toaNha(tn).build();
        return HopDong.builder()
                .id(100)
                .phong(p)
                .trangThai(TrangThaiHopDong.ACTIVE)
                .ngayBatDau(LocalDate.of(2026, 9, 1))
                .ngayKetThuc(LocalDate.of(2027, 8, 31))
                .tienThue(BigDecimal.valueOf(3500000))
                .build();
    }

    private static KhachThue taoKhachThueMau() {
        return KhachThue.builder()
                .id(50)
                .hoTen("Trần Văn Đại Diện")
                .sdt("0912345678")
                .cccd("012345678901")
                .build();
    }

    private static HoaDonService taoHoaDonService(HoaDonRepository hdRepo, HopDongRepository hopDongRepo,
                                                  HopDongKhachThueRepository hdktRepo, NhacThanhToanRepository nhacRepo) {
        return new HoaDonService(
                hdRepo, null, hopDongRepo, hdktRepo,
                null, null, null, null, nhacRepo, Collections.emptyList()
        );
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
            return null;
        });
    }
}

