package com.example.hotelreservationsystem.entity;

import com.example.hotelreservationsystem.converter.RoomStatusConverter;
import com.example.hotelreservationsystem.converter.RoomTypeConverter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "room")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Room extends BaseEntityAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id") // 外键列
    private Hotel hotel;

    @Column
    private String roomNumber;

    @Column
    @Convert(converter = RoomStatusConverter.class)
    private RoomStatus roomStatus;

    @Column
    @Convert(converter = RoomTypeConverter.class)
    private RoomType roomType;
}
