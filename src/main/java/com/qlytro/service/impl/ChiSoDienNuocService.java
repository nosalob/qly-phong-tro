package com.qlytro.service.impl;

import com.qlytro.dto.request.ChiSoDienNuocRequest;
import com.qlytro.dto.response.ChiSoDienNuocResponse;
import com.qlytro.entity.ChiSoDienNuoc;
import com.qlytro.entity.Phong;
import com.qlytro.entity.enums.TrangThaiChiSo;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.ChiSoDienNuocRepository;
import com.qlytro.repository.PhongRepository;
import com.qlytro.service.base.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChiSoDienNuocService implements BaseService<ChiSoDienNuocResponse, Integer> {

    private final ChiSoDienNuocRepository chiSoRepo;
    private final PhongRepository phongRepository;

    public ChiSoDienNuocService(ChiSoDienNuocRepository chiSoRepo,
                                PhongRepository phongRepository) {
        this.chiSoRepo = chiSoRepo;
        this.phongRepository = phongRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ChiSoDienNuocResponse findById(Integer id) {
        return toResponse(chiSoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chỉ số điện nước", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChiSoDienNuocResponse> findAll() {
        return chiSoRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ChiSoDienNuocResponse> findByFilters(
            Integer phongId, String thangNam, Pageable pageable) {

        return chiSoRepo.findByFilters(
                phongId,
                thangNam,
                TrangThaiChiSo.DA_CHOT,
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ChiSoDienNuocResponse> findByPhong(Integer phongId) {
        return chiSoRepo.findByPhong_IdOrderByThangNamDesc(phongId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ChiSoDienNuocResponse> findLatestByPhong(Integer phongId) {
        return chiSoRepo.findTopByPhong_IdOrderByThangNamDesc(phongId)
                .map(this::toResponse);
    }

    public ChiSoDienNuocResponse create(ChiSoDienNuocRequest request) {
        // Không cho nhập tháng ở tương lai
        YearMonth currentYm = YearMonth.now();
        YearMonth reqYm = YearMonth.parse(request.getThangNam());

        if (reqYm.isAfter(currentYm)) {
            throw new BusinessException(
                    "Không thể nhập chỉ số cho tháng ở tương lai (" + request.getThangNam() + ").");
        }

        // Kiểm tra đã có chỉ số cho phòng trong tháng này chưa
        if (chiSoRepo.existsByPhong_IdAndThangNam(
                request.getPhongId(), request.getThangNam())) {
            throw new BusinessException(
                    "Phòng này đã có chỉ số tháng " + request.getThangNam() + ".");
        }

        // Kiểm tra tháng liền trước
        validateChiSoThangTruoc(request, reqYm);

        // Tự động điền chỉ số cũ nếu chưa có
        if (request.getChiSoDienCu() == null || request.getChiSoNuocCu() == null) {
            var latest = chiSoRepo.findTopByPhong_IdOrderByThangNamDesc(
                    request.getPhongId()).orElse(null);

            if (latest != null) {
                if (request.getChiSoDienCu() == null) {
                    request.setChiSoDienCu(latest.getChiSoDienMoi());
                }
                if (request.getChiSoNuocCu() == null) {
                    request.setChiSoNuocCu(latest.getChiSoNuocMoi());
                }
            } else {
                if (request.getChiSoDienCu() == null) {
                    request.setChiSoDienCu(BigDecimal.ZERO);
                }
                if (request.getChiSoNuocCu() == null) {
                    request.setChiSoNuocCu(BigDecimal.ZERO);
                }
            }
        }

        validateChiSo(request);

        Phong phong = phongRepository.findById(request.getPhongId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", request.getPhongId()));

        ChiSoDienNuoc entity = new ChiSoDienNuoc();
        mapFromRequest(request, entity, phong);

        return toResponse(chiSoRepo.save(entity));
    }

    /**
     * Cập nhật bản ghi chỉ số.
     * Không cho sửa bản ghi đã chốt.
     */
    public ChiSoDienNuocResponse update(Integer id, ChiSoDienNuocRequest request) {
        ChiSoDienNuoc entity = chiSoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chỉ số điện nước", id));

        if (TrangThaiChiSo.DA_CHOT.equals(entity.getTrangThai())) {
            throw new BusinessException("Không thể sửa chỉ số đã chốt.");
        }

        YearMonth currentYm = YearMonth.now();
        YearMonth reqYm = YearMonth.parse(request.getThangNam());

        if (reqYm.isAfter(currentYm)) {
            throw new BusinessException(
                    "Không thể nhập chỉ số cho tháng ở tương lai (" + request.getThangNam() + ").");
        }

        // Kiểm tra trùng phòng/tháng với bản ghi khác
        Optional<ChiSoDienNuoc> existing = chiSoRepo.findByPhong_IdAndThangNam(
                request.getPhongId(), request.getThangNam());

        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new BusinessException(
                    "Phòng này đã có chỉ số tháng " + request.getThangNam() + ".");
        }

        // Giữ nguyên quy tắc đối chiếu chỉ số với tháng liền trước
        validateChiSoThangTruoc(request, reqYm);

        validateChiSo(request);

        Phong phong = phongRepository.findById(request.getPhongId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", request.getPhongId()));

        // Chỉ cập nhật dữ liệu nhập, không thay đổi trạng thái và ngày nhập.
        entity.setPhong(phong);
        entity.setThangNam(request.getThangNam());
        entity.setChiSoDienCu(request.getChiSoDienCu());
        entity.setChiSoDienMoi(request.getChiSoDienMoi());
        entity.setChiSoNuocCu(request.getChiSoNuocCu());
        entity.setChiSoNuocMoi(request.getChiSoNuocMoi());
        entity.setAnhChiSo(request.getAnhChiSo());
        entity.setGhiChu(request.getGhiChu());

        return toResponse(chiSoRepo.save(entity));
    }

    /**
     * Chủ trọ chốt chỉ số -> chuyển trạng thái sang DA_CHOT.
     */
    public ChiSoDienNuocResponse chotChiSo(Integer id) {
        ChiSoDienNuoc entity = chiSoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chỉ số điện nước", id));

        if (TrangThaiChiSo.DA_CHOT.equals(entity.getTrangThai())) {
            throw new BusinessException("Chỉ số này đã được chốt rồi.");
        }

        entity.setTrangThai(TrangThaiChiSo.DA_CHOT);
        entity.setNgayChot(LocalDateTime.now());

        return toResponse(chiSoRepo.save(entity));
    }

    public ChiSoDienNuocResponse canXacMinh(Integer id, String ghiChu) {
        ChiSoDienNuoc entity = chiSoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chỉ số điện nước", id));

        entity.setTrangThai(TrangThaiChiSo.CAN_XAC_MINH);
        entity.setGhiChu(ghiChu);

        return toResponse(chiSoRepo.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        ChiSoDienNuoc entity = chiSoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chỉ số điện nước", id));

        if (TrangThaiChiSo.DA_CHOT.equals(entity.getTrangThai())) {
            throw new BusinessException("Không thể xóa chỉ số đã chốt.");
        }

        chiSoRepo.delete(entity);
    }

    /**
     * Kiểm tra chỉ số cũ phải khớp chỉ số mới của tháng liền trước nếu có.
     */
    private void validateChiSoThangTruoc(
            ChiSoDienNuocRequest request, YearMonth reqYm) {

        String thangLienTruoc = reqYm.minusMonths(1).toString();

        var prevChiSoOpt = chiSoRepo.findByPhong_IdAndThangNam(
                request.getPhongId(), thangLienTruoc);

        if (prevChiSoOpt.isPresent()) {
            ChiSoDienNuoc prevChiSo = prevChiSoOpt.get();

            if (request.getChiSoDienCu() != null
                    && prevChiSo.getChiSoDienMoi() != null
                    && request.getChiSoDienCu().compareTo(
                    prevChiSo.getChiSoDienMoi()) != 0) {
                throw new BusinessException(
                        "Chỉ số điện cũ (" + request.getChiSoDienCu()
                                + ") phải bằng chỉ số điện mới của tháng liền trước ("
                                + prevChiSo.getChiSoDienMoi() + ").");
            }

            if (request.getChiSoNuocCu() != null
                    && prevChiSo.getChiSoNuocMoi() != null
                    && request.getChiSoNuocCu().compareTo(
                    prevChiSo.getChiSoNuocMoi()) != 0) {
                throw new BusinessException(
                        "Chỉ số nước cũ (" + request.getChiSoNuocCu()
                                + ") phải bằng chỉ số nước mới của tháng liền trước ("
                                + prevChiSo.getChiSoNuocMoi() + ").");
            }
        }
    }

    /**
     * Chỉ số mới phải lớn hơn hoặc bằng chỉ số cũ.
     */
    private void validateChiSo(ChiSoDienNuocRequest request) {
        if (request.getChiSoDienMoi().compareTo(request.getChiSoDienCu()) < 0) {
            throw new BusinessException(
                    "Chỉ số điện mới (" + request.getChiSoDienMoi()
                            + ") không được nhỏ hơn chỉ số cũ ("
                            + request.getChiSoDienCu() + ").");
        }

        if (request.getChiSoNuocMoi().compareTo(request.getChiSoNuocCu()) < 0) {
            throw new BusinessException(
                    "Chỉ số nước mới (" + request.getChiSoNuocMoi()
                            + ") không được nhỏ hơn chỉ số cũ ("
                            + request.getChiSoNuocCu() + ").");
        }
    }

    private void mapFromRequest(
            ChiSoDienNuocRequest request, ChiSoDienNuoc entity, Phong phong) {

        entity.setPhong(phong);
        entity.setThangNam(request.getThangNam());
        entity.setChiSoDienCu(request.getChiSoDienCu());
        entity.setChiSoDienMoi(request.getChiSoDienMoi());
        entity.setChiSoNuocCu(request.getChiSoNuocCu());
        entity.setChiSoNuocMoi(request.getChiSoNuocMoi());
        entity.setAnhChiSo(request.getAnhChiSo());
        entity.setGhiChu(request.getGhiChu());
        entity.setTrangThai(TrangThaiChiSo.TAM_TINH);
    }

    public ChiSoDienNuocResponse toResponse(ChiSoDienNuoc entity) {
        ChiSoDienNuocResponse response = new ChiSoDienNuocResponse();

        response.setId(entity.getId());
        response.setPhongId(entity.getPhong().getId());
        response.setSoPhong(entity.getPhong().getSoPhong());
        response.setTenToaNha(entity.getPhong().getToaNha().getTenToaNha());
        response.setThangNam(entity.getThangNam());
        response.setChiSoDienCu(entity.getChiSoDienCu());
        response.setChiSoDienMoi(entity.getChiSoDienMoi());
        response.setChiSoNuocCu(entity.getChiSoNuocCu());
        response.setChiSoNuocMoi(entity.getChiSoNuocMoi());
        response.setAnhChiSo(entity.getAnhChiSo());
        response.setTrangThai(entity.getTrangThai());
        response.setTrangThaiLabel(entity.getTrangThai().getLabel());
        response.setGhiChu(entity.getGhiChu());
        response.setNgayBao(entity.getNgayBao());
        response.setNgayChot(entity.getNgayChot());

        return response;
    }
}