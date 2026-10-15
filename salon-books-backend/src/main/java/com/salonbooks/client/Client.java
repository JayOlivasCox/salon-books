package com.salonbooks.client;

import com.salonbooks.common.SyncableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "client")
@Getter
@Setter
public class Client extends SyncableEntity {
    @Column(nullable = false)
    private String name;
    private String phone;
    private String email;
    private String notes;
}
