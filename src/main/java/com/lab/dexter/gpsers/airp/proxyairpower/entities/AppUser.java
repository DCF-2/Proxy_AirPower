package com.lab.dexter.gpsers.airp.proxyairpower.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // Aqui guardaremos a senha criptografada com BCrypt

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING; // Padrão é sempre Pendente

    private String role; // Ex: "TENANT_ADMIN", "STUDENT"

    private LocalDateTime expirationDate; // Data de validade (null = acesso vitalício)

    @ManyToOne
    @JoinColumn(name = "app_client_id", nullable = false)
    private AppClient appClient; // A qual app este utilizador pertence

    private String tbUrl;
    private String tbUsername;
    private String tbPassword;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
    public AppClient getAppClient() { return appClient; }
    public void setAppClient(AppClient appClient) { this.appClient = appClient; }
    public String getTbUrl() { return tbUrl; }
    public void setTbUrl(String tbUrl) { this.tbUrl = tbUrl; }
    public String getTbUsername() { return tbUsername; }
    public void setTbUsername(String tbUsername) { this.tbUsername = tbUsername; }
    public String getTbPassword() { return tbPassword; }
    public void setTbPassword(String tbPassword) { this.tbPassword = tbPassword; }
}