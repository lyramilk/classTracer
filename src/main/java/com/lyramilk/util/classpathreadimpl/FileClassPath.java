package com.lyramilk.util.classpathreadimpl;

import com.lyramilk.util.ClassPathReader;
import com.lyramilk.util.ClassTracer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileClassPath extends ClassPathReader {
    String filename;

    public FileClassPath(String filename, ClassTracer classTracer) {
        this.filename = filename;
    }

    @Override
    public boolean exists(String name) {
        File f = new File(name);
        return f.exists();
    }

    @Override
    public byte[] readAllBytes(String name) throws ClassNotFoundException {
        try (FileInputStream fileInputStream = new FileInputStream(filename)) {
            return fileInputStream.readAllBytes();
        } catch (IOException e) {
            throw new ClassNotFoundException("class not found in file: " + filename);
        }
    }

    @Override
    public String getDisplayName(String name) {
        return filename;
    }
}
