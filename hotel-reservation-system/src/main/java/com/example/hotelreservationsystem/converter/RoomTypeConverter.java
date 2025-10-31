package com.example.hotelreservationsystem.converter;

import com.example.hotelreservationsystem.entity.RoomType;
import jakarta.persistence.AttributeConverter;

@jakarta.persistence.Converter(autoApply = true)
public class RoomTypeConverter implements AttributeConverter<RoomType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(RoomType roomType) {
        return roomType.roomTypeId;
    }

    @Override
    public RoomType convertToEntityAttribute(Integer integer) {
        for(RoomType roomType: RoomType.values()) {
            if (roomType.roomTypeId == integer) {
                return roomType;
            }
        }
        return RoomType.NONE;
    }
}
