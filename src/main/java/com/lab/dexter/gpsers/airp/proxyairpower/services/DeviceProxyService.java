package com.lab.dexter.gpsers.airp.proxyairpower.services;

import com.lab.dexter.gpsers.airp.proxyairpower.dtos.CreateDeviceRequest;
import com.lab.dexter.gpsers.airp.proxyairpower.dtos.LocationRequest;
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

    private final RestTemplate restTemplate;

    public DeviceProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authorization", "Bearer " + token.replace("Bearer ", ""));
        headers.set("Content-Type", "application/json");
        return headers;
    }

    // 1. LISTAR DISPOSITIVOS (Aqui estava o erro das 3 vs 4 variáveis!)
    public JsonNode getTenantDevices(String token, String thingsboardUrl, int pageSize, int page) {
        String url = String.format("%s/api/tenant/devices?pageSize=%d&page=%d", thingsboardUrl, pageSize, page);
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        return response.getBody();
    }

    // 2. CRIAR DISPOSITIVO (Atualizado para receber a URL)
    public JsonNode createDevice(String token, String thingsboardUrl, CreateDeviceRequest request) {
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

    // 3. PEGAR CREDENCIAIS (Atualizado para receber a URL)
    public JsonNode getDeviceCredentials(String token, String thingsboardUrl, String deviceId) {
        String url = thingsboardUrl + "/api/device/" + deviceId + "/credentials";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        return response.getBody();
    }

    // 4. SALVAR LOCALIZAÇÃO (Atualizado para receber a URL)
    public void saveDeviceLocation(String token, String thingsboardUrl, String deviceId, LocationRequest location) {
        String url = thingsboardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/SERVER_SCOPE";

        Map<String, Double> body = new HashMap<>();
        body.put("latitude", location.latitude());
        body.put("longitude", location.longitude());

        HttpEntity<Map<String, Double>> entity = new HttpEntity<>(body, createHeaders(token));
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }
}