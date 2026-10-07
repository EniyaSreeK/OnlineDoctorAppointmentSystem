package com.odas.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PasswordUtil provides cryptographic password hashing and verification.
 * 
 * Purpose (for viva):
 * - Primary: Uses PBKDF2WithHmacSHA256 (210,000 iterations, 16-byte random salt, 256-bit key length).
 * - Format: pbkdf2$iterations$saltB64$hashB64
 * - Backwards-compatible: Verifies legacy 64-character SHA-256 hex hashes.
 * - Auto-upgrade support: Identifies legacy hashes for transparent re-hashing on login.
 */
public class PasswordUtil {

    private static final Logger LOGGER = Logger.getLogger(PasswordUtil.class.getName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final int PBKDF2_ITERATIONS = 210000;
    public static final int SALT_BYTES = 16;
    public static final int KEY_LENGTH_BITS = 256;
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final String PREFIX = "pbkdf2$";

    /**
     * Hashes password using PBKDF2WithHmacSHA256 with 210,000 iterations and a 16-byte salt.
     * 
     * @param plainPassword plain text password
     * @return Formatted hash string: pbkdf2$iterations$saltB64$hashB64
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return PREFIX + PBKDF2_ITERATIONS + "$" + saltB64 + "$" + hashB64;
    }

    /**
     * Validates input password against stored hash (PBKDF2 or legacy SHA-256).
     * 
     * @param plainPassword input password
     * @param storedHash stored password hash
     * @return true if password matches, false otherwise
     */
    public static boolean checkPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.trim().isEmpty()) {
            return false;
        }

        storedHash = storedHash.trim();

        // 1. Modern PBKDF2 Verification
        if (storedHash.startsWith(PREFIX)) {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
                byte[] actualHash = pbkdf2(plainPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
                return MessageDigest.isEqual(expectedHash, actualHash);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to verify PBKDF2 hash", e);
                return false;
            }
        }

        // 2. Legacy Unsalted SHA-256 (64 hex characters)
        if (storedHash.length() == 64 && storedHash.matches("^[0-9a-fA-F]{64}$")) {
            String legacyHash = hashLegacySha256(plainPassword);
            return MessageDigest.isEqual(
                    legacyHash.getBytes(StandardCharsets.UTF_8),
                    storedHash.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        }

        return false;
    }

    /**
     * Checks if the stored hash is a legacy hash or uses older iteration count.
     * 
     * @param storedHash stored password hash
     * @return true if hash should be upgraded to modern PBKDF2
     */
    public static boolean needsUpgrade(String storedHash) {
        if (storedHash == null || storedHash.trim().isEmpty()) {
            return true;
        }
        return !storedHash.startsWith(PREFIX + PBKDF2_ITERATIONS + "$");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error computing PBKDF2 hash", e);
        }
    }

    /**
     * Legacy SHA-256 hashing for backwards compatibility checks.
     */
    public static String hashLegacySha256(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing SHA-256", e);
        }
    }
}
