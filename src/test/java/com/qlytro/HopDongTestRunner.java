package com.qlytro;

import com.qlytro.dto.response.HopDongResponse;
import com.qlytro.entity.HopDong;
import com.qlytro.entity.HopDongKhachThue;
import com.qlytro.entity.KhachThue;
import com.qlytro.entity.Phong;
import com.qlytro.entity.ToaNha;
import com.qlytro.entity.enums.TrangThaiHopDong;
import com.qlytro.repository.HopDongKhachThueRepository;
import com.qlytro.repository.HopDongRepository;
import com.qlytro.service.impl.HopDongService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class HopDongTestRunner {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("BẮT ĐẦU CHẠY KIỂM THỬ QUẢN LÝ HỢP ĐỒNG");
        System.out.println("=================================================");

        testSapXepVaThuTuTrangThai();
        testTimKiemDaTruong();
        testBoLocTrangThai();
        testLogicSapHetHan30Ngay();
        testPhanTrangVaKhongNPlus1();
        testEmptyStateVaReset();
        testNhanTrangThaiTungDongRiengBiet();

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

    private static void testSapXepVaThuTuTrangThai() {
        // Thứ tự mong đợi: ACTIVE -> KET_THUC_BINH_THUONG -> KET_THUC_BAT_THUONG
        // Trong cùng nhóm: ngayBatDau giảm dần
        ToaNha tn = ToaNha.builder().id(1).tenToaNha("Tòa Alpha").build();
        Phong p = Phong.builder().id(10).soPhong("101").toaNha(tn).build();

        HopDong hd1 = HopDong.builder().id(1).phong(p).trangThai(TrangThaiHopDong.ACTIVE)
                .ngayBatDau(LocalDate.of(2026, 6, 1)).tienThue(BigDecimal.valueOf(3000000)).build();
        HopDong hd2 = HopDong.builder().id(2).phong(p).trangThai(TrangThaiHopDong.ACTIVE)
                .ngayBatDau(LocalDate.of(2026, 1, 1)).tienThue(BigDecimal.valueOf(3000000)).build();
        HopDong hd3 = HopDong.builder().id(3).phong(p).trangThai(TrangThaiHopDong.KET_THUC_BINH_THUONG)
                .ngayBatDau(LocalDate.of(2025, 6, 1)).tienThue(BigDecimal.valueOf(2800000)).build();
        HopDong hd4 = HopDong.builder().id(4).phong(p).trangThai(TrangThaiHopDong.KET_THUC_BAT_THUONG)
                .ngayBatDau(LocalDate.of(2025, 1, 1)).tienThue(BigDecimal.valueOf(2500000)).build();

        List<HopDong> list = List.of(hd1, hd2, hd3, hd4);

        HopDongRepository hdRepo = mockRepo(HopDongRepository.class, Map.of(
                "findHopDongFilterAndPage", args -> new PageImpl<>(list, (Pageable) args[3], 4)
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of(
                "findDaiDienByHopDongIds", args -> Collections.emptyList()
        ));

        HopDongService service = new HopDongService(hdRepo, hdktRepo, null, null, null, null, null, null, null, null);
        Page<HopDongResponse> page = service.findHopDongPage(null, "ALL", PageRequest.of(0, 10));

        assertTrue(page.getTotalElements() == 4, "Tổng số 4 hợp đồng");
        assertTrue(page.getContent().get(0).getTrangThai() == TrangThaiHopDong.ACTIVE, "Vị trí 1 là ACTIVE (mới hơn)");
        assertTrue(page.getContent().get(1).getTrangThai() == TrangThaiHopDong.ACTIVE, "Vị trí 2 là ACTIVE (cũ hơn)");
        assertTrue(page.getContent().get(2).getTrangThai() == TrangThaiHopDong.KET_THUC_BINH_THUONG, "Vị trí 3 là KET_THUC_BINH_THUONG");
        assertTrue(page.getContent().get(3).getTrangThai() == TrangThaiHopDong.KET_THUC_BAT_THUONG, "Vị trí 4 là KET_THUC_BAT_THUONG");
        assertTrue(page.getContent().get(0).getNgayBatDau().isAfter(page.getContent().get(1).getNgayBatDau()), "Trong nhóm ACTIVE: ngày bắt đầu giảm dần");
    }

    private static void testTimKiemDaTruong() {
        final String[] capturedKeyword = new String[1];
        HopDongRepository hdRepo = mockRepo(HopDongRepository.class, Map.of(
                "findHopDongFilterAndPage", args -> {
                    capturedKeyword[0] = (String) args[2];
                    return Page.empty();
                }
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        HopDongService service = new HopDongService(hdRepo, hdktRepo, null, null, null, null, null, null, null, null);

        service.findHopDongPage("  p101  ", "ALL", PageRequest.of(0, 10));
        assertTrue("p101".equals(capturedKeyword[0]), "Tìm kiếm đã trim khoảng trắng từ khóa: 'p101'");

        service.findHopDongPage("", "ALL", PageRequest.of(0, 10));
        assertTrue(capturedKeyword[0] == null, "Từ khóa rỗng được chuyển thành null để không lọc thừa");
    }

    private static void testBoLocTrangThai() {
        final String[] capturedFilter = new String[1];
        HopDongRepository hdRepo = mockRepo(HopDongRepository.class, Map.of(
                "findHopDongFilterAndPage", args -> {
                    capturedFilter[0] = (String) args[0];
                    return Page.empty();
                }
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        HopDongService service = new HopDongService(hdRepo, hdktRepo, null, null, null, null, null, null, null, null);

        service.findHopDongPage(null, "active", PageRequest.of(0, 10));
        assertTrue("ACTIVE".equals(capturedFilter[0]), "Bộ lọc 'active' chuyển thành chữ hoa 'ACTIVE'");

        service.findHopDongPage(null, "SAP_HET_HAN", PageRequest.of(0, 10));
        assertTrue("SAP_HET_HAN".equals(capturedFilter[0]), "Bộ lọc 'SAP_HET_HAN' nhận diện đúng");

        service.findHopDongPage(null, "DA_KET_THUC", PageRequest.of(0, 10));
        assertTrue("DA_KET_THUC".equals(capturedFilter[0]), "Bộ lọc 'DA_KET_THUC' nhận diện đúng");

        service.findHopDongPage(null, null, PageRequest.of(0, 10));
        assertTrue("ALL".equals(capturedFilter[0]), "Bộ lọc mặc định khi null là 'ALL'");
    }

    private static void testLogicSapHetHan30Ngay() {
        LocalDate today = LocalDate.now();
        ToaNha tn = ToaNha.builder().id(1).tenToaNha("Tòa B").build();
        Phong p = Phong.builder().id(10).soPhong("201").toaNha(tn).build();

        // Sắp hết hạn (còn 15 ngày)
        HopDong hd1 = HopDong.builder().id(1).phong(p).trangThai(TrangThaiHopDong.ACTIVE)
                .ngayBatDau(today.minusMonths(5)).ngayKetThuc(today.plusDays(15)).build();

        // Còn dài hạn (còn 60 ngày)
        HopDong hd2 = HopDong.builder().id(2).phong(p).trangThai(TrangThaiHopDong.ACTIVE)
                .ngayBatDau(today.minusMonths(2)).ngayKetThuc(today.plusDays(60)).build();

        // Đã kết thúc (ngayKetThuc trong quá khứ nhưng trạng thái KET_THUC_BINH_THUONG)
        HopDong hd3 = HopDong.builder().id(3).phong(p).trangThai(TrangThaiHopDong.KET_THUC_BINH_THUONG)
                .ngayBatDau(today.minusMonths(12)).ngayKetThuc(today.minusDays(5)).build();

        List<HopDong> list = List.of(hd1, hd2, hd3);

        HopDongRepository hdRepo = mockRepo(HopDongRepository.class, Map.of(
                "findHopDongFilterAndPage", args -> new PageImpl<>(list, (Pageable) args[3], 3)
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of(
                "findDaiDienByHopDongIds", args -> Collections.emptyList()
        ));

        HopDongService service = new HopDongService(hdRepo, hdktRepo, null, null, null, null, null, null, null, null);
        Page<HopDongResponse> page = service.findHopDongPage(null, "ALL", PageRequest.of(0, 10));

        assertTrue(page.getContent().get(0).isSapHetHan(), "Hợp đồng còn 15 ngày được gắn cờ sapHetHan = true");
        assertTrue(!page.getContent().get(1).isSapHetHan(), "Hợp đồng còn 60 ngày sapHetHan = false");
        assertTrue(!page.getContent().get(2).isSapHetHan(), "Hợp đồng đã kết thúc không bị gắn cờ sắp hết hạn");
    }

    private static void testPhanTrangVaKhongNPlus1() {
        ToaNha tn = ToaNha.builder().id(1).tenToaNha("Tòa C").build();
        Phong p = Phong.builder().id(10).soPhong("301").toaNha(tn).build();

        List<HopDong> list10 = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            list10.add(HopDong.builder().id(i).phong(p).trangThai(TrangThaiHopDong.ACTIVE)
                    .ngayBatDau(LocalDate.of(2026, 1, 1)).tienThue(BigDecimal.valueOf(3000000)).build());
        }

        KhachThue kt = KhachThue.builder().id(99).hoTen("Nguyễn Văn Đại Diện").sdt("0909090909").build();
        HopDongKhachThue hdkt = HopDongKhachThue.builder().id(1).hopDong(list10.get(0)).khachThue(kt).laDaiDien(true).dangO(true).build();

        final int[] daiDienQueryCount = new int[1];
        HopDongRepository hdRepo = mockRepo(HopDongRepository.class, Map.of(
                "findHopDongFilterAndPage", args -> new PageImpl<>(list10, (Pageable) args[3], 35) // 35 tổng số
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of(
                "findDaiDienByHopDongIds", args -> {
                    daiDienQueryCount[0]++;
                    return List.of(hdkt);
                }
        ));

        HopDongService service = new HopDongService(hdRepo, hdktRepo, null, null, null, null, null, null, null, null);
        Page<HopDongResponse> page = service.findHopDongPage(null, "ALL", PageRequest.of(0, 10));

        assertTrue(page.getSize() == 10, "Kích thước trang là 10");
        assertTrue(page.getTotalElements() == 35, "Tổng số kết quả sau lọc là 35");
        assertTrue(page.getTotalPages() == 4, "Tổng số 4 trang (35 / 10 = 4 trang)");
        assertTrue(daiDienQueryCount[0] == 1, "Chỉ thực hiện đúng 1 truy vấn batch cho người đại diện (Không N+1)");
        assertTrue("Nguyễn Văn Đại Diện".equals(page.getContent().get(0).getDaiDien().getHoTen()), "Người đại diện được ánh xạ đúng");
    }

    private static void testEmptyStateVaReset() {
        HopDongRepository hdRepo = mockRepo(HopDongRepository.class, Map.of(
                "findHopDongFilterAndPage", args -> Page.empty()
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of());

        HopDongService service = new HopDongService(hdRepo, hdktRepo, null, null, null, null, null, null, null, null);
        Page<HopDongResponse> page = service.findHopDongPage("xyz không tồn tại", "ACTIVE", PageRequest.of(0, 10));

        assertTrue(page.isEmpty(), "Kết quả rỗng khi không khớp từ khóa");
        assertTrue(page.getTotalElements() == 0, "totalElements = 0");
    }

    private static void testNhanTrangThaiTungDongRiengBiet() {
        ToaNha tn = ToaNha.builder().id(1).tenToaNha("Tòa D").build();
        Phong p = Phong.builder().id(10).soPhong("401").toaNha(tn).build();

        HopDong hd1 = HopDong.builder().id(1).phong(p).trangThai(TrangThaiHopDong.ACTIVE).build();
        HopDong hd2 = HopDong.builder().id(2).phong(p).trangThai(TrangThaiHopDong.KET_THUC_BINH_THUONG).build();
        HopDong hd3 = HopDong.builder().id(3).phong(p).trangThai(TrangThaiHopDong.KET_THUC_BAT_THUONG).build();

        HopDongRepository hdRepo = mockRepo(HopDongRepository.class, Map.of(
                "findHopDongFilterAndPage", args -> new PageImpl<>(List.of(hd1, hd2, hd3), (Pageable) args[3], 3)
        ));
        HopDongKhachThueRepository hdktRepo = mockRepo(HopDongKhachThueRepository.class, Map.of(
                "findDaiDienByHopDongIds", args -> Collections.emptyList()
        ));

        HopDongService service = new HopDongService(hdRepo, hdktRepo, null, null, null, null, null, null, null, null);
        Page<HopDongResponse> page = service.findHopDongPage(null, "DA_KET_THUC", PageRequest.of(0, 10));

        assertTrue("Đang hiệu lực".equals(page.getContent().get(0).getTrangThaiLabel()), "Dòng 1 giữ nhãn riêng 'Đang hiệu lực'");
        assertTrue("Kết thúc bình thường".equals(page.getContent().get(1).getTrangThaiLabel()), "Dòng 2 giữ nhãn riêng 'Kết thúc bình thường'");
        assertTrue("Kết thúc bất thường".equals(page.getContent().get(2).getTrangThaiLabel()), "Dòng 3 giữ nhãn riêng 'Kết thúc bất thường'");
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
            if (method.getReturnType().equals(Page.class)) return Page.empty();
            return null;
        });
    }
}
