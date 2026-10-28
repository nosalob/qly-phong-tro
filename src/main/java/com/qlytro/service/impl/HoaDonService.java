package com.qlytro.service.impl;

import com.qlytro.dto.response.HoaDonChiTietResponse;
import com.qlytro.dto.response.HoaDonResponse;
import com.qlytro.entity.*;
import com.qlytro.entity.enums.TrangThaiChiSo;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.*;
import com.qlytro.service.base.BaseService;
import com.qlytro.service.strategy.TinhTienContext;
import com.qlytro.service.strategy.TinhTienStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class HoaDonService implements BaseService<HoaDonResponse, Integer> {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository chiTietRepository;
    private final HopDongRepository hopDongRepository;
    private final HopDongKhachThueRepository hdktRepository;
    private final ChiSoDienNuocRepository chiSoRepository;
    private final BangGiaDienNuocRepository bangGiaRepository;
    private final PhiDichVuRepository phiDichVuRepository;
    private final KhachThueRepository khachThueRepository;

    // Strategy Pattern: danh sach tat ca cac chien luoc tinh tien duoc Spring inject tu dong
    private final List<TinhTienStrategy> strategies;

    public HoaDonService(HoaDonRepository hoaDonRepository,
                         HoaDonChiTietRepository chiTietRepository,
                         HopDongRepository hopDongRepository,
                         HopDongKhachThueRepository hdktRepository,
                         ChiSoDienNuocRepository chiSoRepository,
                         BangGiaDienNuocRepository bangGiaRepository,
                         PhiDichVuRepository phiDichVuRepository,
                         KhachThueRepository khachThueRepository,
                         List<TinhTienStrategy> strategies) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietRepository = chiTietRepository;
        this.hopDongRepository = hopDongRepository;
        this.hdktRepository = hdktRepository;
        this.chiSoRepository = chiSoRepository;
        this.bangGiaRepository = bangGiaRepository;
        this.phiDichVuRepository = phiDichVuRepository;
        this.khachThueRepository = khachThueRepository;
        this.strategies = strategies;
    }

    @Override
    @Transactional(readOnly = true)
    public HoaDonResponse findById(Integer id) {
        return toResponse(hoaDonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoaDonResponse> findAll() {
        return hoaDonRepository.findAllOrderByTrangThaiAndThangNam()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    // [THÊM MỚI] Lọc hóa đơn dành cho chủ trọ
    @Transactional(readOnly = true)
    public List<HoaDonResponse> findAllFiltered(
            Integer toaNhaId,
            Integer phongId,
            String tuThang,
            String denThang,
            TrangThaiHoaDon trangThai) {

        String cleanTuThang = (tuThang != null && !tuThang.isBlank()) ? tuThang.trim() : null;
        String cleanDenThang = (denThang != null && !denThang.isBlank()) ? denThang.trim() : null;

        return hoaDonRepository.findAllFiltered(
                        toaNhaId,
                        phongId,
                        cleanTuThang,
                        cleanDenThang,
                        trangThai)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    /**
     * Lay danh sach hoa don cua khach thue dang dang nhap.
     * Truy van: nguoi_dung_id -> khach_thue -> hop_dong -> hoa_don
     */
    @Transactional(readOnly = true)
    public List<HoaDonResponse> findByNguoiDungId(Integer nguoiDungId) {
        // Tim khach_thue tu nguoi_dung_id
        com.qlytro.entity.KhachThue khachThue = khachThueRepository
                .findByNguoiDung_Id(nguoiDungId)
                .orElse(null);
        if (khachThue == null) return java.util.List.of();

        // Tim tat ca hop_dong cua khach nay (qua bang trung gian)
        List<Integer> hopDongIds = hdktRepository.findByKhachThue_Id(khachThue.getId())
                .stream()
                .map(hdkt -> hdkt.getHopDong().getId())
                .toList();

        if (hopDongIds.isEmpty()) return java.util.List.of();

        // Lay hoa don cua tat ca hop dong do
        return hopDongIds.stream()
                .flatMap(hopDongId -> hoaDonRepository
                        .findByHopDong_Id(hopDongId).stream())
                .sorted(
                        java.util.Comparator
                                .comparingInt((HoaDon hd) -> {
                                    if (hd.getTrangThai() == TrangThaiHoaDon.CHUA_THANH_TOAN) return 0;
                                    if (hd.getTrangThai() == TrangThaiHoaDon.CHO_DUYET) return 1;
                                    if (hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) return 2;
                                    return 3;
                                })
                                .thenComparing(HoaDon::getThangNam,
                                        java.util.Comparator.reverseOrder())
                )
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HoaDonResponse findByIdOfNguoiDung(Integer id, Integer nguoiDungId) {
        return toResponse(getHoaDonOfNguoiDung(id, nguoiDungId));
    }

    private HoaDon getHoaDonOfNguoiDung(Integer id, Integer nguoiDungId) {
        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn", id));
        if (nguoiDungId != null) {
            KhachThue khachThue = khachThueRepository
                    .findByNguoiDung_Id(nguoiDungId)
                    .orElse(null);
            if (khachThue == null || !hdktRepository.existsByHopDong_IdAndKhachThue_Id(hd.getHopDong().getId(), khachThue.getId())) {
                throw new ResourceNotFoundException("Hóa đơn", id);
            }
        }
        return hd;
    }

    /** Khach thue nop tien (bao da chuyen khoan): POST form don gian. */
    public HoaDonResponse khachNopTienForm(Integer id, Integer nguoiDungId) {
        HoaDon hd = getHoaDonOfNguoiDung(id, nguoiDungId);
        if (!TrangThaiHoaDon.CHUA_THANH_TOAN.equals(hd.getTrangThai())) {
            throw new BusinessException("Hóa đơn này không ở trạng thái Chưa thanh toán.");
        }
        hd.setTrangThai(TrangThaiHoaDon.CHO_DUYET);
        return toResponse(hoaDonRepository.save(hd));
    }

    public HoaDonResponse khachNopTienForm(Integer id) {
        return khachNopTienForm(id, null);
    }

    /**
     * Tao hoa don cho 1 hop dong trong 1 thang cu the.
     * Quy trinh:
     * 1. Kiem tra hop dong con ACTIVE.
     * 2. Kiem tra chua co hoa don cho thang nay.
     * 3. Lay chi so dien nuoc DA_CHOT cua phong trong thang nay.
     * 4. Dung Strategy Pattern de tinh tung dong chi tiet.
     * 5. Tinh tong_tien = sum(thanh_tien).
     * 6. Luu hoa_don va tat ca hoa_don_chi_tiet.
     */
    public HoaDonResponse taoHoaDon(Integer hopDongId, String thangNam) {
        HopDong hopDong = hopDongRepository.findById(hopDongId)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng", hopDongId));

        if (!com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE.equals(hopDong.getTrangThai())) {
            throw new BusinessException("Hợp đồng đã kết thúc, không thể tạo hóa đơn mới.");
        }
        if (hoaDonRepository.existsByHopDong_IdAndThangNam(hopDongId, thangNam)) {
            throw new BusinessException("Hóa đơn tháng " + thangNam + " đã tồn tại.");
        }

        YearMonth ymThangHoaDon = YearMonth.parse(thangNam);

        if (hopDong.getNgayBatDau() != null && ymThangHoaDon.isBefore(YearMonth.from(hopDong.getNgayBatDau()))) {
            throw new BusinessException("Tháng lập hóa đơn (" + thangNam + ") không được trước ngày bắt đầu hợp đồng ("
                    + hopDong.getNgayBatDau() + ").");
        }
        if (hopDong.getNgayKetThuc() != null && ymThangHoaDon.isAfter(YearMonth.from(hopDong.getNgayKetThuc()))) {
            throw new BusinessException("Hợp đồng chỉ có thời hạn đến "
                    + hopDong.getNgayKetThuc() + ". Không thể tạo hóa đơn cho tháng " + thangNam + ".");
        }

        YearMonth ymThangBatDau = YearMonth.from(hopDong.getNgayBatDau());

        // Neu thang hoa don la thang bat dau hop dong -> chi thu tien phong + phi dich vu, chua co dien nuoc ky truoc
        boolean laThangDau = ymThangHoaDon.equals(ymThangBatDau);
        String thangTruoc = ymThangHoaDon.minusMonths(1).toString();
        ChiSoDienNuoc chiSo = null;

        if (!laThangDau) {
            // Cac thang tiep theo: bat buoc phai co chi so dien nuoc thang truoc da chot
            chiSo = chiSoRepository
                    .findByPhong_IdAndThangNam(hopDong.getPhong().getId(), thangTruoc)
                    .orElse(null);

            if (chiSo == null || !TrangThaiChiSo.DA_CHOT.equals(chiSo.getTrangThai())) {
                throw new BusinessException("Phòng chưa có chỉ số điện nước tháng trước ("
                        + thangTruoc + ") hoặc chỉ số chưa được chốt.");
            }
        }

        BangGiaDienNuoc bangGia = bangGiaRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new BusinessException("Chưa có bảng giá điện nước. Vui lòng thiết lập trước."));

        List<PhiDichVu> danhSachPhi = phiDichVuRepository.findByDangApDung(true);

        // Xay dung context cho Strategy Pattern
        TinhTienContext context = TinhTienContext.builder()
                .hopDong(hopDong)
                .chiSoDienNuoc(chiSo)
                .bangGiaDienNuoc(bangGia)
                .danhSachPhiDichVu(danhSachPhi)
                .thangNam(thangNam)
                .build();

        // Tao hoa don truoc (can id de lien ket chi tiet)
        HoaDon hoaDon = new HoaDon();
        hoaDon.setHopDong(hopDong);
        hoaDon.setThangNam(thangNam);
        hoaDon.setTrangThai(TrangThaiHoaDon.CHUA_THANH_TOAN);
        hoaDon.setTongTien(BigDecimal.ZERO); // se cap nhat sau
        hoaDon = hoaDonRepository.save(hoaDon);

        // Goi tat ca Strategy -> thu thap chi tiet da hinh theo thu tu @Order
        List<HoaDonChiTiet> danhSachChiTiet = new ArrayList<>();
        strategies.forEach(s -> danhSachChiTiet.addAll(s.tinh(context)));

        // Gan hoa_don_id va luu
        final HoaDon finalHoaDon = hoaDon;
        danhSachChiTiet.forEach(ct -> ct.setHoaDon(finalHoaDon));
        chiTietRepository.saveAll(danhSachChiTiet);

        // Tinh tong tien va cap nhat
        BigDecimal tongTien = danhSachChiTiet.stream()
                .map(HoaDonChiTiet::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        hoaDon.setTongTien(tongTien);
        hoaDon = hoaDonRepository.save(hoaDon);

        return toResponse(hoaDon);
    }

    public void validateKhachNopTien(Integer id, Integer nguoiDungId) {
        HoaDon hd = getHoaDonOfNguoiDung(id, nguoiDungId);
        if (!TrangThaiHoaDon.CHUA_THANH_TOAN.equals(hd.getTrangThai())) {
            throw new BusinessException("Hóa đơn này không ở trạng thái Chưa thanh toán.");
        }
    }

    /** Khach nop tien: upload anh chuyen khoan, chuyen sang CHO_DUYET. */
    public HoaDonResponse khachNopTien(Integer id, String anhChuyenKhoan, Integer nguoiDungId) {
        validateKhachNopTien(id, nguoiDungId);
        HoaDon hd = getHoaDonOfNguoiDung(id, nguoiDungId);
        hd.setAnhChuyenKhoan(anhChuyenKhoan);
        hd.setTrangThai(TrangThaiHoaDon.CHO_DUYET);
        return toResponse(hoaDonRepository.save(hd));
    }

    public HoaDonResponse khachNopTien(Integer id, String anhChuyenKhoan) {
        return khachNopTien(id, anhChuyenKhoan, null);
    }

    /** Chu tro xac nhan da nhan tien. */
    public HoaDonResponse xacNhanThanhToan(Integer id) {
        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn", id));
        if (!TrangThaiHoaDon.CHO_DUYET.equals(hd.getTrangThai())) {
            throw new BusinessException("Hóa đơn này không ở trạng thái Chờ duyệt.");
        }
        hd.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
        hd.setNgayThanhToan(LocalDateTime.now());
        return toResponse(hoaDonRepository.save(hd));
    }

    private final java.util.Map<Integer, String> lyDoTuChoiMap = new java.util.concurrent.ConcurrentHashMap<>();

    /** Chu tro tu choi thanh toan -> dua hoa don ve CHUA_THANH_TOAN. */
    public HoaDonResponse tuChoiThanhToan(Integer id, String lyDo) {
        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn", id));
        if (!TrangThaiHoaDon.CHO_DUYET.equals(hd.getTrangThai())) {
            throw new BusinessException("Chỉ có thể từ chối hóa đơn đang ở trạng thái Chờ duyệt.");
        }
        hd.setTrangThai(TrangThaiHoaDon.CHUA_THANH_TOAN);
        hd.setAnhChuyenKhoan(null);
        if (lyDo != null && !lyDo.isBlank()) {
            lyDoTuChoiMap.put(id, lyDo);
        }
        return toResponse(hoaDonRepository.save(hd));
    }

    public HoaDonResponse tuChoiThanhToan(Integer id) {
        return tuChoiThanhToan(id, null);
    }

    @Override
    public void deleteById(Integer id) {
        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn", id));
        if (!TrangThaiHoaDon.CHUA_THANH_TOAN.equals(hd.getTrangThai())) {
            throw new BusinessException("Chỉ có thể xóa hóa đơn chưa thanh toán.");
        }
        chiTietRepository.deleteByHoaDon_Id(id);
        hoaDonRepository.delete(hd);
    }

    private HoaDonResponse toResponse(HoaDon entity) {
        HoaDonResponse r = new HoaDonResponse();
        r.setId(entity.getId());
        r.setHopDongId(entity.getHopDong().getId());
        r.setSoPhong(entity.getHopDong().getPhong().getSoPhong());
        r.setTenToaNha(entity.getHopDong().getPhong().getToaNha().getTenToaNha());
        r.setThangNam(entity.getThangNam());
        r.setTongTien(entity.getTongTien());
        r.setTrangThai(entity.getTrangThai());
        r.setTrangThaiLabel(entity.getTrangThai().getLabel());
        r.setAnhChuyenKhoan(entity.getAnhChuyenKhoan());
        r.setNgayTao(entity.getNgayTao());
        r.setNgayThanhToan(entity.getNgayThanhToan());
        r.setLyDoTuChoi(lyDoTuChoiMap.get(entity.getId()));

        // Load chi tiet
        List<HoaDonChiTietResponse> danhSachChiTiet = chiTietRepository
                .findByHoaDon_Id(entity.getId()).stream()
                .map(ct -> {
                    HoaDonChiTietResponse ctr = new HoaDonChiTietResponse();
                    ctr.setId(ct.getId());
                    ctr.setLoaiKhoan(ct.getLoaiKhoan());
                    ctr.setLoaiKhoanLabel(ct.getLoaiKhoan().getLabel());
                    ctr.setNoiDung(ct.getNoiDung());
                    ctr.setSoLuong(ct.getSoLuong());
                    ctr.setDonGia(ct.getDonGia());
                    ctr.setThanhTien(ct.getThanhTien());
                    return ctr;
                }).toList();
        r.setDanhSachChiTiet(danhSachChiTiet);

        // Lay ten khach dai dien qua repository (HopDong entity khong co collection nay)
        hdktRepository.findByHopDong_Id(entity.getHopDong().getId()).stream()
                .filter(hdkt -> Boolean.TRUE.equals(hdkt.getLaDaiDien()) && Boolean.TRUE.equals(hdkt.getDangO()))
                .findFirst()
                .ifPresent(hdkt -> r.setTenKhachDaiDien(hdkt.getKhachThue().getHoTen()));
        return r;
    }
}
