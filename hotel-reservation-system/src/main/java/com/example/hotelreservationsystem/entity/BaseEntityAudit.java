package com.example.hotelreservationsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@Data
public abstract class BaseEntityAudit extends BaseEntity{
    @Column
    private String createdBy;
    @Column
    private String updatedBy;
    @Column
    private Date createdAt;
    @Column
    private Date updatedAt;

}
