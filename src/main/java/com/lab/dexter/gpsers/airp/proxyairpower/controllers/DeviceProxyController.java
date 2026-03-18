package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@RestController
@RequestMapping("/api/proxy")
@CrossOrigin(origins = "*")
public class DeviceProxyController {

    @Autowired
    private AppUserRepository userRepository;

    // --- MÉTODOS AUXILIARES ---

    // 1. Prepara a "mala" com o Token do Android para enviar ao ThingsBoard
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        return headers;
    }

    // 2. Descobre a URL do ThingsBoard baseada em quem está a fazer o pedido
    private String getDynamicTbUrl(String email) {
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getTbUrl() != null) {
            return userOpt.get().getTbUrl();
        }
        throw new RuntimeException("Utilizador não encontrado ou TB URL não configurada no banco.");
    }

    // --- ROTAS DE ESPELHO (O GATEWAY) ---

    // 1. BUSCAR DISPOSITIVOS
    @GetMapping("/tenant/devices")
    public ResponseEntity<?> getDevices(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email, // O Android vai mandar isto!
            @RequestParam(defaultValue = "10000") int pageSize,
            @RequestParam(defaultValue = "0") int page) {

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            // Monta o destino final
            String targetUrl = tbUrl + "/api/tenant/devices?pageSize=" + pageSize + "&page=" + page;

            // Dispara o pedido
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Get Devices): " + e.getMessage());
        }
    }

    // 2. CRIAR DISPOSITIVO
    @PostMapping("/device")
    public ResponseEntity<?> createDevice(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @RequestBody String devicePayload) { // Recebe o JSON exato que o Android mandar

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(devicePayload, createHeaders(token));

            String targetUrl = tbUrl + "/api/device";

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Create Device): " + e.getMessage());
        }
    }

    // 3. OBTER CREDENCIAIS (O TOKEN DA ESP32)
    @GetMapping("/device/{deviceId}/credentials")
    public ResponseEntity<?> getDeviceCredentials(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @PathVariable String deviceId) {

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            String targetUrl = tbUrl + "/api/device/" + deviceId + "/credentials";

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Get Credentials): " + e.getMessage());
        }
    }

    // 4. SALVAR LOCALIZAÇÃO (O MÉTODO QUE FALTAVA NO PROXY!)
    @PostMapping("/device/{deviceId}/location")
    public ResponseEntity<?> saveDeviceLocation(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @PathVariable String deviceId,
            @RequestBody String locationPayload) { // Recebe o DTO do Android como String JSON

        try {
            // 1. Pega a URL real do ThingsBoard
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(locationPayload, createHeaders(token));

            // 2. Monta o destino final no ThingsBoard
            // (No ThingsBoard, os dados como descrição/GPS são salvos como Atributos de Servidor)
            String targetUrl = tbUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/SERVER_SCOPE";

            // 3. Dispara o pedido
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Save Location): " + e.getMessage());
        }
    }
}