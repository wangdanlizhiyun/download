package com.lzy.down;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

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

    public static void nioMappedCopy(File Copyfile, File newfile) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel inFiC = null;
        FileChannel outFoC = null;
        int length = 2097152;
        try {
            fis = new FileInputStream(Copyfile);
            fos = new FileOutputStream(newfile);
            inFiC = fis.getChannel();
            outFoC = fos.getChannel();
            long startTime = System.currentTimeMillis();
            while (inFiC.position() != inFiC.size()) {
                if ((inFiC.size() - inFiC.position()) < length) {
                    length = (int) (inFiC.size() - inFiC.position());
                }
                MappedByteBuffer inbuffer = inFiC.map(FileChannel.MapMode.READ_ONLY, inFiC.position(), length);
                outFoC.write(inbuffer);
                inFiC.position(inFiC.position() + length);
            }
            long EndTime = System.currentTimeMillis();
            System.out.print("nioMappedCopy用了毫秒数：");
            System.out.println(EndTime - startTime);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outFoC != null) {
                outFoC.close();
            }
            if (inFiC != null) {
                inFiC.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
    }

}