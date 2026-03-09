package com.lab.dexter.gpsers.airp.proxyairpower.services;

import com.lab.dexter.gpsers.airp.proxyairpower.dtos.CreateDeviceRequest;

import com.lab.dexter.gpsers.airp.proxyairpower.dtos.LocationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeviceProxyService {

    @Value("${thingsboard.api.url}")
    private String thingsboardUrl;

    private final RestTemplate restTemplate;

    public DeviceProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Metodo auxiliar para montar os cabeçalhos com o Token
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        // O ThingsBoard aceita o cabeçalho X-Authorization ou o Authorization padrão
        headers.set("X-Authorization", "Bearer " + token.replace("Bearer ", ""));
        headers.set("Content-Type", "application/json");
        return headers;
    }

    // 1. LISTAR DISPOSITIVOS
    public JsonNode getTenantDevices(String token, int pageSize, int page) {
        // A API do ThingsBoard exige paginação
        String url = String.format("%s/api/tenant/devices?pageSize=%d&page=%d", thingsboardUrl, pageSize, page);

        HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        return response.getBody(); // Pode devolver o JSON Node cru, ou mapear para uma List<DeviceDTO> se preferir filtrar campos.
    }

    // 2. CRIAR DISPOSITIVO
    public JsonNode createDevice(String token, CreateDeviceRequest request) {
        String url = thingsboardUrl + "/api/device";

        Map<String, String> body = new HashMap<>();
        body.put("name", request.name());
        body.put("type", request.type());
        if (request.label() != null) {
            body.put("label", request.label());
        }

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, createHeaders(token));
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        return response.getBody();
    }

    // 3. PEGAR CREDENCIAIS (Para a ESP32 saber quem ela é)
    public JsonNode getDeviceCredentials(String token, String deviceId) {
        String url = thingsboardUrl + "/api/device/" + deviceId + "/credentials";

        HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        return response.getBody();
    }

    // 4. SALVAR LOCALIZAÇÃO (TELEMETRIA)
    public void saveDeviceLocation(String token, String deviceId, LocationRequest location) {
        // A rota de telemetria do ThingsBoard
        String url = thingsboardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/SERVER_SCOPE";

        Map<String, Double> body = new HashMap<>();
        body.put("latitude", location.latitude());
        body.put("longitude", location.longitude());

        HttpEntity<Map<String, Double>> entity = new HttpEntity<>(body, createHeaders(token));

        // Enviamos um POST para guardar os atributos/telemetria
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }
}