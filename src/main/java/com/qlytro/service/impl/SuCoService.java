package com.qlytro.service.impl;

import com.qlytro.dto.request.SuCoRequest;
import com.qlytro.dto.response.SuCoResponse;
import com.qlytro.entity.*;
import com.qlytro.entity.enums.NguyenNhanSuCo;
import com.qlytro.entity.enums.TrangThaiSuCo;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.*;
import com.qlytro.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class SuCoService implements BaseService<SuCoResponse, Integer> {

    private final SuCoRepository suCoRepository;
    private final PhongRepository phongRepository;
    private final ThietBiRepository thietBiRepository;
    private final PhongThietBiRepository phongThietBiRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final KhachThueRepository khachThueRepository;
    private final HopDongRepository hopDongRepository;

    public SuCoService(SuCoRepository suCoRepository,
                       PhongRepository phongRepository,
                       ThietBiRepository thietBiRepository,
                       PhongThietBiRepository phongThietBiRepository,
                       NguoiDungRepository nguoiDungRepository,
                       KhachThueRepository khachThueRepository,
                       HopDongRepository hopDongRepository) {
        this.suCoRepository = suCoRepository;
        this.phongRepository = phongRepository;
        this.thietBiRepository = thietBiRepository;
        this.phongThietBiRepository = phongThietBiRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.khachThueRepository = khachThueRepository;
        this.hopDongRepository = hopDongRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SuCoResponse findById(Integer id) {
        return toResponse(suCoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sự cố", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuCoResponse> findAll() {
        return suCoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SuCoResponse> findByTrangThai(TrangThaiSuCo trangThai) {
        return suCoRepository.findByTrangThai(trangThai).stream().map(this::toResponse).toList();
    }

    /** Tim su co cua 1 nguoi bao cu the (dung cho tenant view). */
    @Transactional(readOnly = true)
    public List<SuCoResponse> findByNguoiBao(Integer nguoiDungId) {
        return suCoRepository.findByNguoiBao_Id(nguoiDungId)
                .stream().map(this::toResponse).toList();
    }


    /**
     * Khách thuê báo sự cố:
     * - Service tự xác định phong_id từ hop dong ACTIVE của tài khoản đang đăng nhập.
     * - Khách không cần tự chọn phòng.
     */
    public SuCoResponse taoSuCoByTenant(SuCoRequest request, Integer nguoiDungId) {
        // Tim khach_thue tuong ung voi nguoi_dung_id
        KhachThue khachThue = khachThueRepository.findByNguoiDung_Id(nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        "Tài khoản của bạn chưa được liên kết với hồ sơ khách thuê."));

        // Tim hop dong ACTIVE cua khach nay de lay phong_id
        HopDong hopDong = hopDongRepository.findActiveByKhachThueId(khachThue.getId())
                .orElseThrow(() -> new BusinessException(
                        "Bạn không có hợp đồng đang hiệu lực. Vui lòng liên hệ chủ trọ."));

        return taoSuCo(request, hopDong.getPhong().getId(), nguoiDungId);
    }

    /** Chủ trọ tạo sự cố thủ công (phong_id do chủ trọ chọn). */
    public SuCoResponse taoSuCoByLandlord(SuCoRequest request, Integer nguoiDungId) {
        if (request.getPhongId() == null) {
            throw new BusinessException("Vui lòng chọn phòng cho sự cố.");
        }
        return taoSuCo(request, request.getPhongId(), nguoiDungId);
    }

    private SuCoResponse taoSuCo(SuCoRequest request, Integer phongId, Integer nguoiDungId) {
        Phong phong = phongRepository.findById(phongId)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng", phongId));
        NguoiDung nguoiBao = nguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", nguoiDungId));

        SuCo suCo = new SuCo();
        suCo.setPhong(phong);
        suCo.setNguoiBao(nguoiBao);
        suCo.setMoTa(request.getMoTa());
        suCo.setTrangThai(TrangThaiSuCo.MOI);

        if (request.getThietBiId() != null) {
            ThietBi tb = thietBiRepository.findById(request.getThietBiId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thiết bị", request.getThietBiId()));
            if (!phongThietBiRepository.existsByPhong_IdAndThietBi_Id(phongId, tb.getId())) {
                throw new BusinessException("Thiết bị '" + tb.getTenThietBi()
                        + "' không thuộc phòng " + phong.getSoPhong() + ".");
            }
            suCo.setThietBi(tb);
        }
        return toResponse(suCoRepository.save(suCo));
    }

    public SuCoResponse capNhatTrangThai(Integer id, Integer trangThaiValue) {
        SuCo suCo = suCoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sự cố", id));
        if (TrangThaiSuCo.HOAN_THANH.equals(suCo.getTrangThai())) {
            throw new BusinessException("Sự cố đã hoàn thành, không thể thay đổi trạng thái.");
        }
        TrangThaiSuCo target = TrangThaiSuCo.fromValue(trangThaiValue);
        if (TrangThaiSuCo.HOAN_THANH.equals(target)) {
            throw new BusinessException("Chuyển sang trạng thái Hoàn thành phải thông qua chức năng Hoàn thành và có nguyên nhân.");
        }
        if (TrangThaiSuCo.DANG_XU_LY.equals(target)) {
            if (!TrangThaiSuCo.MOI.equals(suCo.getTrangThai())) {
                throw new BusinessException("Chỉ sự cố đang ở trạng thái 'Mới' mới có thể chuyển sang 'Đang xử lý'.");
            }
        } else {
            throw new BusinessException("Không hỗ trợ chuyển về trạng thái này.");
        }
        suCo.setTrangThai(target);
        return toResponse(suCoRepository.save(suCo));
    }

    /**
     * Hoàn thành sự cố: cần điền nguyên nhân.
     * Nguyên nhân chỉ được điền SAU khi trang_thai = HOAN_THANH.
     * Validate trong Service theo quy ước nghiệp vụ.
     */
    public SuCoResponse hoanThanh(Integer id, Integer nguyenNhanValue) {
        SuCo suCo = suCoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sự cố", id));
        if (TrangThaiSuCo.HOAN_THANH.equals(suCo.getTrangThai())) {
            throw new BusinessException("Sự cố này đã hoàn thành trước đó, không thể mở lại hoặc đổi nguyên nhân.");
        }
        if (nguyenNhanValue == null) {
            throw new BusinessException("Vui lòng chọn nguyên nhân khi hoàn thành sự cố.");
        }
        suCo.setTrangThai(TrangThaiSuCo.HOAN_THANH);
        suCo.setNguyenNhan(NguyenNhanSuCo.fromValue(nguyenNhanValue));
        return toResponse(suCoRepository.save(suCo));
    }

    @Override
    public void deleteById(Integer id) {
        SuCo suCo = suCoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sự cố", id));
        if (!TrangThaiSuCo.MOI.equals(suCo.getTrangThai())) {
            throw new BusinessException("Chỉ có thể xóa sự cố ở trạng thái Mới.");
        }
        suCoRepository.delete(suCo);
    }

    /** Lay danh sach thiet bi dang gan vao 1 phong cu the. */
    @Transactional(readOnly = true)
    public List<ThietBi> thietBiCuaPhong(Integer phongId) {
        return phongThietBiRepository.findByPhong_Id(phongId)
                .stream().map(PhongThietBi::getThietBi).toList();
    }

    /**
     * Lay danh sach thiet bi cua phong ma tenant dang o
     * (xac dinh qua hop dong ACTIVE).
     */
    @Transactional(readOnly = true)
    public List<ThietBi> thietBiCuaPhongHienTai(Integer nguoiDungId) {
        KhachThue kt = khachThueRepository.findByNguoiDung_Id(nguoiDungId)
                .orElseThrow(() -> new BusinessException(
                        "Tài khoản của bạn chưa được liên kết với hồ sơ khách thuê."));
        HopDong hd = hopDongRepository.findActiveByKhachThueId(kt.getId())
                .orElseThrow(() -> new BusinessException(
                        "Bạn không có hợp đồng đang hiệu lực. Vui lòng liên hệ chủ trọ."));
        return thietBiCuaPhong(hd.getPhong().getId());
    }

    public SuCoResponse toResponse(SuCo entity) {
        SuCoResponse r = new SuCoResponse();
        r.setId(entity.getId());
        r.setPhongId(entity.getPhong().getId());
        r.setSoPhong(entity.getPhong().getSoPhong());
        r.setTenToaNha(entity.getPhong().getToaNha().getTenToaNha());
        r.setThietBiId(entity.getThietBi() != null ? entity.getThietBi().getId() : null);
        r.setTenThietBi(entity.getThietBi() != null ? entity.getThietBi().getTenThietBi() : null);
        r.setNguoiBaoId(entity.getNguoiBao().getId());
        r.setTenNguoiBao(entity.getNguoiBao().getHoTen());
        r.setMoTa(entity.getMoTa());
        r.setTrangThai(entity.getTrangThai());
        r.setTrangThaiLabel(entity.getTrangThai().getLabel());
        r.setNguyenNhan(entity.getNguyenNhan());
        r.setNguyenNhanLabel(entity.getNguyenNhan() != null ? entity.getNguyenNhan().getLabel() : null);
        r.setNgayBao(entity.getNgayBao());
        return r;
    }
}
