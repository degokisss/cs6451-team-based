package com.example.hotelreservationsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "room_type")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
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
