package com.lyramilk.util.classpathreadimpl;

import com.lyramilk.util.ClassPathReader;
import com.lyramilk.util.ClassTracer;
import com.lyramilk.util.Md5Util;
import com.lyramilk.util.cache.ClassPathCache;
import com.lyramilk.util.cache.FileClassPathCache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

interface FileCallback {
    void callback(File file);
}

public class ApkClassPath extends ClassPathReader {
    boolean isInit = false;
    String apkFileName;
    String isolate;
    ClassTracer classTracer;

    List<ClassPathReader> list = new ArrayList<>();

    public ApkClassPath(String filename, ClassTracer classTracer) {
        apkFileName = filename;
        this.classTracer = classTracer;

    }

    public static void directLookup(File dir, FileCallback cbk) {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                directLookup(f, cbk);
            } else {
                cbk.callback(f);
            }
        }
    }

    void init() {
        isInit = true;

        try {
            byte[] fileContent = null;
            File f = new File(apkFileName);
            fileContent = Files.readAllBytes(f.toPath());
            String md5value = Md5Util.getMD5(fileContent);
            isolate = f.getName() + "." + md5value;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClassPathCache cacheUnknow = classTracer.getCache();
        if (!(cacheUnknow instanceof FileClassPathCache)) {
            throw new RuntimeException("parse apk class path must set file cache");
        }
        FileClassPathCache cache = (FileClassPathCache) cacheUnknow;


        if (!classTracer.getCache().containsIsolate(isolate)) {
            try (ZipFile zipFile = new ZipFile(apkFileName)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    try {
                        byte[] bytes = zipFile.getInputStream(entry).readAllBytes();

                        int expindex = name.lastIndexOf(".");
                        if (expindex == -1) continue;

                        String expandedName = name.substring(expindex).toLowerCase();
                        if (expandedName.endsWith(".jar")) {
                            String subPath = cache.getFile(isolate, name);
                            cache.putFile(isolate, name, bytes);
                            //classTracer.appendClassPath(subPath);
                        } else if (expandedName.endsWith(".class")) {
                            String subPath = cache.getFile(isolate, name);
                            cache.putFile(isolate, name, bytes);
                            //classTracer.appendClassPath(subPath);
                        } else if (expandedName.endsWith(".dex")) {
                            String subPath = cache.getFile(isolate, name);
                            cache.putFile(isolate, name, bytes);
                            //classTracer.appendClassPath(subPath);
                        } else {
                            // cache.putFile(isolate, name, bytes); 没这个必要
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                isolate = null;
            }
            if (isolate != null) {
                try {
                    classTracer.getCache().putIsolate(isolate);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (isolate != null) {
            String filename = cache.pathOf(isolate);
            File f = new File(filename);
            directLookup(f, new FileCallback() {
                @Override
                public void callback(File file) {
                    String name = file.getName();
                    int expindex = name.lastIndexOf(".");
                    if (expindex == -1) return;

                    String expandedName = name.substring(expindex).toLowerCase();
                    if (expandedName.endsWith(".jar")) {
                        classTracer.appendClassPath(file.getAbsolutePath());
                    } else if (expandedName.endsWith(".class")) {
                        classTracer.appendClassPath(file.getAbsolutePath());
                    } else if (expandedName.endsWith(".dex")) {
                        classTracer.appendClassPath(file.getAbsolutePath());
                    }
                }
            });
        }

    }

    @Override
    public boolean exists(String name) {
        if (!isInit) {
            init();
        }
        if (isolate == null) {
            return false;
        }
        return false;
    }

    @Override
    public byte[] readAllBytes(String name) throws ClassNotFoundException {
        if (!isInit) {
            init();
        }
        if (isolate == null) {
            throw new ClassNotFoundException("apk file fail:" + apkFileName);
        }
        return new byte[0];
    }

    @Override
    public String getDisplayName(String name) {
        return "";
    }
}
