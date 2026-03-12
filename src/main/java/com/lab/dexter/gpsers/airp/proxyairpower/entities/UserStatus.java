package com.lab.dexter.gpsers.airp.proxyairpower.entities;

public enum UserStatus {
    PENDING,   // Acabou de se registar, aguarda aprovação do Admin
    APPROVED,  // Aprovado e com acesso liberado
    REJECTED,  // Cadastro negado
    BANNED     // Foi banido do sistema
}