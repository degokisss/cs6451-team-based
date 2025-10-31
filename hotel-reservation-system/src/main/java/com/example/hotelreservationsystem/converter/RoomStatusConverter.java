package com.example.hotelreservationsystem.converter;

import com.example.hotelreservationsystem.entity.RoomStatus;
import jakarta.persistence.AttributeConverter;

@jakarta.persistence.Converter(autoApply = true)
public class RoomStatusConverter implements AttributeConverter<RoomStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(RoomStatus roomStatus) {
        return roomStatus.ordinal();
    }

    @Override
    public RoomStatus convertToEntityAttribute(Integer integer) {
        return RoomStatus.values()[integer];
    }
}
