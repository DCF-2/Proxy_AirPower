package com.lab.dexter.gpsers.airp.proxyairpower;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@SpringBootApplication
public class ProxyairpowerApplication {

	public static void main(String[] args) {
		disableSSLCertificateChecking();
		SpringApplication.run(ProxyairpowerApplication.class, args);
	}

	private static void disableSSLCertificateChecking() {
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

			// Isto aplica a regra para todas as conexões HttpsURLConnection da JVM (incluindo o StandardWebSocketClient)
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Opcional, mas recomendado para lab: desativa a verificação de Hostname também
			HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

			System.out.println("⚠️ AVISO: Verificação de Certificados SSL foi globalmente DESATIVADA (Ambiente de Laboratório).");
		} catch (Exception e) {
			System.err.println("Erro ao tentar desativar verificação SSL: " + e.getMessage());
		}
	}
}