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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

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
    public List<ChiSoDienNuocResponse> findByPhong(Integer phongId) {
        return chiSoRepo.findByPhong_IdOrderByThangNamDesc(phongId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ChiSoDienNuocResponse> findLatestByPhong(Integer phongId) {
        return chiSoRepo.findTopByPhong_IdOrderByThangNamDesc(phongId).map(this::toResponse);
    }

    public ChiSoDienNuocResponse create(ChiSoDienNuocRequest request) {
        // Validate: khong cho nhap thang o tuong lai
        YearMonth currentYm = YearMonth.now();
        YearMonth reqYm = YearMonth.parse(request.getThangNam());
        if (reqYm.isAfter(currentYm)) {
            throw new BusinessException("Không thể nhập chỉ số cho tháng ở tương lai (" + request.getThangNam() + ").");
        }

        // Validate: da co chi so cho phong nay trong thang nay chua?
        if (chiSoRepo.existsByPhong_IdAndThangNam(request.getPhongId(), request.getThangNam())) {
            throw new BusinessException("Phòng này đã có chỉ số tháng " + request.getThangNam() + ".");
        }

        // Kiem tra thang lien truoc
        String thangLienTruoc = reqYm.minusMonths(1).toString();
        var prevChiSoOpt = chiSoRepo.findByPhong_IdAndThangNam(request.getPhongId(), thangLienTruoc);
        if (prevChiSoOpt.isPresent()) {
            ChiSoDienNuoc prevChiSo = prevChiSoOpt.get();
            if (request.getChiSoDienCu() != null && prevChiSo.getChiSoDienMoi() != null
                    && request.getChiSoDienCu().compareTo(prevChiSo.getChiSoDienMoi()) != 0) {
                throw new BusinessException("Chỉ số điện cũ (" + request.getChiSoDienCu()
                        + ") phải bằng chỉ số điện mới của tháng liền trước ("
                        + prevChiSo.getChiSoDienMoi() + ").");
            }
            if (request.getChiSoNuocCu() != null && prevChiSo.getChiSoNuocMoi() != null
                    && request.getChiSoNuocCu().compareTo(prevChiSo.getChiSoNuocMoi()) != 0) {
                throw new BusinessException("Chỉ số nước cũ (" + request.getChiSoNuocCu()
                        + ") phải bằng chỉ số nước mới của tháng liền trước ("
                        + prevChiSo.getChiSoNuocMoi() + ").");
            }
        }

        // Tu dong dien chi so cu neu chua co
        if (request.getChiSoDienCu() == null || request.getChiSoNuocCu() == null) {
            var latest = chiSoRepo.findTopByPhong_IdOrderByThangNamDesc(request.getPhongId()).orElse(null);
            if (latest != null) {
                if (request.getChiSoDienCu() == null) request.setChiSoDienCu(latest.getChiSoDienMoi());
                if (request.getChiSoNuocCu() == null) request.setChiSoNuocCu(latest.getChiSoNuocMoi());
            } else {
                if (request.getChiSoDienCu() == null) request.setChiSoDienCu(java.math.BigDecimal.ZERO);
                if (request.getChiSoNuocCu() == null) request.setChiSoNuocCu(java.math.BigDecimal.ZERO);
            }
        }

        // Validate nghiep vu tai tang Service: chi so moi >= chi so cu
        validateChiSo(request);

        Phong phong = phongRepository.findById(request.getPhongId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", request.getPhongId()));

        ChiSoDienNuoc entity = new ChiSoDienNuoc();
        mapFromRequest(request, entity, phong);
        return toResponse(chiSoRepo.save(entity));
    }

    /**
     * Chu tro chot chi so -> chuyen trang thai sang DA_CHOT.
     * Chi cho phep chot khi dang o trang thai TAM_TINH hoac CAN_XAC_MINH.
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
     * Validate nghiep vu: chi so moi phai >= chi so cu.
     * Validate nay phai nam o tang Service, KHONG chi dua vao Bean Validation DTO.
     */
    private void validateChiSo(ChiSoDienNuocRequest request) {
        if (request.getChiSoDienMoi().compareTo(request.getChiSoDienCu()) < 0) {
            throw new BusinessException("Chỉ số điện mới ("
                    + request.getChiSoDienMoi() + ") không được nhỏ hơn chỉ số cũ ("
                    + request.getChiSoDienCu() + ").");
        }
        if (request.getChiSoNuocMoi().compareTo(request.getChiSoNuocCu()) < 0) {
            throw new BusinessException("Chỉ số nước mới ("
                    + request.getChiSoNuocMoi() + ") không được nhỏ hơn chỉ số cũ ("
                    + request.getChiSoNuocCu() + ").");
        }
    }

    private void mapFromRequest(ChiSoDienNuocRequest request, ChiSoDienNuoc entity, Phong phong) {
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
        ChiSoDienNuocResponse r = new ChiSoDienNuocResponse();
        r.setId(entity.getId());
        r.setPhongId(entity.getPhong().getId());
        r.setSoPhong(entity.getPhong().getSoPhong());
        r.setTenToaNha(entity.getPhong().getToaNha().getTenToaNha());
        r.setThangNam(entity.getThangNam());
        r.setChiSoDienCu(entity.getChiSoDienCu());
        r.setChiSoDienMoi(entity.getChiSoDienMoi());
        r.setChiSoNuocCu(entity.getChiSoNuocCu());
        r.setChiSoNuocMoi(entity.getChiSoNuocMoi());
        r.setAnhChiSo(entity.getAnhChiSo());
        r.setTrangThai(entity.getTrangThai());
        r.setTrangThaiLabel(entity.getTrangThai().getLabel());
        r.setGhiChu(entity.getGhiChu());
        r.setNgayBao(entity.getNgayBao());
        r.setNgayChot(entity.getNgayChot());
        return r;
    }
}
