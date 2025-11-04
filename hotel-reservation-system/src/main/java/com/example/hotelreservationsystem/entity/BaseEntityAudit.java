package com.example.hotelreservationsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.util.Date;

@MappedSuperclass
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
