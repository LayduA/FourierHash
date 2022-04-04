package src.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


public interface SecureCrypto {

    static String getHash(String in) {
        MessageDigest digester;
        try {
            digester = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digester.digest(in.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e);
            return "";
        }

    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    static int[][] randomArray(String seed, int width, int height){
        int[][] arr = new int[width][height];
        SecureRandom prng = new SecureRandom();
        prng.setSeed(seed.getBytes());
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                arr[i][j] = prng.nextInt(16);
            }
        }
        return arr;
    }
}
