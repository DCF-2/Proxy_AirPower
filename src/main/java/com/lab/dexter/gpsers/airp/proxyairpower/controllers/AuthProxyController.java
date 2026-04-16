package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.config.CryptoUtil;
import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import com.lab.dexter.gpsers.airp.proxyairpower.entities.UserStatus;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
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

    // Método privado para criar um RestTemplate que ignora o certificado SSL expirado do ThingsBoard
    private RestTemplate createBlindRestTemplate() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
            return new RestTemplate(factory);
        } catch (Exception e) {
            e.printStackTrace();
            return new RestTemplate(); // Fallback de emergência
        }
    }

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

        // 2. Valida a senha
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Senha incorreta."));
        }

        // 3. Valida Status
        if (user.getStatus() != UserStatus.APPROVED) {
            return ResponseEntity.status(403).body(Map.of("error", "Acesso negado. Status atual: " + user.getStatus()));
        }

        // 4. Valida Expiração
        if (user.getExpirationDate() != null && user.getExpirationDate().isBefore(LocalDateTime.now())) {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
            return ResponseEntity.status(403).body(Map.of("error", "O seu tempo de acesso expirou."));
        }

        // 5. Verifica credenciais TB
        if (user.getTbUrl() == null || user.getTbUsername() == null || user.getTbPassword() == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Credenciais do ThingsBoard não configuradas. Fale com o Administrador."));
        }

        // 6. Prepara conexão com ThingsBoard
        String dynamicTbUrl = user.getTbUrl(); // Aqui VAI usar HTTPS
        String dynamicTbUser = user.getTbUsername();
        String dynamicTbPass = CryptoUtil.decrypt(user.getTbPassword());

        try {
            // INVOCAMOS O NOSSO REST TEMPLATE BLINDADO AQUI!
            RestTemplate restTemplate = createBlindRestTemplate();

            // 1. FORÇA OS HEADERS DE JSON (Isso evita que o TB rejeite o corpo)
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));

            // 2. MONTA AS CREDENCIAIS
            Map<String, String> tbCredentials = new HashMap<>();
            tbCredentials.put("username", dynamicTbUser);
            tbCredentials.put("password", dynamicTbPass);

            org.springframework.http.HttpEntity<Map<String, String>> requestEntity = new org.springframework.http.HttpEntity<>(tbCredentials, headers);

            System.out.println("🚀 [AuthProxyController] Mandando pro TB -> URL: " + dynamicTbUrl + " | User: " + dynamicTbUser);

            // 3. Bate no servidor (HTTPS) ignorando a data de validade do certificado!
            ResponseEntity<Map> tbResponse = restTemplate.postForEntity(
                    dynamicTbUrl + "/api/auth/login",
                    requestEntity, // Usa a entidade com os Headers forçados!
                    Map.class
            );

            if (tbResponse.getBody() == null || !tbResponse.getBody().containsKey("token")) {
                return ResponseEntity.status(502).body(Map.of("error", "O ThingsBoard não retornou um token válido."));
            }

            Map<String, Object> responseBody = new HashMap<>(tbResponse.getBody());
            responseBody.put("tbUrl", dynamicTbUrl);

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            System.err.println("❌ ERRO [AuthProxyController]: Falha ao conectar no ThingsBoard: " + e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "Erro ao conectar com o ThingsBoard: " + e.getMessage()));
        }
    }
}