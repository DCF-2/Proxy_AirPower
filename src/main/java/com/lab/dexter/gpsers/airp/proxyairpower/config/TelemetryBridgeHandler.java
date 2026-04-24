package com.lab.dexter.gpsers.airp.proxyairpower.config;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppUserRepository;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class TelemetryBridgeHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryBridgeHandler.class);
    private final AppUserRepository userRepository;
    private final ConcurrentHashMap<String, WebSocket> tbSockets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocket, WebSocketSession> androidSessions = new ConcurrentHashMap<>();
    private final OkHttpClient okHttpClient;

    @Autowired
    public TelemetryBridgeHandler(AppUserRepository userRepository) {
        this.userRepository = userRepository;
        this.okHttpClient = createBlindOkHttpClient();
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
        if (token != null) {
            // Limpa o token de aspas, espaços e quebras de linha escondidas
            token = token.replaceAll("[\\n\\r\\\" ]", "");
        }

        if (token == null || email == null) {
            androidSession.close(CloseStatus.NOT_ACCEPTABLE.withReason("Faltam Headers"));
            return;
        }

        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || userOpt.get().getTbUrl() == null) {
            androidSession.close(CloseStatus.SERVER_ERROR.withReason("TB URL não configurada"));
            return;
        }

        String tbBaseUrl = userOpt.get().getTbUrl();
        String wsBaseUrl = tbBaseUrl.replace("https://", "wss://").replace("http://", "ws://");

        // 🚨 REMOVIDO O HACK DO DOWNGRADE 🚨
        // Descobrimos que o servidor ThingsBoard usa mesmo SSL na porta 8080!
        // Ao enviarmos ws:// (texto limpo) para uma porta WSS, recebíamos o HTTP 400 Bad Request!

        if (wsBaseUrl.endsWith("/")) {
            wsBaseUrl = wsBaseUrl.substring(0, wsBaseUrl.length() - 1);
        }

        // Endpoint original e limpo
        String dynamicTbWsUrl = wsBaseUrl + "/api/ws/plugins/telemetry?token=" + token;
        logger.info("🔗 A rotear telemetria para o ThingsBoard (com OkHttp): {}", wsBaseUrl);

        // Deixamos o OkHttp gerir os cabeçalhos nativamente
        Request request = new Request.Builder()
                .url(dynamicTbWsUrl)
                .build();

        okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("⚡ Ponte ativada! Proxy OkHttp conectado ao ThingsBoard.");
                tbSockets.put(androidSession.getId(), webSocket);
                androidSessions.put(webSocket, androidSession);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                WebSocketSession aSession = androidSessions.get(webSocket);
                if (aSession != null && aSession.isOpen()) {
                    try {
                        aSession.sendMessage(new TextMessage(text));
                    } catch (IOException e) {
                        logger.error("Erro a repassar msg para Android", e);
                    }
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                cleanupSockets(webSocket);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.error("❌ Falha na ponte OkHttp para o ThingsBoard: {} (Code: {})",
                        t.getMessage(), response != null ? response.code() : "null");
                cleanupSockets(webSocket);
            }
        });
    }

    private void cleanupSockets(WebSocket webSocket) {
        WebSocketSession aSession = androidSessions.remove(webSocket);
        if (aSession != null) {
            tbSockets.remove(aSession.getId());
            if (aSession.isOpen()) {
                try {
                    aSession.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession androidSession, TextMessage message) throws Exception {
        WebSocket tbSocket = tbSockets.get(androidSession.getId());
        if (tbSocket != null) {
            tbSocket.send(message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession androidSession, CloseStatus status) throws Exception {
        WebSocket tbSocket = tbSockets.remove(androidSession.getId());
        if (tbSocket != null) {
            androidSessions.remove(tbSocket);
            tbSocket.close(1000, "Android disconnected");
        }
    }

    private OkHttpClient createBlindOkHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .pingInterval(20, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}