package com.qlytro.service.impl;

import com.qlytro.dto.request.KhachThueRequest;
import com.qlytro.dto.response.KhachThueResponse;
import com.qlytro.entity.KhachThue;
import com.qlytro.entity.NguoiDung;
import com.qlytro.entity.enums.VaiTroNguoiDung;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.KhachThueRepository;
import com.qlytro.repository.NguoiDungRepository;
import com.qlytro.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class KhachThueService implements BaseService<KhachThueResponse, Integer> {

    private final KhachThueRepository khachThueRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final com.qlytro.repository.HopDongKhachThueRepository hdktRepository;

    public KhachThueService(KhachThueRepository khachThueRepository,
                             NguoiDungRepository nguoiDungRepository,
                             com.qlytro.repository.HopDongKhachThueRepository hdktRepository) {
        this.khachThueRepository = khachThueRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.hdktRepository = hdktRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public KhachThueResponse findById(Integer id) {
        return toResponse(khachThueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khách thuê", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KhachThueResponse> findAll() {
        return khachThueRepository.findAll().stream().map(this::toResponse).toList();
    }

    public KhachThueResponse create(KhachThueRequest request) {
        if (khachThueRepository.existsByCccd(request.getCccd())) {
            throw new BusinessException("CCCD '" + request.getCccd() + "' đã được đăng ký.");
        }
        KhachThue kt = new KhachThue();
        mapFromRequest(request, kt);
        return toResponse(khachThueRepository.save(kt));
    }

    public KhachThueResponse update(Integer id, KhachThueRequest request) {
        KhachThue kt = khachThueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khách thuê", id));
        if (khachThueRepository.existsByCccdAndIdNot(request.getCccd(), id)) {
            throw new BusinessException("CCCD '" + request.getCccd() + "' đã được đăng ký bởi người khác.");
        }
        mapFromRequest(request, kt);
        return toResponse(khachThueRepository.save(kt));
    }

    @Override
    public void deleteById(Integer id) {
        khachThueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khách thuê", id));
        if (hdktRepository.existsByKhachThue_Id(id)) {
            throw new BusinessException("Khách thuê đã từng thuộc hợp đồng, không thể xóa.");
        }
        khachThueRepository.deleteById(id);
    }

    private void mapFromRequest(KhachThueRequest request, KhachThue kt) {
        kt.setHoTen(request.getHoTen());
        kt.setCccd(request.getCccd());
        kt.setNgaySinh(request.getNgaySinh());
        kt.setSdt(request.getSdt());
        if (request.getNguoiDungId() != null) {
            NguoiDung nd = nguoiDungRepository.findById(request.getNguoiDungId())
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng", request.getNguoiDungId()));
            if (!VaiTroNguoiDung.KHACH_THUE.equals(nd.getVaiTro())) {
                throw new BusinessException("Tài khoản '" + nd.getUsername() + "' không có vai trò Người thuê.");
            }
            var existingKt = khachThueRepository.findByNguoiDung_Id(nd.getId());
            if (existingKt.isPresent() && !existingKt.get().getId().equals(kt.getId())) {
                throw new BusinessException("Tài khoản '" + nd.getUsername()
                        + "' đã được liên kết với hồ sơ khách thuê khác (" + existingKt.get().getHoTen() + ").");
            }
            kt.setNguoiDung(nd);
        } else {
            kt.setNguoiDung(null);
        }
    }

    public KhachThueResponse toResponse(KhachThue entity) {
        KhachThueResponse r = new KhachThueResponse();
        r.setId(entity.getId());
        r.setHoTen(entity.getHoTen());
        r.setCccd(entity.getCccd());
        r.setNgaySinh(entity.getNgaySinh());
        r.setSdt(entity.getSdt());
        if (entity.getNguoiDung() != null) {
            r.setNguoiDungId(entity.getNguoiDung().getId());
            r.setUsername(entity.getNguoiDung().getUsername());
        }
        return r;
    }
}
