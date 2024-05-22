package com.lyramilk.util.classpathreadimpl;

import com.lyramilk.util.ClassPathReader;
import com.lyramilk.util.ClassTracer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DirClassPath extends ClassPathReader {
    String path;

    public DirClassPath(String filename, ClassTracer classTracer) {
        path = filename;
    }

    public String packageNameToPathName(String name) {
        return path + File.separator + name.replace(".", File.separator) + ".class";
    }

    @Override
    public boolean exists(String name) {
        String pathname = packageNameToPathName(name);
        File f = new File(pathname);
        return f.exists();
    }

    @Override
    public byte[] readAllBytes(String name) {
        String pathname = packageNameToPathName(name);
        try {
            return Files.readAllBytes(Paths.get(pathname));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getDisplayName(String name) {
        return packageNameToPathName(name);
    }
}
