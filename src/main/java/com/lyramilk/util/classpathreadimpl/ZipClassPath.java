package com.lyramilk.util.classpathreadimpl;

import com.lyramilk.util.ClassPathReader;
import com.lyramilk.util.ClassTracer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.zip.ZipFile;

public class ZipClassPath extends ClassPathReader {
    private static final Logger logger = Logger.getLogger(ClassTracer.class);

    ZipFile zipFile;
    String zipFileName;

    public ZipClassPath(String filename, ClassTracer classTracer) {
        try {
            zipFile = new ZipFile(filename);
            zipFileName = filename;
        } catch (IOException e) {
            logger.warn("Error opening zip file: " + filename + " " + e.getMessage());
            zipFile = null;
            zipFileName = null;
        }
    }

    public String packageNameToZipClassFile(String name) {
        return name.replace(".", "/") + ".class";
    }

    @Override
    public boolean exists(String name) {
        if (zipFile != null) {
            String classInZip = packageNameToZipClassFile(name);
            return zipFile.getEntry(classInZip) != null;
        }
        return false;
    }

    @Override
    public byte[] readAllBytes(String name) throws ClassNotFoundException {
        if (zipFile != null) {
            try {
                String classInZip = packageNameToZipClassFile(name);
                return zipFile.getInputStream(zipFile.getEntry(classInZip)).readAllBytes();
            } catch (IOException e) {
                logger.error("Error reading zip file: " + zipFileName + " " + e.getMessage());
            }
        }
        logger.error("Error reading zip file: " + zipFileName);
        throw new ClassNotFoundException("class not found in zip file: " + zipFileName + "!" + name);
    }

    @Override
    public String getDisplayName(String name) {
        return zipFileName + "!" + name;
    }
}
