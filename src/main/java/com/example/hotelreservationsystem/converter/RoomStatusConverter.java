package com.example.hotelreservationsystem.converter;

import com.example.hotelreservationsystem.enums.RoomStatus;
import jakarta.persistence.AttributeConverter;

@jakarta.persistence.Converter(autoApply = true)
public class RoomStatusConverter implements AttributeConverter<RoomStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RoomStatus roomStatus) {
        if (roomStatus == null)
            return null;

        return roomStatus.ordinal();
    }

    @Override
    public RoomStatus convertToEntityAttribute(Integer value) {
        if (value == null)
            return null;

        RoomStatus[] statuses = RoomStatus.values();
        if (value < 0 || value >= statuses.length)
            throw new IllegalArgumentException("Unknown room status value: " + value + ". Valid range: 0-" + (statuses.length - 1));

        return statuses[value];
    }
}
