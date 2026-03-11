package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.WifiNetwork;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.WifiNetworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wifi")
@CrossOrigin(origins = "*")
public class WifiNetworkController {

    @Autowired
    private WifiNetworkRepository repository;

    // 1. BUSCAR TODAS AS REDES (O que o Android já usa)
    @GetMapping("/authorized")
    public List<WifiNetwork> getAuthorizedNetworks() {
        return repository.findAll();
    }

    // 2. ADICIONAR NOVA REDE (Para a Web)
    @PostMapping("/authorized")
    public WifiNetwork addNetwork(@RequestBody WifiNetwork network) {
        return repository.save(network);
    }

    // 3. APAGAR REDE (Para a Web)
    @DeleteMapping("/authorized/{id}")
    public ResponseEntity<?> deleteNetwork(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}