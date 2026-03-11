package com.lab.dexter.gpsers.airp.proxyairpower.models;

import jakarta.persistence.*;

@Entity
@Table(name = "wifi_networks") // Conecta à tabela antiga
public class WifiNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ssid;

    private String password;

    // MUDANÇA AQUI: de "description" para "location"
    private String location;

    // Gere os Getters e Setters novamente para refletir o novo nome:
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}