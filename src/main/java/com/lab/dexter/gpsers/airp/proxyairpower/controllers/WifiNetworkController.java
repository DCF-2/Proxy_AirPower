package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.WifiNetwork;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.WifiNetworkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wifi")
public class WifiNetworkController {

    private final WifiNetworkRepository repository;

    public WifiNetworkController(WifiNetworkRepository repository) {
        this.repository = repository;
    }

    // Para o aplicativo Android ler (Módulo 2)
    @GetMapping("/authorized")
    public ResponseEntity<List<WifiNetwork>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    // Para você cadastrar usando Postman/Web
    @PostMapping("/cadastrar")
    public ResponseEntity<WifiNetwork> create(@RequestBody WifiNetwork network) {
        return ResponseEntity.ok(repository.save(network));
    }
}
