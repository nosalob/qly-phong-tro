package com.qlytro.service.impl;

import com.qlytro.dto.request.PhongRequest;
import com.qlytro.dto.response.PhongResponse;
import com.qlytro.entity.Phong;
import com.qlytro.entity.ToaNha;
import com.qlytro.entity.enums.TrangThaiPhong;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.HopDongRepository;
import com.qlytro.repository.PhongRepository;
import com.qlytro.repository.PhongThietBiRepository;
import com.qlytro.repository.ToaNhaRepository;
import com.qlytro.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class PhongService implements BaseService<PhongResponse, Integer> {

    private final PhongRepository phongRepository;
    private final ToaNhaRepository toaNhaRepository;
    private final HopDongRepository hopDongRepository;
    private final PhongThietBiRepository phongThietBiRepository;
    private final com.qlytro.repository.ChiSoDienNuocRepository chiSoRepository;
    private final com.qlytro.repository.SuCoRepository suCoRepository;

    public PhongService(PhongRepository phongRepository,
                        ToaNhaRepository toaNhaRepository,
                        HopDongRepository hopDongRepository,
                        PhongThietBiRepository phongThietBiRepository,
                        com.qlytro.repository.ChiSoDienNuocRepository chiSoRepository,
                        com.qlytro.repository.SuCoRepository suCoRepository) {
        this.phongRepository = phongRepository;
        this.toaNhaRepository = toaNhaRepository;
        this.hopDongRepository = hopDongRepository;
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
}
