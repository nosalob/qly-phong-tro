package com.qlytro.entity.converter;

import com.qlytro.entity.enums.HinhThucNhac;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HinhThucNhacConverter implements AttributeConverter<HinhThucNhac, Byte> {

    @Override
    public Byte convertToDatabaseColumn(HinhThucNhac attribute) {
        return attribute == null ? null : (byte) attribute.getValue();
    }

    @Override
    public HinhThucNhac convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : HinhThucNhac.fromValue(dbData.intValue());
    }
}

