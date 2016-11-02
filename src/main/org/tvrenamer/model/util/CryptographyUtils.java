package org.tvrenamer.model.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;

public class CryptographyUtils {
    private static Logger logger = Logger.getLogger(CryptographyUtils.class.getName());

    private static final String SECRET_KEY_METHOD = "PBEWithMD5AndDES";
    private static final Base64 base64 = new Base64(76, "".getBytes());

    // A little bit of a security flaw having the password and salt in clear text, but securing the password in the settings file is more important
    private static final char[] PASSWORD = "sai;fdug213j,09ah2kfd/sa92n]sdf'65a".toCharArray();
    private static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };

    public static String encrypt(String value) {
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_METHOD);
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
            Cipher pbeCipher = Cipher.getInstance(SECRET_KEY_METHOD);
            pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
            return base64Encode(pbeCipher.doFinal(value.getBytes()));
        }
        catch(GeneralSecurityException gse) {
            logger.log(Level.WARNING, "Exception when encrypting value", gse);
        }

        return null;
    }

    private static String base64Encode(byte[] bytes) {
        return base64.encodeToString(bytes);
    }

    public static String decrypt(String value) {
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SECRET_KEY_METHOD);
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
            Cipher pbeCipher = Cipher.getInstance(SECRET_KEY_METHOD);
            pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
            return new String(pbeCipher.doFinal(base64Decode(value)));
        } catch(GeneralSecurityException gse) {
            logger.log(Level.WARNING, "Exception when encrypting value", gse);
        } catch(IOException ioe) {
            logger.log(Level.WARNING, "Exception when encrypting value", ioe);
        }

        return null;
    }

    private static byte[] base64Decode(String value) throws IOException {
        return base64.decode(value);
    }
}
