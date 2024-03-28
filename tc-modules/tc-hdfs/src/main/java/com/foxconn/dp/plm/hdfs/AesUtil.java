package com.foxconn.dp.plm.hdfs;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public class AesUtil {

    private static String AES;

    static {
        try {
            Properties properties = PropertiesLoaderUtils.loadAllProperties("config.properties");
            AES = properties.getProperty("encryption.name");
            aesKey = properties.getProperty("encryption.key");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static String encrypt(String content) {
        return encrypt(content, aesKey);
    }

    public static String decrypt(String content) {
        return decrypt(content, aesKey);
    }

    public static void main(String[] args) {
        String admin = encrypt("empId=admin", "x.H@7h21*z%u?GCt{m");
        System.out.println(admin);
        System.out.println(decrypt(admin, "x.H@7h21*z%u?GCt{m"));
    }


    /**
     * AES 加密
     *
     * @param content 需要加密的内容
     * @param aesKey  加密密钥
     * @return
     */
    public static String encrypt(String content, String aesKey) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance(AES);
            kgen.init(128, new SecureRandom(aesKey.getBytes()));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, AES);
            Cipher cipher = Cipher.getInstance(AES);// 创建密码器
            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
            cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
            byte[] bytes = cipher.doFinal(byteContent);
            return parseByte2HexStr(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     *
     * @param content 待解密内容
     * @param aesKey  解密密钥 秘miyao
     * @return
     */
    public static String decrypt(String content, String aesKey) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance(AES);
            kgen.init(128, new SecureRandom(aesKey.getBytes()));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, AES);
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");// 创建密码器
            cipher.init(Cipher.DECRYPT_MODE, key);// 初始化
            byte[] obj = parseHexStr2Byte(content);
            byte[] bytes = cipher.doFinal(Objects.requireNonNull(obj));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将二进制转换成16进制
     */
    public static String parseByte2HexStr(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase(Locale.ENGLISH));
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    private static String aesKey;


}
