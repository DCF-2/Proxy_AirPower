package com.lab.dexter.gpsers.airp.proxyairpower.services;

import com.lab.dexter.gpsers.airp.proxyairpower.dtos.AuthRequestDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AuthProxyServiceTest {

    @Test
    void testAuthenticateSuccess() {
        // 1. Preparação (Mock)
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        AuthProxyService service = new AuthProxyService(mockRestTemplate);

        Map<String, String> mockResponse = Map.of(
                "token", "eyJhbG...",
                "refreshToken", "eyJhbG..."
        );

        Mockito.when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // 2. Execução
        AuthRequestDTO request = new AuthRequestDTO("admin@test.com", "123");
        var response = service.authenticate(request, "http://localhost:8080");

        // 3. Verificação
        assertNotNull(response.token());
        assertNotNull(response.refreshToken());
    }
}