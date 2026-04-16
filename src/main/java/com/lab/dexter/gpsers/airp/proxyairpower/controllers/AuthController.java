package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.dtos.AuthRequestDTO;
import com.lab.dexter.gpsers.airp.proxyairpower.dtos.AuthResponseDTO;
import com.lab.dexter.gpsers.airp.proxyairpower.services.AuthProxyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/proxy/auth") // Ajustado para bater exatamente com a chamada do Android
public class AuthController {

    private final AuthProxyService authService;

    public AuthController(AuthProxyService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody AuthRequestDTO request,
            @RequestHeader(value = "X-ThingsBoard-URL", required = false) String thingsboardUrl) {

        try {
            // 1. Validação de Segurança (Feedback 400 - Bad Request)
            if (thingsboardUrl == null || thingsboardUrl.isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "O cabeçalho X-ThingsBoard-URL é obrigatório.");
            }

            // 2. A SANITIZAÇÃO IMPLACÁVEL (Resolve o bug do laboratório)
            // Se a URL vier com HTTPS para um IP local (10.x.x.x ou 192.x.x.x), forçamos para HTTP
            if (thingsboardUrl.startsWith("https://10.") || thingsboardUrl.startsWith("https://192.")) {
                System.out.println("🔄 AVISO [AuthController]: Interceptando URL de laboratório. Forçando HTTP para: " + thingsboardUrl);
                thingsboardUrl = thingsboardUrl.replace("https://", "http://");
            }

            // 3. Executa a autenticação real
            AuthResponseDTO response = authService.authenticate(request, thingsboardUrl);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Feedback para dados inválidos enviados pelo Android (Ex: Email vazio)
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            // Feedback 502 (Bad Gateway) formatado lindamente para o Android ler e exibir no Toast/Snackbar
            System.err.println("❌ ERRO [AuthController]: Falha na comunicação com o ThingsBoard - " + e.getMessage());
            return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Erro ao conectar com o servidor final: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", message);
        errorBody.put("status", String.valueOf(status.value()));
        return ResponseEntity.status(status).body(errorBody);
    }
}