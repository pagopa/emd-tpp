package it.gov.pagopa.common.utils;

import it.gov.pagopa.common.web.exception.EmdEncryptionException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class providing common helper methods for SHA-256 hash generation
 * and input sanitization.
 */
@Slf4j
public class Utils {

    private Utils(){}
    
    /**
     * Creates a SHA-256 hash of the provided fiscal code.
     * <p>
     * This method generates a cryptographic hash using the SHA-256 algorithm
     * to ensure secure handling of sensitive personal information.
     * The input string is encoded using UTF-8 character encoding before hashing.
     *
     * @param fiscalCode the fiscal code to hash
     * @return a lowercase hexadecimal string representation of the SHA-256 hash
     *         of the input fiscal code
     * @throws EmdEncryptionException if the SHA-256 algorithm is not available
     *                                or if any error occurs during the hashing process
     */
    public static String createSHA256(String fiscalCode)  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = md.digest(fiscalCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
                log.info("Something went wrong creating SHA256");
            throw new EmdEncryptionException("Something went wrong creating SHA256",true,e);
        }
    }

    /**
     * Sanitizes input strings for safe logging by removing line break characters.
     * <p>
     * This method prevents log injection attacks by replacing newline ({@code \n})
     * and carriage return ({@code \r}) characters with spaces. This ensures that
     * log entries remain on a single line and cannot be used to inject fake log
     * entries or manipulate log formatting.
     *
     * @param message the input string to sanitize
     * @return the sanitized string with line breaks replaced by spaces,
     *         or a warning message if the input is {@code null}
     */
    public static String inputSanitization(String message){
        if (message != null)
           return message.replace("\n", " ").replace("\r", " ");
       return "[EMD][WARNING] Null log";
    }

}

