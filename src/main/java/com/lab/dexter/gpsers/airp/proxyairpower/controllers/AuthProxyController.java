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
        String dynamicTbUrl = user.getTbUrl();
        String dynamicTbUser = user.getTbUsername();
        String dynamicTbPass = null;

        try {
            dynamicTbPass = CryptoUtil.decrypt(user.getTbPassword());
        } catch (Exception e) {
            System.err.println("⚠️ AVISO: Falha ao descriptografar a senha. Usando valor puro do banco.");
            dynamicTbPass = user.getTbPassword(); // Se falhar a descriptografia, assume que tá em texto puro
        }

        // 🕵️ LOG DETETIVE: Imprime exatamente o que vamos mandar (Os colchetes mostram se tem espaços invisíveis)
        System.out.println("🕵️ DETETIVE -> User TB: [" + dynamicTbUser + "]");
        System.out.println("🕵️ DETETIVE -> Pass TB: [" + dynamicTbPass + "]");

        try {
            RestTemplate restTemplate = createBlindRestTemplate();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));

            // 🛡️ BLINDAGEM MÁXIMA DO JSON: Forçamos a String na mão para o Spring não formatar errado
            String jsonBody = "{\"username\":\"" + dynamicTbUser + "\", \"password\":\"" + dynamicTbPass + "\"}";
            System.out.println("🕵️ DETETIVE -> Payload JSON: " + jsonBody);

            org.springframework.http.HttpEntity<String> requestEntity = new org.springframework.http.HttpEntity<>(jsonBody, headers);

            // Bate no servidor (HTTPS) ignorando a data de validade do certificado!
            ResponseEntity<Map> tbResponse = restTemplate.postForEntity(
                    dynamicTbUrl + "/api/auth/login",
                    requestEntity, // Usa o nosso JSON cravado na mão
                    Map.class
            );

            if (tbResponse.getBody() == null || !tbResponse.getBody().containsKey("token")) {
                return ResponseEntity.status(502).body(Map.of("error", "O ThingsBoard não retornou um token válido."));
            }

            Map<String, Object> responseBody = new HashMap<>(tbResponse.getBody());
            responseBody.put("tbUrl", dynamicTbUrl);

            return ResponseEntity.ok(responseBody);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("⚠️ [AuthProxyController]: ThingsBoard recusou o login. Status: " + e.getStatusCode());
            if (e.getStatusCode().value() == 401) {
                return ResponseEntity.status(401).body(Map.of("error", "As credenciais do ThingsBoard salvas no banco estão incorretas. Atualize a senha do Tenant."));
            }
            return ResponseEntity.status(502).body(Map.of("error", "O ThingsBoard retornou erro: " + e.getStatusCode()));

        } catch (Exception e) {
            System.err.println("❌ ERRO [AuthProxyController]: Falha na rede ao conectar no ThingsBoard: " + e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "O servidor ThingsBoard (10.5.0.66) está offline ou inacessível."));
        }
    }
}