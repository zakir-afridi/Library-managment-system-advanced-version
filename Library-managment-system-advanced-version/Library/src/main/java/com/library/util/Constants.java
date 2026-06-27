package com.library.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Constants {

    public static final String APP_NAME    = "LibraCore Pro";
    public static final String APP_VERSION = "v3.0.0";

    // SHA-256 of "03150315"
    private static final String RECOVERY_KEY_HASH =
            sha256("03150315");

    public static boolean isValidRecoveryKey(String input) {
        if (input == null) return false;
        return RECOVERY_KEY_HASH.equals(sha256(input.trim()));
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
