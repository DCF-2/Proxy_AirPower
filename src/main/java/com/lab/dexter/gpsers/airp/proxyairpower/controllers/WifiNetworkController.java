package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.config.CryptoUtil;
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

    // 1. BUSCAR TODAS AS REDES (Destranca as senhas para o Android ler)
    @GetMapping("/authorized")
    public List<WifiNetwork> getAuthorizedNetworks() {
        List<WifiNetwork> networks = repository.findAll();
        // Percorre a lista e destranca a senha de cada uma antes de enviar
        networks.forEach(net -> net.setPassword(CryptoUtil.decrypt(net.getPassword())));
        return networks;
    }

    // 2. ADICIONAR NOVA REDE (Tranca a senha antes de guardar no banco)
    @PostMapping("/authorized")
    public WifiNetwork addNetwork(@RequestBody WifiNetwork network) {
        // Encripta a senha que veio do formulário
        String encryptedPassword = CryptoUtil.encrypt(network.getPassword());
        network.setPassword(encryptedPassword);

        return repository.save(network);
    }

    // 3. APAGAR REDE
    @DeleteMapping("/authorized/{id}")
    public ResponseEntity<?> deleteNetwork(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}