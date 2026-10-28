package com.qlytro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"LANDLORD"})
    public void testDoiMatKhauAsLandlord() throws Exception {
        mockMvc.perform(get("/doi-mat-khau"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mật khẩu cũ")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Yêu cầu đổi mật khẩu"))));
    }

    @Test
    @WithMockUser(username = "khach1", roles = {"TENANT"})
    public void testDoiMatKhauAsTenant() throws Exception {
        mockMvc.perform(get("/doi-mat-khau"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mật khẩu cũ")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Yêu cầu đổi mật khẩu"))));
    }

    @Test
    @WithMockUser(username = "khach1", roles = {"TENANT"})
    public void testDoiMatKhauRequired() throws Exception {
        mockMvc.perform(get("/doi-mat-khau?required=true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yêu cầu đổi mật khẩu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("123456")));
    }
}
