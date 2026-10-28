package com.qlytro.controller;

import com.qlytro.dto.response.HoaDonResponse;
import com.qlytro.entity.HoaDon;
import com.qlytro.entity.enums.TrangThaiHoaDon;
import com.qlytro.repository.HoaDonRepository;
import com.qlytro.service.impl.HoaDonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HoaDonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonService hoaDonService;

    @Test
    @WithMockUser(username = "admin", roles = {"LANDLORD"})
    @DisplayName("Truy cập /landlord/hoa-don không có bộ lọc: hiển thị đầy đủ bảng hóa đơn")
    public void testLandlordList_NoFilter_ReturnsFullListAndTable() throws Exception {
        MvcResult result = mockMvc.perform(get("/landlord/hoa-don"))
                .andExpect(status().isOk())
                .andReturn();

        List<HoaDonResponse> danhSach = (List<HoaDonResponse>) result.getModelAndView().getModel().get("danhSach");
        assertNotNull(danhSach);
        assertFalse(danhSach.isEmpty(), "Danh sách hóa đơn không được rỗng");

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("<table class=\"table\">"), "Giao diện phải có bảng table");
        assertTrue(html.contains("Quản lý Hóa đơn"));
        assertTrue(html.contains("Tất cả hóa đơn"));

        // Kiểm tra thứ tự sắp xếp: CHO_DUYET (0) -> CHUA_THANH_TOAN (1) -> DA_THANH_TOAN (2), trong từng nhóm tháng DESC
        int prevRank = -1;
        String prevThang = "9999-99";
        for (HoaDonResponse hd : danhSach) {
            int rank = getTrangThaiRank(hd.getTrangThai());
            if (rank > prevRank) {
                prevRank = rank;
                prevThang = hd.getThangNam();
            } else if (rank == prevRank) {
                assertTrue(prevThang.compareTo(hd.getThangNam()) >= 0,
                        "Trong cùng nhóm trạng thái, tháng mới hơn phải xuất hiện trước: " + prevThang + " vs " + hd.getThangNam());
                prevThang = hd.getThangNam();
            } else {
                fail("Thứ tự trạng thái không đúng: rank " + rank + " đứng sau rank " + prevRank);
            }
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = {"LANDLORD"})
    @DisplayName("Submit form lọc với tất cả tham số rỗng: phải trả về đầy đủ hóa đơn")
    public void testLandlordList_EmptyParams_ReturnsFullList() throws Exception {
        MvcResult result = mockMvc.perform(get("/landlord/hoa-don")
                        .param("toaNhaId", "")
                        .param("phongId", "")
                        .param("tuThang", "")
                        .param("denThang", "")
                        .param("trangThai", ""))
                .andExpect(status().isOk())
                .andReturn();

        List<HoaDonResponse> danhSach = (List<HoaDonResponse>) result.getModelAndView().getModel().get("danhSach");
        assertNotNull(danhSach);
        assertFalse(danhSach.isEmpty(), "Danh sách không được rỗng khi submit tham số rỗng");

        List<HoaDon> allInDb = hoaDonRepository.findAll();
        assertEquals(allInDb.size(), danhSach.size(), "Số lượng hóa đơn phải bằng tổng số trong DB");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"LANDLORD"})
    @DisplayName("Lọc theo trạng thái CHO_DUYET / CHUA_THANH_TOAN / DA_THANH_TOAN")
    public void testLandlordList_FilterByTrangThai() throws Exception {
        for (TrangThaiHoaDon tt : TrangThaiHoaDon.values()) {
            MvcResult result = mockMvc.perform(get("/landlord/hoa-don")
                            .param("trangThai", tt.name()))
                    .andExpect(status().isOk())
                    .andReturn();

            List<HoaDonResponse> danhSach = (List<HoaDonResponse>) result.getModelAndView().getModel().get("danhSach");
            assertNotNull(danhSach);
            for (HoaDonResponse hd : danhSach) {
                assertEquals(tt, hd.getTrangThai(), "Mỗi hóa đơn phải đúng trạng thái đã lọc");
            }
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = {"LANDLORD"})
    @DisplayName("Lọc kết hợp: tòa nhà, phòng, khoảng tháng và trạng thái")
    public void testLandlordList_FilterCombined() throws Exception {
        List<HoaDonResponse> all = hoaDonService.findAll();
        if (all.isEmpty()) return;

        HoaDonResponse sample = all.get(0);
        String thangNam = sample.getThangNam();
        TrangThaiHoaDon tt = sample.getTrangThai();

        MvcResult result = mockMvc.perform(get("/landlord/hoa-don")
                        .param("tuThang", thangNam)
                        .param("denThang", thangNam)
                        .param("trangThai", tt.name()))
                .andExpect(status().isOk())
                .andReturn();

        List<HoaDonResponse> danhSach = (List<HoaDonResponse>) result.getModelAndView().getModel().get("danhSach");
        assertNotNull(danhSach);
        assertFalse(danhSach.isEmpty(), "Phải tìm thấy ít nhất hóa đơn mẫu");
        for (HoaDonResponse hd : danhSach) {
            assertEquals(thangNam, hd.getThangNam());
            assertEquals(tt, hd.getTrangThai());
        }
    }

    private int getTrangThaiRank(TrangThaiHoaDon tt) {
        if (tt == TrangThaiHoaDon.CHO_DUYET) return 0;
        if (tt == TrangThaiHoaDon.CHUA_THANH_TOAN) return 1;
        if (tt == TrangThaiHoaDon.DA_THANH_TOAN) return 2;
        return 3;
    }
}
