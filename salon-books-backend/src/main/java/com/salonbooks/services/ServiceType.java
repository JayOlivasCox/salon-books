package com.salonbooks.services;

import com.salonbooks.common.SyncableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A kind of service (haircut, balayage, ...). No fixed price: prices are learned from income entries. */
@Entity
@Table(name = "service_type")
@Getter
@Setter
public class ServiceType extends SyncableEntity {
    @Column(nullable = false)
    private String name;

    @Column(name = "default_duration_minutes")
    private Integer defaultDurationMinutes;
}
