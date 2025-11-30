package com.example.hotelreservationsystem.converter;

import com.example.hotelreservationsystem.enums.RoomStatus;
import jakarta.persistence.AttributeConverter;

/**
 * JPA attribute converter that maps {@link RoomStatus} to an Integer database column
 * and back.
 *
 * <p>This converter uses the enum's {@link Enum#ordinal() ordinal} value for
 * persistence. It also performs defensive checks when converting from the
 * database value back to the enum to avoid {@link ArrayIndexOutOfBoundsException}.
 * Null values are preserved (null -> null).</p>
 *
 * <p>Note: Using ordinal() ties the stored values to the enum declaration order.
 * If enum order changes, previously stored values may no longer map correctly.
 * Consider storing an explicit code/value in the enum if the mapping must be stable.</p>
 */
@jakarta.persistence.Converter(autoApply = true)
public class RoomStatusConverter implements AttributeConverter<RoomStatus, Integer> {

    /**
     * Convert the enum value to its database representation.
     *
     * @param roomStatus the enum value to convert, may be {@code null}
     * @return the ordinal {@link Integer} to store in the database, or {@code null} if input is {@code null}
     */
    @Override
    public Integer convertToDatabaseColumn(RoomStatus roomStatus) {
        if (roomStatus == null)
            return null;

        return roomStatus.ordinal();
    }

    /**
     * Convert the database value (stored as Integer) back to the enum.
     *
     * @param value the integer value from the database, may be {@code null}
     * @return the corresponding {@link RoomStatus} enum instance, or {@code null} if input is {@code null}
     * @throws IllegalArgumentException if the integer value is outside the valid ordinal range for {@link RoomStatus}
     */
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