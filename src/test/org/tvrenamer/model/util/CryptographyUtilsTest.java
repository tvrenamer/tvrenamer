package org.tvrenamer.model.util;

import junit.framework.Assert;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class CryptographyUtilsTest {

    /**
     * Test that the encryption algorithm works forwards and backwards.
     */
    @Test
    public void testTwoWay() {
        String password = "password";
        String encryptedPassword = CryptographyUtils.encrypt(password);
        Assert.assertNotSame(password, encryptedPassword);  // Ensure it did something

        String decryptedEncryptedPassword = CryptographyUtils.decrypt(encryptedPassword);
        Assert.assertEquals(password, decryptedEncryptedPassword);  // Ensure that we are back where we started
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
        File tempFile = File.createTempFile("encrypted-password-", ".txt");

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        fileOutputStream.write(encryptedPassword.getBytes());
        fileOutputStream.close();

        byte[] buffer = new byte[(int) tempFile.length()];
        FileInputStream f = new FileInputStream(tempFile);
        f.read(buffer);
        f.close();
        String encryptedPasswordFromFile = new String(buffer);

        String decryptedEncryptedPasswordFromFile = CryptographyUtils.decrypt(encryptedPasswordFromFile);

        Assert.assertEquals(password, decryptedEncryptedPasswordFromFile);
        tempFile.delete();
    }
}
