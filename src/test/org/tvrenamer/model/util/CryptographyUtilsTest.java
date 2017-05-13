package org.tvrenamer.model.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class CryptographyUtilsTest {

    /**
     * Test that the encryption algorithm works forwards and backwards.
     */
    @Test
    public void testTwoWay() {
        String password = "password";
        String encryptedPassword = CryptographyUtils.encrypt(password);
        assertNotEquals(password, encryptedPassword);  // Ensure it did something

        String decryptedEncryptedPassword = CryptographyUtils.decrypt(encryptedPassword);
        assertEquals(password, decryptedEncryptedPassword);  // Ensure that we are back where we started
    }

    /**
     * Test that the algorithm works when writing the file to and from disk
     * (and therefore susceptible to incorrect line endings)
     * @throws Exception
     */
    @Test
    public void testWritingToFile() throws Exception {
        String password = "p";
        String encryptedPassword = CryptographyUtils.encrypt(password);

        // Write the encrypted password to a file
        Path tempFile = Files.createTempFile("encrypted-password-", ".txt");

        try (OutputStream fileOutputStream = Files.newOutputStream(tempFile)) {
            fileOutputStream.write(encryptedPassword.getBytes());
        } catch (IOException ioe) {
            fail("could not encrypted password output file");
        }

        long outputSize = Files.size(tempFile);
        byte[] buffer = new byte[(int) outputSize];
        int bytesRead = -1;
        try (InputStream f = Files.newInputStream(tempFile)) {
            bytesRead = f.read(buffer);
        }
        assertTrue(bytesRead > 0);
        String encryptedPasswordFromFile = new String(buffer);

        String decryptedEncryptedPasswordFromFile = CryptographyUtils.decrypt(encryptedPasswordFromFile);

        assertEquals(password, decryptedEncryptedPasswordFromFile);
        Files.delete(tempFile);
    }
}
