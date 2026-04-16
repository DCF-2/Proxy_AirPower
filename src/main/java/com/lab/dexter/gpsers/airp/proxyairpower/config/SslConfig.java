package com.lab.dexter.gpsers.airp.proxyairpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;

@Configuration
public class SslConfig {

    @Bean
    public RestTemplate restTemplate() throws Exception {
        System.out.println("AVISO CRÍTICO: Forçando RestTemplate nativo a ignorar Certificados Expirados.");

        // Estratégia "kamikaze" que aceita absolutamente qualquer certificado
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        // Força o uso do conector nativo (SimpleClientHttpRequestFactory)
        // Isso impede que o Apache HttpClient 5 assuma o controle da requisição
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
                }
                super.prepareConnection(connection, httpMethod);
            }
        };

        return new RestTemplate(factory);
    }
}