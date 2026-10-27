package com.juan.reservas.comun;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens secretos opacos: los refresh tokens y los enlaces de las reservas.
 * <p>
 * El valor se entrega una sola vez (en una cookie, en un enlace) y en la base de datos solo
 * se guarda su hash. Si alguien lee la base de datos, no obtiene tokens que funcionen.
 */
public final class TokenSecreto {

    private static final int BYTES = 32; // 256 bits de entropía
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenSecreto() {
    }

    /** 32 bytes aleatorios en Base64 URL sin relleno: 43 caracteres que se pueden poner en una URL. */
    public static String generar() {
        byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 en hexadecimal (64 caracteres). Basta un hash rápido, no hace falta BCrypt:
     * con 256 bits aleatorios no se puede adivinar el token por fuerza bruta como una contraseña.
     */
    public static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
