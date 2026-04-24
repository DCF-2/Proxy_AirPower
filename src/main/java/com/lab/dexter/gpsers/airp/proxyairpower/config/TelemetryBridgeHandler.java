package com.lab.dexter.gpsers.airp.proxyairpower.config;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryBridgeHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryBridgeHandler.class);

    private final AppUserRepository userRepository;

    private final ConcurrentHashMap<String, WebSocketSession> tbSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> androidSessions = new ConcurrentHashMap<>();
    private final StandardWebSocketClient wsClient;

    @Autowired
    public TelemetryBridgeHandler(AppUserRepository userRepository) {
        this.userRepository = userRepository;

        this.wsClient = new StandardWebSocketClient();

        // No Spring Boot moderno/Jakarta, o Tomcat procura o SSL_CONTEXT nestas propriedades.
        // Usamos a string direta para evitar problemas de import de bibliotecas internas.
        SSLContext sslContext = createBlindSslContext();

        // 1. Define para o cliente padrão do Spring
        this.wsClient.getUserProperties().put("org.apache.tomcat.websocket.SSL_CONTEXT", sslContext);

        // 2. Garante que o motor subjacente (seja Tomcat ou Tyrus) receba a configuração
        // Algumas versões do Jakarta exigem esta chave específica:
        this.wsClient.getUserProperties().put("jakarta.websocket.ssl.context", sslContext);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession androidSession) throws Exception {
        logger.info("📱 Android conectado ao Proxy via WebSocket. ID: {}", androidSession.getId());

        String rawToken = (String) androidSession.getAttributes().get("Auth-Token");
        String email = (String) androidSession.getAttributes().get("User-Email");

        String token = rawToken;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || email == null) {
            logger.warn("❌ Conexão recusada: Token ou Email ausente no handshake.");
            androidSession.close(CloseStatus.NOT_ACCEPTABLE.withReason("Faltam Headers"));
            return;
        }

        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || userOpt.get().getTbUrl() == null) {
            logger.warn("❌ Conexão recusada: Utilizador inválido ou sem URL TB.");
            androidSession.close(CloseStatus.SERVER_ERROR.withReason("TB URL não configurada"));
            return;
        }

        String tbBaseUrl = userOpt.get().getTbUrl();
        String wsBaseUrl = tbBaseUrl.replace("https://", "wss://").replace("http://", "ws://");

        // 🚨 PROTEÇÃO CONTRA PROTOCOLO INVÁLIDO 🚨
        if (wsBaseUrl.contains(":8080") && wsBaseUrl.startsWith("wss://")) {
            logger.warn("⚠️ A corrigir protocolo WSS para WS na porta 8080...");
            wsBaseUrl = wsBaseUrl.replace("wss://", "ws://");
        }

        // Remover qualquer barra / no final da baseUrl para evitar erro 400 (ex: "http://ip:8080//api/ws...")
        if (wsBaseUrl.endsWith("/")) {
            wsBaseUrl = wsBaseUrl.substring(0, wsBaseUrl.length() - 1);
        }

        String dynamicTbWsUrl = wsBaseUrl + "/api/ws/plugins/telemetry?token=" + token;

        logger.info("🔗 A rotear telemetria para: {}", wsBaseUrl);
        WebSocketHandler tbHandler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession tbSession) {
                logger.info("⚡ Ponte ativada! Proxy conectado ao ThingsBoard. ID: {}", tbSession.getId());
                tbSessions.put(androidSession.getId(), tbSession);
                androidSessions.put(tbSession.getId(), androidSession);
            }

            @Override
            protected void handleTextMessage(WebSocketSession tbSession, TextMessage message) throws Exception {
                WebSocketSession aSession = androidSessions.get(tbSession.getId());
                if (aSession != null && aSession.isOpen()) {
                    aSession.sendMessage(message);
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession tbSession, CloseStatus status) throws Exception {
                logger.info("🔌 Conexão ThingsBoard fechada: {}", status);
                WebSocketSession aSession = androidSessions.remove(tbSession.getId());
                if (aSession != null) {
                    tbSessions.remove(aSession.getId());
                    if (aSession.isOpen()) aSession.close(status);
                }
            }
        };

        try {
            // A solução mágica: Executar num Thread separado e deixar o AndroidSession em paz
            new Thread(() -> {
                try {
                    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                    // Como não passámos os headers do Android diretamente, não há conflito de CORS.
                    // Apenas fazemos a chamada com os headers em branco e o token na query params.
                    wsClient.execute(tbHandler, headers, URI.create(dynamicTbWsUrl)).get();
                } catch (Exception e) {
                    logger.error("❌ Falha na thread de ligação: {}", e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            logger.error("❌ Erro ao instanciar thread: {}", e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession androidSession, TextMessage message) throws Exception {
        WebSocketSession tbSession = tbSessions.get(androidSession.getId());
        if (tbSession != null && tbSession.isOpen()) {
            tbSession.sendMessage(message);
        } else {
            logger.warn("Tentativa de envio do Android sem ponte ativa. Ignorando.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession androidSession, CloseStatus status) throws Exception {
        logger.info("📱 Android desconectado do Proxy: {}", status);
        WebSocketSession tbSession = tbSessions.remove(androidSession.getId());
        if (tbSession != null) {
            androidSessions.remove(tbSession.getId());
            if (tbSession.isOpen()) tbSession.close(status);
        }
    }

    private String extractHeader(WebSocketSession session, String headerName) {
        // Tenta ler dos HandshakeHeaders (onde o Tomcat coloca o Upgrade Request originalmente)
        List<String> headers = session.getHandshakeHeaders().get(headerName);
        return (headers != null && !headers.isEmpty()) ? headers.get(0) : null;
    }

    private SSLContext createBlindSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar Blind SSL Context", e);
            }
        }
    }