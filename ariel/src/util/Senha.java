package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Guarda e confere senhas com PBKDF2 (já vem no Java). Cada senha tem um "sal" aleatório,
 * então duas senhas iguais geram hashes diferentes. Formato guardado: "sal:hash" em Base64.
 */
public final class Senha {
    private static final int ITERACOES = 100_000;
    private static final int BITS = 256;

    private Senha() { }

    public static String gerarHash(char[] senha) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal) + ":" + Base64.getEncoder().encodeToString(derivar(senha, sal));
    }

    public static boolean confere(char[] senha, String guardado) {
        String[] partes = guardado.split(":");
        if (partes.length != 2) return false;
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] esperado = Base64.getDecoder().decode(partes[1]);
        return MessageDigest.isEqual(esperado, derivar(senha, sal));   // comparação em tempo constante
    }

    private static byte[] derivar(char[] senha, byte[] sal) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(senha, sal, ITERACOES, BITS)).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível neste Java", e);
        }
    }
}
