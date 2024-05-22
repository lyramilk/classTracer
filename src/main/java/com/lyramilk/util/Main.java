package com.lyramilk.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, IOException, NoSuchMethodException {
        String baseDir = "E:\\test\\";
        ClassTracer classTracer = new ClassTracer();
        classTracer.appendClassPath(baseDir + "myapk_64.apk");
        classTracer.appendClassPath(baseDir + "android-34.jar");
        classTracer.setCacheDir(baseDir + "cache");


        Class<?> clazz = classTracer.findClass("com.lyramilk.test.Test");
        classTracer.dumpToDir(baseDir + "dump");
        classTracer.dumpToJar(baseDir + "dump" + File.separator + "mydump.jar");
        Method[] methods = clazz.getMethods();
        System.out.println("Hello World! " + clazz.getPackageName() + " with " + methods.length + " methods");

    }
}