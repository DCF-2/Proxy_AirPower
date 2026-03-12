package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.config.CryptoUtil;
import com.lab.dexter.gpsers.airp.proxyairpower.entities.WifiNetwork;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.WifiNetworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

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

    // --- 4. IMPORTAR CSV ---
    @PostMapping("/import")
        public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "O ficheiro está vazio."));
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int count = 0;

            // Lê linha a linha (Formato esperado: SSID;Senha;Local)
            while ((line = br.readLine()) != null) {
                // Ignora linhas de cabeçalho (ex: se a primeira palavra for SSID)
                if (line.toLowerCase().startsWith("ssid")) continue;

                String[] data = line.split("[,;]"); // Aceita separador por vírgula ou ponto-e-vírgula

                if (data.length >= 3) {
                    WifiNetwork net = new WifiNetwork();
                    net.setSsid(data[0].trim());
                    // Tranca a senha com a nossa super chave AES
                    net.setPassword(CryptoUtil.encrypt(data[1].trim()));
                    net.setLocation(data[2].trim());

                    repository.save(net);
                    count++;
                }
            }
            return ResponseEntity.ok(Map.of("message", "Sucesso! " + count + " redes importadas."));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao processar o ficheiro CSV: " + e.getMessage()));
        }
    }

    // --- 5. EDITAR REDE EXISTENTE ---
    @PutMapping("/authorized/{id}")
    public ResponseEntity<?> updateNetwork(@PathVariable Long id, @RequestBody WifiNetwork updatedNetwork) {
        return repository.findById(id).map(net -> {
            net.setSsid(updatedNetwork.getSsid());
            net.setLocation(updatedNetwork.getLocation());

            // Só atualiza a senha se o admin digitou uma nova (evita apagar a antiga sem querer)
            if (updatedNetwork.getPassword() != null && !updatedNetwork.getPassword().isEmpty()) {
                net.setPassword(CryptoUtil.encrypt(updatedNetwork.getPassword()));
            }

            repository.save(net);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}