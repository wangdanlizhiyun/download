package com.lzy.down;

import java.io.Closeable;
import java.net.HttpURLConnection;

public class Util {
    public static String getFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return new java.text.DecimalFormat("#.##").format(size / 1024.0f) + "KB";
        } else {
            return new java.text.DecimalFormat("#.##").format(size / (1024 * 1024.0f)) + "M";
        }
    }
    public static void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
        }
    }

    public static void close(HttpURLConnection c) {
        if (c != null) {
            c.disconnect();
        }
    }

    public static void close(HttpURLConnection conn, Closeable out, Closeable in) {
        close(conn);
        close(out);
        close(in);
    }

    public static void close(Closeable... closeables) {
        for (Closeable c : closeables
                ) {
            close(c);
        }
    }


}