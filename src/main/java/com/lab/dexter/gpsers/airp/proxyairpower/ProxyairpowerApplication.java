package com.lab.dexter.gpsers.airp.proxyairpower;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@SpringBootApplication
public class ProxyairpowerApplication {

	public static void main(String[] args) {
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(createBlindSslContext().getSocketFactory());
		SpringApplication.run(ProxyairpowerApplication.class, args);
	}
	private static SSLContext createBlindSslContext() {
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

