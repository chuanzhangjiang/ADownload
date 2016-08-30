package me.zjc.androidlib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * MD5工具类
 * 用于计算MD5值
 */
public final class Md5Utils {
    private Md5Utils() {
        throw new IllegalAccessError();
    }

    public static String getFileMd5String(File file) {
        if (!file.isFile())
            throw new IllegalArgumentException("only file can calculate MD5 value!");

        MessageDigest digest;
        FileInputStream in = null;
        byte buffer[] = new byte[8192];
        int len;
        try {
            digest =MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
