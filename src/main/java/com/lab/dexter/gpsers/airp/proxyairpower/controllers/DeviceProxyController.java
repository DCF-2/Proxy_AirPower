package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@RestController
@RequestMapping("/api/proxy")
@CrossOrigin(origins = "*")
public class DeviceProxyController {

    @Autowired
    private AppUserRepository userRepository;

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

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

    // ==========================================
    // ROTAS DE ESPELHO (O GATEWAY)
    // ==========================================

    // 1. BUSCAR DISPOSITIVOS (Rota antiga sem status, mantida por compatibilidade)
    @GetMapping("/tenant/devices")
    public ResponseEntity<?> getDevices(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @RequestParam(defaultValue = "10000") int pageSize,
            @RequestParam(defaultValue = "0") int page) {

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            String targetUrl = tbUrl + "/api/tenant/devices?pageSize=" + pageSize + "&page=" + page;

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
            @RequestBody String devicePayload) {

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

    // 4. SALVAR LOCALIZAÇÃO NA TELEMETRIA
    @PostMapping("/device/{deviceId}/location")
    public ResponseEntity<?> saveDeviceLocation(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @PathVariable String deviceId,
            @RequestBody String locationPayload) {

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(locationPayload, createHeaders(token));

            String targetUrl = tbUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/timeseries/ANY";

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Save Location): " + e.getMessage());
        }
    }

    // 5. BUSCAR A ÚLTIMA TELEMETRIA (MAPA E DASHBOARD DINÂMICO)
    @GetMapping("/device/{deviceId}/telemetry/latest")
    public ResponseEntity<?> getLatestTelemetry(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @PathVariable String deviceId,
            @RequestParam(value = "keys", required = false) String keys) { // Agora é OPCIONAL!

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            String targetUrl;

            // LÓGICA INTELIGENTE DE ROTEAMENTO
            if (keys != null && !keys.trim().isEmpty()) {
                // Cenário A: O Android pediu chaves específicas (ex: "latitude,longitude" para o Mapa)
                targetUrl = tbUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + keys;
            } else {
                // Cenário B: O Android quer o Dashboard Completo (todas as chaves)

                // Primeiro: Descobrimos quais são todas as chaves de telemetria desta placa
                String keysUrl = tbUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries";
                ResponseEntity<String[]> keysResponse = restTemplate.exchange(keysUrl, HttpMethod.GET, entity, String[].class);

                String[] availableKeys = keysResponse.getBody();

                if (availableKeys == null || availableKeys.length == 0) {
                    return ResponseEntity.ok("{}"); // A placa não tem dados
                }

                // Segundo: Montamos a string de chaves e fazemos o pedido final para obter os valores
                String allKeysString = String.join(",", availableKeys);
                targetUrl = tbUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + allKeysString;
            }

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Get Telemetry): " + e.getMessage());
        }
    }

    // 6. BUSCAR LISTA DE DASHBOARDS DO TENANT
    @GetMapping("/tenant/dashboards")
    public ResponseEntity<?> getDashboards(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "0") int page) {

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();

            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            String targetUrl = tbUrl + "/api/tenant/dashboards?pageSize=" + pageSize + "&page=" + page;

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Get Dashboards): " + e.getMessage());
        }
    }

    // 7. ENVIAR COMANDO RPC PARA A ESP32 (CONTROLE REMOTO)
    @PostMapping("/device/{deviceId}/rpc")
    public ResponseEntity<?> sendRpcCommand(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email,
            @PathVariable String deviceId,
            @RequestBody String rpcPayload) {

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = createHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(rpcPayload, headers);

            // API oficial do ThingsBoard para RPC Two-Way
            String targetUrl = tbUrl + "/api/plugins/rpc/twoway/" + deviceId;

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro ao enviar comando RPC: " + e.getMessage());
        }
    }

    // 8. BUSCAR LISTA DE DISPOSITIVOS COM STATUS ONLINE/OFFLINE (DeviceInfo)
    @GetMapping("/devices")
    public ResponseEntity<?> getDevicesWithStatus(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Email") String email) {

        try {
            String tbUrl = getDynamicTbUrl(email);
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

            // A MÁGICA ESTÁ AQUI: Usar 'deviceInfos' em vez de 'devices'
            String targetUrl = tbUrl + "/api/tenant/deviceInfos?pageSize=100&page=0";

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(502).body("Erro Gateway (Get DeviceInfos): " + e.getMessage());
        }
    }
}