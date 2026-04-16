package com.lab.dexter.gpsers.airp.proxyairpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Configuration
public class SslConfig {

    @Bean
    public Boolean disableSSLValidation() throws Exception {
        System.out.println("AVISO CRÍTICO: Validação de Certificado SSL Desabilitada Globalmente (Modo Laboratório).");

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Aceita qualquer nome de host (Hostname Verifier bypass)
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        return true;
    }
}