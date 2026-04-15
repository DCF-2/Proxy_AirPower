package com.lab.dexter.gpsers.airp.proxyairpower.config;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

/**
 * Utilitário de Criptografia Refatorado (Segurança Forte).
 * Implementa AES/CBC/PKCS5Padding com Vetor de Inicialização (IV) aleatório.
 */
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    // A chave agora DEVE vir das variáveis de ambiente. Falha segura se não estiver definida.
    private static final String SECRET_KEY = getSecretKeyFromEnv();

    private static String getSecretKeyFromEnv() {
        String key = System.getenv("AIRPOWER_CRYPTO_KEY");
        if (key == null || key.length() != 16) {
            // Lança exceção para impedir que o app suba com configuração insegura
            throw new IllegalStateException("CRÍTICO: Variável de ambiente AIRPOWER_CRYPTO_KEY deve estar definida com exatamente 16 caracteres.");
        }
        return key;
    }

    /**
     * Encripta os dados usando AES e anexa o IV gerado no início da string resultante.
     */
    public static String encrypt(String value) {
        try {
            // Gera um IV aleatório e seguro
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encrypted = cipher.doFinal(value.getBytes());

            // Combina o IV e o texto cifrado para que possam ser recuperados na desencriptação
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao encriptar os dados", e);
        }
    }

    /**
     * Desencripta os dados extraindo o IV do início da string.
     */
    public static String decrypt(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            // Extrai o IV
            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // Extrai o texto cifrado real
            byte[] ciphertext = Arrays.copyOfRange(combined, 16, combined.length);

            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao desencriptar os dados", e);
        }
    }
}