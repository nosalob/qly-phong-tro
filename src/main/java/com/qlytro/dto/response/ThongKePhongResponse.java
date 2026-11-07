package com.qlytro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongKePhongResponse {
    private long tongSoPhong;
    private long dangChoThue;
    private double dangChoThuePct;
    private long phongTrong;
    private double phongTrongPct;
    private long ngungSuDung;
    private double ngungSuDungPct;
}

