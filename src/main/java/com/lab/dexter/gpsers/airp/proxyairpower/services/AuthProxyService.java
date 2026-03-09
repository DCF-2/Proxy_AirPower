package com.lab.dexter.gpsers.airp.proxyairpower.services;


import com.lab.dexter.gpsers.airp.proxyairpower.dtos.AuthRequestDTO;
import com.lab.dexter.gpsers.airp.proxyairpower.dtos.AuthResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AuthProxyService {


    private String thingsboardUrl;

    private final RestTemplate restTemplate;

    // Injeção de dependência via construtor (Melhor prática)
    public AuthProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AuthResponseDTO authenticate(AuthRequestDTO requestDTO, String thingsboardUrl) {
        String url = thingsboardUrl + "/api/auth/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AuthRequestDTO> request = new HttpEntity<>(requestDTO, headers);

        try {
            // Faz a chamada ao ThingsBoard
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            String token = response.getBody().get("token").toString();
            String refreshToken = response.getBody().get("refreshToken").toString();

            return new AuthResponseDTO(token, refreshToken);
        } catch (Exception e) {
            throw new RuntimeException("Credenciais inválidas ou ThingsBoard indisponível", e);
        }
    }
}