package com.lyramilk.util;

import java.security.NoSuchAlgorithmException;

public class Md5Util {
    public static String getMD5(byte[] fileContent) {
        try {
            byte[] md5Bytes = java.security.MessageDigest.getInstance("MD5").digest(fileContent);
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

}
