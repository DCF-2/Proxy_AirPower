package com.lab.dexter.gpsers.airp.proxyairpower.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class TokenHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // LER OS HEADERS AQUI É SEGURO, ANTES DA REQUEST SER RECICLADA
            String authorization = servletRequest.getServletRequest().getHeader("Authorization");
            String email = servletRequest.getServletRequest().getHeader("X-User-Email");

            // GUARDAR NO MAPA DE ATRIBUTOS (Isto será passado para o WebSocketSession)
            if (authorization != null) {
                attributes.put("Auth-Token", authorization);
            }
            if (email != null) {
                attributes.put("User-Email", email);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nada a fazer após o handshake
    }
}