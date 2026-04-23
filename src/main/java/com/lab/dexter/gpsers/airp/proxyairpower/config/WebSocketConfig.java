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
        // Expõe o endpoint para o Android conectar (permitindo origens cruzadas)
        registry.addHandler(telemetryBridgeHandler, "/ws/telemetry")
                .setAllowedOrigins("*");
    }
}