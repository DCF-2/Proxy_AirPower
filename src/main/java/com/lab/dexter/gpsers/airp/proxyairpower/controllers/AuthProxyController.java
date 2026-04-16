package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.config.CryptoUtil;
import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import com.lab.dexter.gpsers.airp.proxyairpower.entities.UserStatus;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/proxy/auth")
@CrossOrigin(origins = "*")
public class AuthProxyController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String plainPassword = credentials.get("password");

        // 1. Procura o utilizador no banco
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Utilizador não encontrado."));
        }

        AppUser user = userOpt.get();

        // 2. Valida a senha da nossa API (BCrypt)
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Senha incorreta."));
        }

        // 3. Valida se está Aprovado
        if (user.getStatus() != UserStatus.APPROVED) {
            return ResponseEntity.status(403).body(Map.of("error", "Acesso negado. Status atual: " + user.getStatus()));
        }

        // 4. Valida se expirou
        if (user.getExpirationDate() != null && user.getExpirationDate().isBefore(LocalDateTime.now())) {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
            return ResponseEntity.status(403).body(Map.of("error", "O seu tempo de acesso expirou."));
        }

        // 5. Verifica se o utilizador tem as credenciais do ThingsBoard configuradas
        if (user.getTbUrl() == null || user.getTbUsername() == null || user.getTbPassword() == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Credenciais do ThingsBoard não configuradas para este utilizador. Fale com o Administrador."));
        }

        // 6. DINÂMICO: Pega os dados exatos deste utilizador e destranca a senha!
        String dynamicTbUrl = user.getTbUrl();

        if (dynamicTbUrl != null && (dynamicTbUrl.startsWith("https://10.") || dynamicTbUrl.startsWith("https://192."))) {
            System.out.println("🔄 AVISO [AuthProxyController]: Interceptando URL de laboratório. Forçando HTTP para: " + dynamicTbUrl);
            dynamicTbUrl = dynamicTbUrl.replace("https://", "http://");
        }

        String dynamicTbUser = user.getTbUsername();
        String dynamicTbPass = CryptoUtil.decrypt(user.getTbPassword()); // Destranca a senha na hora!

        try {
            // Como forçamos HTTP, um RestTemplate padrão funciona perfeitamente!
            RestTemplate restTemplate = new RestTemplate();
            Map<String, String> tbCredentials = new HashMap<>();
            tbCredentials.put("username", dynamicTbUser);
            tbCredentials.put("password", dynamicTbPass);

            // Faz a requisição de login ao ThingsBoard dinâmico
            ResponseEntity<Map> tbResponse = restTemplate.postForEntity(
                    dynamicTbUrl + "/api/auth/login",
                    tbCredentials,
                    Map.class
            );

            // Verifica se o corpo é nulo antes de enviar para o Android
            if (tbResponse.getBody() == null || !tbResponse.getBody().containsKey("token")) {
                return ResponseEntity.status(502).body(Map.of("error", "O ThingsBoard não retornou um token válido."));
            }

            // Pegamos a resposta do ThingsBoard e adicionamos a URL real do usuário!
            Map<String, Object> responseBody = new HashMap<>(tbResponse.getBody());
            responseBody.put("tbUrl", dynamicTbUrl);

            // ---> RETORNA O responseBody COM A URL DENTRO! <---
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            // Agora o Spring Boot vai gritar no log o motivo exato do erro!
            System.err.println("❌ ERRO [AuthProxyController]: Falha ao conectar no ThingsBoard: " + e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "Erro ao conectar com o ThingsBoard: " + e.getMessage()));
        }
    }
}