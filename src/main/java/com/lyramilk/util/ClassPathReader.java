package com.lyramilk.util;

import com.lyramilk.util.classpathreadimpl.*;

public abstract class ClassPathReader {
    public static ClassPathReader fromDir(String s, ClassTracer classTracer) {
        return new DirClassPath(s, classTracer);
    }

    public static ClassPathReader fromJar(String s, ClassTracer classTracer) {
        return new JarClassPath(s, classTracer);
    }

    public static ClassPathReader fromApk(String s, ClassTracer classTracer) {
        return new ApkClassPath(s, classTracer);
    }

    public static ClassPathReader fromZip(String s, ClassTracer classTracer) {
        return new ZipClassPath(s, classTracer);
    }

    public static ClassPathReader fromDex(String s, ClassTracer classTracer) {
        return new DexClassPath(s, classTracer);
    }

    public static ClassPathReader fromClass(String s, ClassTracer classTracer) {
        return new FileClassPath(s, classTracer);
    }

    public abstract boolean exists(String name);

    public abstract byte[] readAllBytes(String name) throws ClassNotFoundException;

    public abstract String getDisplayName(String name);
}
