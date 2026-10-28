package com.qlytro.service.impl;

import com.qlytro.dto.request.BoiThuongThietHaiRequest;
import com.qlytro.dto.response.BoiThuongThietHaiResponse;
import com.qlytro.entity.BoiThuongThietHai;
import com.qlytro.entity.HopDong;
import com.qlytro.entity.KhachThue;
import com.qlytro.entity.SuCo;
import com.qlytro.entity.enums.NguonPhatHienBoiThuong;
import com.qlytro.entity.enums.TrangThaiBoiThuong;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.BoiThuongThietHaiRepository;
import com.qlytro.repository.HopDongKhachThueRepository;
import com.qlytro.repository.HopDongRepository;
import com.qlytro.repository.KhachThueRepository;
import com.qlytro.repository.SuCoRepository;
import com.qlytro.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class BoiThuongThietHaiService implements BaseService<BoiThuongThietHaiResponse, Integer> {

    private final BoiThuongThietHaiRepository boiThuongRepository;
    private final HopDongRepository hopDongRepository;
    private final SuCoRepository suCoRepository;
    private final HopDongKhachThueRepository hdktRepository;
    private final KhachThueRepository khachThueRepository;

    public BoiThuongThietHaiService(BoiThuongThietHaiRepository boiThuongRepository,
                                   HopDongRepository hopDongRepository,
                                   SuCoRepository suCoRepository,
                                   HopDongKhachThueRepository hdktRepository,
                                   KhachThueRepository khachThueRepository) {
        this.boiThuongRepository = boiThuongRepository;
        this.hopDongRepository = hopDongRepository;
        this.suCoRepository = suCoRepository;
        this.hdktRepository = hdktRepository;
        this.khachThueRepository = khachThueRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BoiThuongThietHaiResponse findById(Integer id) {
        return toResponse(boiThuongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bồi thường thiệt hại", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoiThuongThietHaiResponse> findAll() {
        return boiThuongRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BoiThuongThietHaiResponse> findByHopDongId(Integer hopDongId) {
        return boiThuongRepository.findByHopDong_Id(hopDongId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BoiThuongThietHaiResponse> findByNguoiDungId(Integer nguoiDungId) {
        KhachThue khachThue = khachThueRepository.findByNguoiDung_Id(nguoiDungId).orElse(null);
        if (khachThue == null) return List.of();

        List<Integer> hopDongIds = hdktRepository.findByKhachThue_Id(khachThue.getId())
                .stream()
                .map(hdkt -> hdkt.getHopDong().getId())
                .toList();

        if (hopDongIds.isEmpty()) return List.of();

        return hopDongIds.stream()
                .flatMap(hdId -> boiThuongRepository.findByHopDong_Id(hdId).stream())
                .map(this::toResponse)
                .toList();
    }

    public BoiThuongThietHaiResponse create(BoiThuongThietHaiRequest request) {
        HopDong hopDong = hopDongRepository.findById(request.getHopDongId())
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng", request.getHopDongId()));

        if (!com.qlytro.entity.enums.TrangThaiHopDong.ACTIVE.equals(hopDong.getTrangThai())) {
            throw new BusinessException("Hợp đồng này đã kết thúc, không thể lập biên bản bồi thường.");
        }

        SuCo suCo = null;
        if (request.getSuCoId() != null) {
            suCo = suCoRepository.findById(request.getSuCoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sự cố", request.getSuCoId()));
            if (!suCo.getPhong().getId().equals(hopDong.getPhong().getId())) {
                throw new BusinessException("Sự cố được chọn không thuộc cùng phòng với hợp đồng.");
            }
            if (!com.qlytro.entity.enums.TrangThaiSuCo.HOAN_THANH.equals(suCo.getTrangThai())) {
                throw new BusinessException("Chỉ sự cố đã hoàn thành mới có thể tạo bồi thường.");
            }
            if (!com.qlytro.entity.enums.NguyenNhanSuCo.DO_NGUOI_O_LAM_HONG.equals(suCo.getNguyenNhan())) {
                throw new BusinessException("Chỉ sự cố có nguyên nhân 'Do người ở làm hỏng' mới tạo bồi thường thiệt hại.");
            }
        }

        NguonPhatHienBoiThuong nguon = NguonPhatHienBoiThuong.fromValue(request.getNguonPhatHien());

        BoiThuongThietHai bt = BoiThuongThietHai.builder()
                .hopDong(hopDong)
                .suCo(suCo)
                .moTa(request.getMoTa())
                .soTien(request.getSoTien())
                .nguonPhatHien(nguon)
                .trangThai(TrangThaiBoiThuong.TRANG_THAI_0)
                .ngayGhiNhan(request.getNgayGhiNhan() != null ? request.getNgayGhiNhan() : LocalDate.now())
                .build();

        return toResponse(boiThuongRepository.save(bt));
    }

    @Transactional(readOnly = true)
    public BoiThuongThietHaiResponse findByIdOfNguoiDung(Integer id, Integer nguoiDungId) {
        return toResponse(getBoiThuongOfNguoiDung(id, nguoiDungId));
    }

    private BoiThuongThietHai getBoiThuongOfNguoiDung(Integer id, Integer nguoiDungId) {
        BoiThuongThietHai bt = boiThuongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bồi thường thiệt hại", id));
        if (nguoiDungId != null) {
            KhachThue khachThue = khachThueRepository.findByNguoiDung_Id(nguoiDungId).orElse(null);
            if (khachThue == null || !hdktRepository.existsByHopDong_IdAndKhachThue_Id(bt.getHopDong().getId(), khachThue.getId())) {
                throw new ResourceNotFoundException("Bồi thường thiệt hại", id);
            }
        }
        return bt;
    }

    public void validateKhachNopTien(Integer id, Integer nguoiDungId) {
        BoiThuongThietHai bt = getBoiThuongOfNguoiDung(id, nguoiDungId);
        if (!NguonPhatHienBoiThuong.TRONG_LUC_O.equals(bt.getNguonPhatHien())) {
            throw new BusinessException("Khoản bồi thường này sẽ được khấu trừ vào tiền cọc khi trả phòng.");
        }
        if (!TrangThaiBoiThuong.TRANG_THAI_0.equals(bt.getTrangThai())) {
            throw new BusinessException("Khoản bồi thường này không ở trạng thái Chưa thanh toán.");
        }
    }

    public BoiThuongThietHaiResponse khachNopTien(Integer id, String anhChuyenKhoan, Integer nguoiDungId) {
        validateKhachNopTien(id, nguoiDungId);
        BoiThuongThietHai bt = getBoiThuongOfNguoiDung(id, nguoiDungId);
        bt.setAnhChuyenKhoan(anhChuyenKhoan);
        bt.setTrangThai(TrangThaiBoiThuong.TRANG_THAI_1);
        return toResponse(boiThuongRepository.save(bt));
    }

    public BoiThuongThietHaiResponse khachNopTien(Integer id, String anhChuyenKhoan) {
        return khachNopTien(id, anhChuyenKhoan, null);
    }

    public BoiThuongThietHaiResponse xacNhanThanhToan(Integer id) {
        BoiThuongThietHai bt = boiThuongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bồi thường thiệt hại", id));

        if (!NguonPhatHienBoiThuong.TRONG_LUC_O.equals(bt.getNguonPhatHien())) {
            throw new BusinessException("Khoản bồi thường này thuộc loại khấu trừ cọc, không thu trực tiếp.");
        }
        if (!TrangThaiBoiThuong.TRANG_THAI_1.equals(bt.getTrangThai()) && !TrangThaiBoiThuong.TRANG_THAI_0.equals(bt.getTrangThai())) {
            throw new BusinessException("Khoản bồi thường này đã được thanh toán.");
        }

        bt.setTrangThai(TrangThaiBoiThuong.TRANG_THAI_2);
        return toResponse(boiThuongRepository.save(bt));
    }

    @Override
    public void deleteById(Integer id) {
        BoiThuongThietHai bt = boiThuongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bồi thường thiệt hại", id));
        if (!TrangThaiBoiThuong.TRANG_THAI_0.equals(bt.getTrangThai())) {
            throw new BusinessException("Chỉ có thể xóa khoản bồi thường chưa xử lý.");
        }
        boiThuongRepository.delete(bt);
    }

    public BoiThuongThietHaiResponse toResponse(BoiThuongThietHai entity) {
        BoiThuongThietHaiResponse r = new BoiThuongThietHaiResponse();
        r.setId(entity.getId());
        r.setHopDongId(entity.getHopDong().getId());
        r.setSoPhong(entity.getHopDong().getPhong().getSoPhong());
        r.setTenToaNha(entity.getHopDong().getPhong().getToaNha().getTenToaNha());
        r.setSuCoId(entity.getSuCo() != null ? entity.getSuCo().getId() : null);
        r.setMoTaSuCo(entity.getSuCo() != null ? entity.getSuCo().getMoTa() : null);
        r.setMoTa(entity.getMoTa());
        r.setSoTien(entity.getSoTien());
        r.setNguonPhatHien(entity.getNguonPhatHien());
        r.setNguonPhatHienLabel(entity.getNguonPhatHien().getLabel());
        r.setTrangThai(entity.getTrangThai());
        r.setTrangThaiLabel(entity.getTrangThai().getLabelByNguon(entity.getNguonPhatHien()));
        r.setAnhChuyenKhoan(entity.getAnhChuyenKhoan());
        r.setNgayGhiNhan(entity.getNgayGhiNhan());

        hdktRepository.findByHopDong_Id(entity.getHopDong().getId()).stream()
                .filter(hdkt -> Boolean.TRUE.equals(hdkt.getLaDaiDien()) && Boolean.TRUE.equals(hdkt.getDangO()))
                .findFirst()
                .ifPresent(hdkt -> r.setTenKhachDaiDien(hdkt.getKhachThue().getHoTen()));

        return r;
    }
}
