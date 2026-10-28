package com.qlytro.service.impl;

import com.qlytro.entity.ThietBi;
import com.qlytro.exception.BusinessException;
import com.qlytro.exception.ResourceNotFoundException;
import com.qlytro.repository.PhongThietBiRepository;
import com.qlytro.repository.ThietBiRepository;
import com.qlytro.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ThietBiService implements BaseService<ThietBi, Integer> {

    private final ThietBiRepository thietBiRepository;
    private final PhongThietBiRepository phongThietBiRepository;
    private final com.qlytro.repository.SuCoRepository suCoRepository;

    public ThietBiService(ThietBiRepository thietBiRepository,
                          PhongThietBiRepository phongThietBiRepository,
                          com.qlytro.repository.SuCoRepository suCoRepository) {
        this.thietBiRepository = thietBiRepository;
        this.phongThietBiRepository = phongThietBiRepository;
        this.suCoRepository = suCoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ThietBi findById(Integer id) {
        return thietBiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thiết bị", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThietBi> findAll() {
        return thietBiRepository.findAll();
    }

    public ThietBi create(String tenThietBi) {
        if (thietBiRepository.existsByTenThietBi(tenThietBi)) {
            throw new BusinessException("Thiết bị '" + tenThietBi + "' đã tồn tại.");
        }
        ThietBi tb = new ThietBi();
        tb.setTenThietBi(tenThietBi);
        return thietBiRepository.save(tb);
    }

    public ThietBi update(Integer id, String tenThietBi) {
        ThietBi tb = findById(id);
        tb.setTenThietBi(tenThietBi);
        return thietBiRepository.save(tb);
    }

    @Override
    public void deleteById(Integer id) {
        ThietBi tb = findById(id);
        long countPhong = phongThietBiRepository.countByThietBi_Id(id);
        if (countPhong > 0) {
            throw new BusinessException("Thiết bị đang được gắn vào " + countPhong + " phòng, hãy gỡ trước khi xóa.");
        }
        if (suCoRepository.existsByThietBi_Id(id)) {
            throw new BusinessException("Thiết bị đang có trong hồ sơ sự cố, không thể xóa.");
        }
        thietBiRepository.delete(tb);
    }

    // ----- Quan ly phong-thiet_bi -----

    public void themVaoPhong(Integer phongId, Integer thietBiId) {
        if (phongThietBiRepository.existsByPhong_IdAndThietBi_Id(phongId, thietBiId)) {
            throw new BusinessException("Thiết bị này đã có trong phòng.");
        }
        com.qlytro.entity.Phong phong = new com.qlytro.entity.Phong();
        phong.setId(phongId);
        ThietBi tb = new ThietBi();
        tb.setId(thietBiId);
        com.qlytro.entity.PhongThietBi ptb = new com.qlytro.entity.PhongThietBi();
        ptb.setPhong(phong);
        ptb.setThietBi(tb);
        phongThietBiRepository.save(ptb);
    }

    public void xoaKhoiPhong(Integer phongId, Integer thietBiId) {
        phongThietBiRepository.deleteByPhong_IdAndThietBi_Id(phongId, thietBiId);
    }
}
