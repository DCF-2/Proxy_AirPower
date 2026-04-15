package com.lab.dexter.gpsers.airp.proxyairpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import javax.net.ssl.SSLContext;

// Imports corretos do HttpClient 5
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;

@Configuration
public class SslConfig {

    @Value("${trust.store.path:classpath:truststore.jks}")
    private String trustStorePath;

    @Value("${trust.store.password:}")
    private String trustStorePassword;

    @Bean
    public RestTemplate restTemplate() throws Exception {

        if (trustStorePassword == null || trustStorePassword.isEmpty()) {
            System.out.println("AVISO: trust.store.password não definida. Usando RestTemplate padrão.");
            return new RestTemplate();
        }

        File trustStoreFile = ResourceUtils.getFile(trustStorePath);

        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(trustStoreFile, trustStorePassword.toCharArray())
                .build();

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}