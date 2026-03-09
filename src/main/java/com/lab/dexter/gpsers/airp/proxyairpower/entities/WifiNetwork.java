package com.lab.dexter.gpsers.airp.proxyairpower.entities;

import jakarta.persistence.*;
import lombok.Data; // Lombok gera Getters e Setters automaticamente

@Data
@Entity
@Table(name = "wifi_networks")
public class WifiNetwork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ssid;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String location;
}