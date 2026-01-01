package ma.emsi.batchservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption Service for Archive Files
 * Implements AES-256-GCM encryption for secure archive storage
 * Requirements: 10.4, 10.5
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${batch.encryption.key-vault.url:}")
    private String keyVaultUrl;

    // In production, this should be retrieved from a secure key vault
    // For now, we'll generate and store keys securely
    private SecretKey masterKey;

    /**
     * Initialize the encryption service with a master key
     * In production, this should retrieve the key from a secure key vault
     */
    public void initialize() {
        if (keyVaultUrl != null && !keyVaultUrl.isEmpty()) {
            // TODO: Integrate with key vault (Azure Key Vault, AWS KMS, HashiCorp Vault,
            // etc.)
            log.info("Key vault URL configured: {}", keyVaultUrl);
            // masterKey = retrieveFromKeyVault(keyVaultUrl);
        }

        // For now, generate a key (in production, retrieve from vault)
        if (masterKey == null) {
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                keyGenerator.init(KEY_SIZE);
                masterKey = keyGenerator.generateKey();
                log.warn("Generated temporary master key. In production, use a key vault!");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to initialize encryption service", e);
            }
        }
    }

    /**
     * Encrypt a file using AES-256-GCM
     * 
     * @param inputFile  Path to the file to encrypt
     * @param outputFile Path where encrypted file will be saved
     * @return Encryption key encoded as Base64 (to be stored securely)
     * @throws Exception if encryption fails
     */
    public String encryptFile(Path inputFile, Path outputFile) throws Exception {
        log.debug("Encrypting file: {} -> {}", inputFile, outputFile);

        // Generate a unique key for this file
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        SecretKey fileKey = keyGenerator.generateKey();

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, fileKey, parameterSpec);

        // Read input file and encrypt
        byte[] inputBytes = Files.readAllBytes(inputFile);
        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        // Write IV + encrypted data to output file
        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            fos.write(iv);
            fos.write(encryptedBytes);
        }

        log.info("File encrypted successfully: {}", outputFile);

        // Return the encryption key as Base64 (to be stored in database)
        return Base64.getEncoder().encodeToString(fileKey.getEncoded());
    }

    /**
     * Decrypt a file using AES-256-GCM
     * 
     * @param inputFile  Path to the encrypted file
     * @param outputFile Path where decrypted file will be saved
     * @param encodedKey Base64-encoded encryption key
     * @throws Exception if decryption fails
     */
    public void decryptFile(Path inputFile, Path outputFile, String encodedKey) throws Exception {
        log.debug("Decrypting file: {} -> {}", inputFile, outputFile);

        // Decode the encryption key
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        SecretKey fileKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);

        // Read encrypted file
        byte[] fileContent = Files.readAllBytes(inputFile);

        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(fileContent, 0, iv, 0, GCM_IV_LENGTH);

        byte[] encryptedBytes = new byte[fileContent.length - GCM_IV_LENGTH];
        System.arraycopy(fileContent, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, fileKey, parameterSpec);

        // Decrypt and write to output file
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        Files.write(outputFile, decryptedBytes);

        log.info("File decrypted successfully: {}", outputFile);
    }

    /**
     * Encrypt byte array data
     * 
     * @param data Byte array to encrypt
     * @return Encrypted byte array with IV prepended
     * @throws Exception if encryption fails
     */
    public byte[] encrypt(byte[] data) throws Exception {
        if (masterKey == null) {
            initialize();
        }

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

        // Encrypt data
        byte[] encryptedBytes = cipher.doFinal(data);

        // Combine IV + encrypted data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return combined;
    }

    /**
     * Decrypt byte array data
     * 
     * @param encryptedData Encrypted byte array with IV prepended
     * @return Decrypted byte array
     * @throws Exception if decryption fails
     */
    public byte[] decrypt(byte[] encryptedData) throws Exception {
        if (masterKey == null) {
            initialize();
        }

        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

        byte[] encryptedBytes = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

        // Decrypt
        return cipher.doFinal(encryptedBytes);
    }

    /**
     * Encrypt data in memory (for small data)
     * 
     * @param data Data to encrypt
     * @return Encrypted data with IV prepended, encoded as Base64
     * @throws Exception if encryption fails
     */
    public String encryptData(String data) throws Exception {
        if (masterKey == null) {
            initialize();
        }

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

        // Encrypt data
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());

        // Combine IV + encrypted data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt data in memory (for small data)
     * 
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @return Decrypted data as string
     * @throws Exception if decryption fails
     */
    public String decryptData(String encryptedData) throws Exception {
        if (masterKey == null) {
            initialize();
        }

        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

        // Decrypt
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }

    /**
     * Verify file encryption by attempting to decrypt
     * 
     * @param encryptedFile Path to encrypted file
     * @param encodedKey    Base64-encoded encryption key
     * @return true if file can be decrypted successfully
     */
    public boolean verifyEncryption(Path encryptedFile, String encodedKey) {
        try {
            // Try to decrypt to a temporary location
            Path tempFile = Files.createTempFile("verify", ".tmp");
            try {
                decryptFile(encryptedFile, tempFile, encodedKey);
                return true;
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            log.error("Encryption verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate a new encryption key for file encryption
     * 
     * @return Base64-encoded encryption key
     * @throws NoSuchAlgorithmException if algorithm is not available
     */
    public String generateFileKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        SecretKey key = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
