package com.lyramilk.util.classpathreadimpl;

import com.lyramilk.util.ClassPathReader;
import com.lyramilk.util.ClassTracer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.jar.JarFile;

public class JarClassPath extends ClassPathReader {
    private static final Logger logger = Logger.getLogger(ClassTracer.class);

    JarFile jarFile;
    String jarFileName;

    public JarClassPath(String filename, ClassTracer classTracer) {
        try {
            jarFile = new JarFile(filename);
            jarFileName = filename;
        } catch (IOException e) {
            logger.warn("Error opening jar file: " + filename + " " + e.getMessage());
            jarFile = null;
            jarFileName = null;
        }
    }

    public String packageNameToJarClassFile(String name) {
        return name.replace(".", "/") + ".class";
    }

    @Override
    public boolean exists(String name) {
        if (jarFile != null) {
            String classInJar = packageNameToJarClassFile(name);
            boolean result = jarFile.getJarEntry(classInJar) != null;
            return result;
        }
        return false;
    }

    @Override
    public byte[] readAllBytes(String name) throws ClassNotFoundException {
        if (jarFile != null) {
            try {
                String classInJar = packageNameToJarClassFile(name);
                return jarFile.getInputStream(jarFile.getJarEntry(classInJar)).readAllBytes();
            } catch (IOException e) {
                logger.error("Error reading jar file: " + jarFileName + " " + e.getMessage());
            }
        }
        logger.error("Error reading jar file: " + jarFileName);
        throw new ClassNotFoundException("class not found in jar file: " + jarFileName + "!" + name);
    }

    @Override
    public String getDisplayName(String name) {
        return jarFileName + "!" + name;
    }
}
