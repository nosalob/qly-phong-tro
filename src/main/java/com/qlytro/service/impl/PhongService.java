package com.qlytro.service.impl;

import com.qlytro.dto.request.PhongRequest;
import com.qlytro.dto.response.PhongQuanLyResponse;
import com.qlytro.dto.response.PhongResponse;
import com.qlytro.entity.HopDong;
import com.qlytro.entity.HopDongKhachThue;
import com.qlytro.entity.Phong;
import com.qlytro.entity.ToaNha;
import com.qlytro.entity.enums.TrangThaiHopDong;
import com.qlytro.entity.enums.TrangThaiPhong;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.HopDongKhachThueRepository;
import com.qlytro.repository.HopDongRepository;
import com.qlytro.repository.PhongRepository;
import com.qlytro.repository.PhongThietBiRepository;
import com.qlytro.repository.ToaNhaRepository;
import com.qlytro.service.base.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PhongService implements BaseService<PhongResponse, Integer> {

    private static final Logger log = LoggerFactory.getLogger(PhongService.class);

    private final PhongRepository phongRepository;
    private final ToaNhaRepository toaNhaRepository;
    private final HopDongRepository hopDongRepository;
    private final HopDongKhachThueRepository hdktRepository;
    private final PhongThietBiRepository phongThietBiRepository;
    private final com.qlytro.repository.ChiSoDienNuocRepository chiSoRepository;
    private final com.qlytro.repository.SuCoRepository suCoRepository;

    public PhongService(PhongRepository phongRepository,
                        ToaNhaRepository toaNhaRepository,
                        HopDongRepository hopDongRepository,
                        HopDongKhachThueRepository hdktRepository,
                        PhongThietBiRepository phongThietBiRepository,
                        com.qlytro.repository.ChiSoDienNuocRepository chiSoRepository,
                        com.qlytro.repository.SuCoRepository suCoRepository) {
        this.phongRepository = phongRepository;
        this.toaNhaRepository = toaNhaRepository;
        this.hopDongRepository = hopDongRepository;
        this.hdktRepository = hdktRepository;
        this.phongThietBiRepository = phongThietBiRepository;
        this.chiSoRepository = chiSoRepository;
        this.suCoRepository = suCoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PhongResponse findById(Integer id) {
        return toResponse(phongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhongResponse> findAll() {
        return phongRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PhongResponse> findByToaNha(Integer toaNhaId) {
        return phongRepository.findByToaNha_Id(toaNhaId).stream().map(this::toResponse).toList();
    }

    public PhongResponse create(PhongRequest request) {
        // Validate: so phong phai duy nhat trong toa nha
        if (phongRepository.existsByToaNha_IdAndSoPhong(request.getToaNhaId(), request.getSoPhong())) {
            throw new BusinessException("Số phòng '" + request.getSoPhong()
                    + "' đã tồn tại trong tòa nhà này.");
        }
        ToaNha toaNha = toaNhaRepository.findById(request.getToaNhaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tòa nhà", request.getToaNhaId()));
        Phong phong = new Phong();
        mapFromRequestForCreate(request, phong, toaNha);
        return toResponse(phongRepository.save(phong));
    }

    public PhongResponse update(Integer id, PhongRequest request) {
        Phong phong = phongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", id));
        // Validate: so phong phai duy nhat (ngoai tru ban than phong nay)
        if (phongRepository.existsByToaNha_IdAndSoPhongAndIdNot(request.getToaNhaId(), request.getSoPhong(), id)) {
            throw new BusinessException("Số phòng '" + request.getSoPhong()
                    + "' đã tồn tại trong tòa nhà này.");
        }
        ToaNha toaNha = toaNhaRepository.findById(request.getToaNhaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tòa nhà", request.getToaNhaId()));
        mapFromRequestForUpdate(request, phong, toaNha);
        return toResponse(phongRepository.save(phong));
    }

    public PhongResponse ngungSuDung(Integer id) {
        Phong phong = phongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", id));
        if (hopDongRepository.existsByPhong_IdAndTrangThai(id, com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE)) {
            throw new BusinessException("Phòng đang có hợp đồng hoạt động (ACTIVE), không thể ngừng sử dụng.");
        }
        if (!TrangThaiPhong.TRONG.equals(phong.getTrangThai())) {
            throw new BusinessException("Chỉ phòng đang ở trạng thái Trống mới có thể chuyển sang Ngừng sử dụng.");
        }
        phong.setTrangThai(TrangThaiPhong.NGUNG_SU_DUNG);
        return toResponse(phongRepository.save(phong));
    }

    public PhongResponse moLai(Integer id) {
        Phong phong = phongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", id));
        if (!TrangThaiPhong.NGUNG_SU_DUNG.equals(phong.getTrangThai())) {
            throw new BusinessException("Chỉ phòng đang Ngừng sử dụng mới có thể mở lại.");
        }
        phong.setTrangThai(TrangThaiPhong.TRONG);
        return toResponse(phongRepository.save(phong));
    }

    @Override
    public void deleteById(Integer id) {
        Phong phong = phongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", id));
        // Rang buoc: khong xoa phong da co hop dong, chi so hoac su co
        if (hopDongRepository.existsByPhong_Id(id)
                || chiSoRepository.existsByPhong_Id(id)
                || suCoRepository.existsByPhong_Id(id)) {
            throw new BusinessException("Phòng đã có lịch sử hợp đồng, chỉ số điện nước hoặc sự cố. Vui lòng chuyển trạng thái phòng sang 'Ngừng sử dụng' thay vì xóa.");
        }
        phongRepository.delete(phong);
    }

    /**
     * Dung cho CREATE: map tat ca cac truong tu request, bao gom trang_thai.
     * Phong moi co the bat dau o TRONG hoac NGUNG_SU_DUNG.
     */
    private void mapFromRequestForCreate(PhongRequest request, Phong phong, ToaNha toaNha) {
        phong.setToaNha(toaNha);
        phong.setSoPhong(request.getSoPhong());
        phong.setDienTich(request.getDienTich());
        phong.setGiaThueTheoMacDinh(request.getGiaThueTheoMacDinh());
        TrangThaiPhong tt = request.getTrangThai() != null
                ? TrangThaiPhong.fromValue(request.getTrangThai())
                : TrangThaiPhong.TRONG;
        if (TrangThaiPhong.DANG_THUE.equals(tt)) {
            throw new BusinessException("Không thể tạo phòng ở trạng thái 'Đang thuê'. Phòng mới chỉ có thể là Trống hoặc Ngừng sử dụng.");
        }
        phong.setTrangThai(tt);
        phong.setMoTa(request.getMoTa());
    }

    /**
     * Dung cho UPDATE: KHONG bao gio ghi de trang_thai tu request.
     * Trang thai phong chi duoc thay doi boi nghiep vu hop dong
     * (HopDongService.create() -> DANG_THUE, HopDongService.ketThuc() -> TRONG).
     * Giu nguyen gia tri trang_thai hien tai cua phong trong DB.
     */
    private void mapFromRequestForUpdate(PhongRequest request, Phong phong, ToaNha toaNha) {
        phong.setToaNha(toaNha);
        phong.setSoPhong(request.getSoPhong());
        phong.setDienTich(request.getDienTich());
        phong.setGiaThueTheoMacDinh(request.getGiaThueTheoMacDinh());
        // trangThai: KHONG set - giu nguyen gia tri hien tai
        phong.setMoTa(request.getMoTa());
    }

    private PhongResponse toResponse(Phong entity) {
        PhongResponse r = new PhongResponse();
        r.setId(entity.getId());
        r.setToaNhaId(entity.getToaNha().getId());
        r.setTenToaNha(entity.getToaNha().getTenToaNha());
        r.setSoPhong(entity.getSoPhong());
        r.setDienTich(entity.getDienTich());
        r.setGiaThueTheoMacDinh(entity.getGiaThueTheoMacDinh());
        r.setTrangThai(entity.getTrangThai());
        r.setTrangThaiLabel(entity.getTrangThai().getLabel());
        r.setMoTa(entity.getMoTa());
        // Load danh sach thiet bi
        r.setDanhSachThietBi(
            phongThietBiRepository.findByPhong_Id(entity.getId())
                .stream()
                .map(ptb -> ptb.getThietBi())
                .toList()
        );
        return r;
    }

    /**
     * Lay danh sach phong cho trang Quan ly:
     * - Thuc hien phan trang va Natural Sort o cap do Database.
     * - Ho tro xem theo 1 toa nha hoac xem tat ca toa nha (toaNhaId == null).
     * - Batch query hop dong ACTIVE va dai dien theo danh sach phong trang hien tai (khong N+1).
     * - Phat hien bat thuong neu phong co > 1 hop dong ACTIVE.
     * - So luong truy van co dinh (O(1)): toi da 4 cau SQL cho moi trang.
     */
    @Transactional(readOnly = true)
    public Page<PhongQuanLyResponse> findPhongQuanLy(
            Integer toaNhaId,
            String keyword,
            Integer trangThai,
            int page,
            int size) {

        int validatedPage = Math.max(1, page);
        int validatedSize = (size <= 0) ? 10 : size;
        int pageIndex = validatedPage - 1;

        Pageable pageable = PageRequest.of(pageIndex, validatedSize);

        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        TrangThaiPhong enumTrangThai = null;
        if (trangThai != null) {
            try {
                enumTrangThai = TrangThaiPhong.fromValue(trangThai);
            } catch (IllegalArgumentException e) {
                log.warn("Trạng thái phòng không hợp lệ: {}", trangThai);
            }
        }

        // 1 & 2. Truy van phan trang tai Database kem Natural Sort (SELECT + COUNT = 2 cau SQL)
        Page<Phong> pagePhong = phongRepository.findPhongFilterAndPage(
                toaNhaId,
                cleanKeyword,
                enumTrangThai,
                pageable
        );

        if (pagePhong.isEmpty()) {
            return new PageImpl<>(
                    Collections.emptyList(),
                    pageable,
                    pagePhong.getTotalElements()
            );
        }

        List<Integer> phongIds = pagePhong.getContent().stream()
                .map(Phong::getId)
                .toList();

        // 3. Batch query lay hop dong ACTIVE cua cac phong trong trang hien tai (1 cau SQL)
        List<HopDong> dsHopDongActive = hopDongRepository.findActiveByPhongIds(phongIds, TrangThaiHopDong.ACTIVE);

        Map<Integer, List<HopDong>> mapActiveByPhong = dsHopDongActive.stream()
                .filter(hd -> hd.getPhong() != null && hd.getPhong().getId() != null)
                .collect(Collectors.groupingBy(hd -> hd.getPhong().getId()));

        // 4. Batch query lay nguoi dai dien cua cac hop dong ACTIVE tren (1 cau SQL)
        List<Integer> activeHdIds = dsHopDongActive.stream()
                .map(HopDong::getId)
                .toList();

        Map<Integer, String> mapDaiDienByHopDong = new HashMap<>();
        if (!activeHdIds.isEmpty()) {
            List<HopDongKhachThue> dsDaiDien = hdktRepository.findDaiDienByHopDongIds(activeHdIds);
            for (HopDongKhachThue hdkt : dsDaiDien) {
                if (hdkt.getHopDong() != null && hdkt.getHopDong().getId() != null
                        && hdkt.getKhachThue() != null && hdkt.getKhachThue().getHoTen() != null) {
                    mapDaiDienByHopDong.put(hdkt.getHopDong().getId(), hdkt.getKhachThue().getHoTen());
                }
            }
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 5. Map du lieu sang PhongQuanLyResponse bang ID, xu ly safe null
        List<PhongQuanLyResponse> responses = new ArrayList<>();
        for (Phong p : pagePhong.getContent()) {
            PhongQuanLyResponse.PhongQuanLyResponseBuilder builder = PhongQuanLyResponse.builder()
                    .id(p.getId())
                    .toaNhaId(p.getToaNha() != null ? p.getToaNha().getId() : null)
                    .tenToaNha(p.getToaNha() != null ? p.getToaNha().getTenToaNha() : "—")
                    .soPhong(p.getSoPhong())
                    .dienTich(p.getDienTich())
                    .giaThueTheoMacDinh(p.getGiaThueTheoMacDinh())
                    .trangThai(p.getTrangThai())
                    .trangThaiLabel(p.getTrangThai() != null ? p.getTrangThai().getLabel() : "—")
                    .tenNguoiThueDaiDien("—")
                    .ngayHetHanHopDong("—");

            List<HopDong> hdsCuaPhong = mapActiveByPhong.get(p.getId());
            if (hdsCuaPhong != null && !hdsCuaPhong.isEmpty()) {
                if (hdsCuaPhong.size() > 1) {
                    log.warn("BẤT THƯỜNG DỮ LIỆU: Phòng ID {} (Số phòng: {}) có {} hợp đồng ACTIVE cùng lúc!",
                            p.getId(), p.getSoPhong(), hdsCuaPhong.size());
                    builder.canhBaoTrungHopDong(true);
                }
                // Chon hop dong co ID lon nhat
                HopDong hdChinh = hdsCuaPhong.stream()
                        .max(Comparator.comparing(HopDong::getId))
                        .orElse(hdsCuaPhong.get(0));

                builder.hopDongId(hdChinh.getId());
                String tenDaiDien = mapDaiDienByHopDong.get(hdChinh.getId());
                if (tenDaiDien != null && !tenDaiDien.isBlank()) {
                    builder.tenNguoiThueDaiDien(tenDaiDien);
                }
                if (hdChinh.getNgayKetThuc() != null) {
                    builder.ngayHetHanHopDong(hdChinh.getNgayKetThuc().format(dateFormatter));
                }
            }

            responses.add(builder.build());
        }

        return new PageImpl<>(
                responses,
                pageable,
                pagePhong.getTotalElements()
        );
    }
}
