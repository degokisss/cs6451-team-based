package com.example.hotelreservationsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "room_type")
public class RoomType extends BaseEntityAudit {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private float price;

    @Column
    private Integer capacity;
}
