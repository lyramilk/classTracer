package com.lyramilk.util.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileClassPathCache implements ClassPathCache {
    String cacheDir;

    public FileClassPathCache(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String pathOf(String isolate, String name) {
        return cacheDir + File.separator + isolate + File.separator + name.replace("/", File.separator);
    }

    public String pathOf(String isolate) {
        return cacheDir + File.separator + isolate;
    }


    private String packageNameToPathName(String isolate, String name) {
        return cacheDir + File.separator + isolate + File.separator + name.replace(".", File.separator) + ".class";
    }

    private String packageNameToIsolate(String isolate) {
        return cacheDir + File.separator + isolate + File.separator + ".ct";
    }

    @Override
    public boolean containsIsolate(String isolate) {
        String pathname = packageNameToIsolate(isolate);
        return Files.exists(Paths.get(pathname));
    }

    @Override
    public boolean putIsolate(String isolate) throws IOException {
        if (this.cacheDir == null) {
            throw new IOException("cacheDir is null");
        }
        String filepath = packageNameToIsolate(isolate);

        File f = new File(filepath);
        if (!f.exists()) {
            File parent = f.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            f.createNewFile();
        } else {
            if (f.delete()) {
                f.createNewFile();
            } else {
                throw new IOException("delete file failed:" + filepath);
            }
        }
        Date currentTime = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        String datetime = sdf.format(currentTime);
        Files.writeString(Paths.get(filepath), datetime);
        return true;
    }

    @Override
    public boolean containsKey(String isolate, String nameWithPackage) {
        String pathname = packageNameToPathName(isolate, nameWithPackage);
        return Files.exists(Paths.get(pathname));
    }

    @Override
    public void put(String isolate, String nameWithPackage, byte[] data) throws IOException {
        if (this.cacheDir == null) {
            throw new IOException("cacheDir is null");
        }
        String filepath = packageNameToPathName(isolate, nameWithPackage);

        File f = new File(filepath);
        if (!f.exists()) {
            File parent = f.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            f.createNewFile();
        } else {
            if (f.delete()) {
                f.createNewFile();
            } else {
                throw new IOException("delete file failed:" + filepath);
            }
        }
        Files.write(Paths.get(filepath), data);
    }

    public void putFile(String isolate, String relativePathName, byte[] data) throws IOException {
        if (this.cacheDir == null) {
            throw new IOException("cacheDir is null");
        }
        String filepath = pathOf(isolate, relativePathName);

        File f = new File(filepath);
        if (!f.exists()) {
            File parent = f.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            f.createNewFile();
        } else {
            if (f.delete()) {
                f.createNewFile();
            } else {
                throw new IOException("delete file failed:" + filepath);
            }
        }
        Files.write(Paths.get(filepath), data);
    }


    public String getFile(String isolate, String relativePathName) throws IOException {
        return pathOf(isolate, relativePathName);
    }

    @Override
    public byte[] get(String isolate, String nameWithPackage) throws IOException {
        String pathname = packageNameToPathName(isolate, nameWithPackage);
        try {
            return Files.readAllBytes(Paths.get(pathname));
        } catch (IOException e) {
            return null;
        }
    }
}
