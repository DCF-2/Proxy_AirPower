package com.lab.dexter.gpsers.airp.proxyairpower.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TelemetryBridgeHandler telemetryBridgeHandler;

    public WebSocketConfig(TelemetryBridgeHandler telemetryBridgeHandler) {
        this.telemetryBridgeHandler = telemetryBridgeHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(telemetryBridgeHandler, "/ws/telemetry")
                .setAllowedOriginPatterns("*"); // O setAllowedOrigins("*") em versões Spring mais novas pode não funcionar bem, setAllowedOriginPatterns é o correto!
    }
}