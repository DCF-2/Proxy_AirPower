package com.lab.dexter.gpsers.airp.proxyairpower.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "app_clients")
public class AppClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // Ex: "AirPower Admin App", "AirPower Student App"

    @Column(unique = true, nullable = false)
    private String appKey; // Uma chave única que o app vai mandar (ex: "APP-ADMIN-001")

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
}