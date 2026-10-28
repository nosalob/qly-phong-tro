package com.qlytro.service.impl;

import com.qlytro.dto.request.NguoiDungRequest;
import com.qlytro.dto.response.NguoiDungResponse;
import com.qlytro.entity.NguoiDung;
import com.qlytro.entity.enums.TrangThaiNguoiDung;
import com.qlytro.entity.enums.VaiTroNguoiDung;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.NguoiDungRepository;
import com.qlytro.service.base.BaseService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class NguoiDungService implements BaseService<NguoiDungResponse, Integer> {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public NguoiDungService(NguoiDungRepository nguoiDungRepository,
                             PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public NguoiDungResponse findById(Integer id) {
        return toResponse(nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NguoiDungResponse> findAll() {
        return nguoiDungRepository.findAll().stream().map(this::toResponse).toList();
    }

    public NguoiDungResponse create(NguoiDungRequest request) {
        if (nguoiDungRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' đã tồn tại.");
        }
        NguoiDung nd = new NguoiDung();
        mapFromRequest(request, nd, true);
        return toResponse(nguoiDungRepository.save(nd));
    }

    public NguoiDungResponse update(Integer id, NguoiDungRequest request) {
        NguoiDung nd = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
        mapFromRequest(request, nd, !request.getPassword().isBlank());
        return toResponse(nguoiDungRepository.save(nd));
    }

    public void khoaTaiKhoan(Integer id) {
        NguoiDung nd = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
        nd.setTrangThai(TrangThaiNguoiDung.KHOA);
        nguoiDungRepository.save(nd);
    }

    public void moKhoa(Integer id) {
        NguoiDung nd = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
        nd.setTrangThai(TrangThaiNguoiDung.ACTIVE);
        nguoiDungRepository.save(nd);
    }

    @Override
    public void deleteById(Integer id) {
        nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
        nguoiDungRepository.deleteById(id);
    }

    public void resetMatKhau(Integer id) {
        NguoiDung nd = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
        nd.setPassword(passwordEncoder.encode("123456"));
        nd.setMustChangePassword(true);
        nguoiDungRepository.save(nd);
    }

    public void doiMatKhau(Integer nguoiDungId, com.qlytro.dto.request.DoiMatKhauRequest request) {
        NguoiDung nd = nguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", nguoiDungId));

        if (!passwordEncoder.matches(request.getMatKhauCu(), nd.getPassword())) {
            throw new BusinessException("Mật khẩu cũ không chính xác.");
        }

        if (request.getMatKhauMoi() == null || request.getMatKhauMoi().length() < 8) {
            throw new BusinessException("Mật khẩu mới phải có tối thiểu 8 ký tự.");
        }

        if (!request.getMatKhauMoi().equals(request.getXacNhanMatKhau())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp với mật khẩu mới.");
        }

        if (passwordEncoder.matches(request.getMatKhauMoi(), nd.getPassword())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu cũ.");
        }

        nd.setPassword(passwordEncoder.encode(request.getMatKhauMoi()));
        nd.setMustChangePassword(false);
        nguoiDungRepository.save(nd);
    }

    private void mapFromRequest(NguoiDungRequest request, NguoiDung nd, boolean encodePassword) {
        nd.setUsername(request.getUsername());
        if (encodePassword && request.getPassword() != null && !request.getPassword().isBlank()) {
            // Service dam bao mat khau luon duoc ma hoa BCrypt, KHONG luu plain text
            nd.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        nd.setHoTen(request.getHoTen());
        nd.setSdt(request.getSdt());
        nd.setEmail(request.getEmail());
        nd.setVaiTro(VaiTroNguoiDung.fromValue(request.getVaiTro()));
        if (nd.getTrangThai() == null) {
            nd.setTrangThai(TrangThaiNguoiDung.ACTIVE);
        }
        if (nd.getMustChangePassword() == null) {
            nd.setMustChangePassword(false);
        }
    }

    private NguoiDungResponse toResponse(NguoiDung entity) {
        NguoiDungResponse r = new NguoiDungResponse();
        r.setId(entity.getId());
        r.setUsername(entity.getUsername());
        r.setHoTen(entity.getHoTen());
        r.setSdt(entity.getSdt());
        r.setEmail(entity.getEmail());
        r.setVaiTro(entity.getVaiTro());
        r.setVaiTroLabel(entity.getVaiTro().getLabel());
        r.setTrangThai(entity.getTrangThai());
        r.setTrangThaiLabel(entity.getTrangThai().getLabel());
        r.setMustChangePassword(entity.getMustChangePassword());
        r.setNgayTao(entity.getNgayTao());
        return r;
    }
}
