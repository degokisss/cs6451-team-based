package com.example.hotelreservationsystem.entity;

import com.example.hotelreservationsystem.converter.RoomStatusConverter;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(
    name = "room",
    indexes = {
        @Index(name = "idx_room_status", columnList = "room_status"),
        @Index(name = "idx_room_type_status", columnList = "room_type_id, room_status"),
        @Index(name = "idx_hotel_id", columnList = "hotel_id")
    }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Room extends BaseEntityAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id") // 外键列
    private Hotel hotel;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "room_status")
    @Convert(converter = RoomStatusConverter.class)
    private RoomStatus roomStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;
}
