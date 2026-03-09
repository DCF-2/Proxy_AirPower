package com.lab.dexter.gpsers.airp.proxyairpower.services;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class DeviceProxyServiceTest {

    @Test
    void testGetTenantDevicesSuccess() throws Exception {
        // 1. Preparação (Mock do RestTemplate e do JSON de resposta)
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        DeviceProxyService service = new DeviceProxyService(mockRestTemplate);

        // Criamos um JSON falso para simular a resposta do ThingsBoard
        String jsonResponse = "{\"data\": [{\"id\": {\"id\": \"123\"}, \"name\": \"Sensor Lab\"}], \"hasNext\": false}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode mockNode = mapper.readTree(jsonResponse);

        Mockito.when(mockRestTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(new ResponseEntity<>(mockNode, HttpStatus.OK));

        // 2. Execução (Passando o token falso e o IP dinâmico)
        JsonNode response = service.getTenantDevices("Bearer tokenFalso", "https://ip-do-lab:8080", 10, 0);

        // 3. Verificação
        assertNotNull(response);
        assertNotNull(response.get("data"));
    }
}