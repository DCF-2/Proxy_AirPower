package com.lab.dexter.gpsers.airp.proxyairpower.dtos;

public record CreateDeviceRequest(
        String name,
        String type,
        String label // Onde guardamos a localização (ex: "Laboratório 1")
) {}
