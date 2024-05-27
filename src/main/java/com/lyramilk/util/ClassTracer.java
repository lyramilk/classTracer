package com.lyramilk.util;

import com.lyramilk.util.cache.ClassPathCache;
import com.lyramilk.util.cache.FileClassPathCache;
import com.lyramilk.util.cache.MemoryClassPathCache;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;


public class ClassTracer extends ClassLoader {
    private static final Logger logger = Logger.getLogger(ClassTracer.class);

    ClassPathCache cache = new MemoryClassPathCache();
    MemoryClassPathCache loaderCache = new MemoryClassPathCache();
    HashMap<String, String> loadedClassNames = new HashMap<>();
    HashMap<String, Class<?>> loadedClassObjects = new HashMap<>();

    List<ClassPathReader> classPathReaders = new ArrayList<>();
    // 追踪参数和返回值中引用到的类。
    Set<Class<?>> traceClasses = new HashSet<>();

    public ClassTracer() {
    }

    private static String outputDirByPackageName(String outputDir, String name) {
        return outputDir + File.separator + name.replace(".", File.separator) + ".class";
    }

    private static String outputJarPathByPackageName(String name) {
        return name.replace(".", "/") + ".class";
    }

    private void addClassToTraceSet(Class<?> clazz, Set<Class<?>> classes) {
        boolean r = false;
        Class<?> simpleClazz = clazz.isArray() ? clazz.getComponentType() : clazz;
        if (simpleClazz != null && classes.add(simpleClazz)) {
            try {
                findClass(simpleClazz.getName());
            } catch (ClassNotFoundException e) {
                //throw new RuntimeException(e);
                logger.warn("class not found:" + simpleClazz.getName());
            }
            if (simpleClazz.getName().startsWith("android.os")) {
                System.out.println(simpleClazz.getName());
            }
        }
    }

    public void setCacheDir(String cacheDir) {
        this.cache = new FileClassPathCache(cacheDir);
    }

    public ClassPathCache getCache() {
        return this.cache;
    }

    public void appendClassPath(String path) {
        File f = new File(path);
        if (f.isDirectory()) {
            classPathReaders.add(ClassPathReader.fromDir(path, this));
        } else if (f.exists()) {
            String expandedName = path.substring(path.lastIndexOf(".")).toLowerCase();
            if (expandedName.endsWith(".jar")) {
                classPathReaders.add(ClassPathReader.fromJar(path, this));
            }
            if (expandedName.endsWith(".apk")) {
                classPathReaders.add(ClassPathReader.fromApk(path, this));
            }

            if (expandedName.endsWith(".zip")) {
                classPathReaders.add(ClassPathReader.fromZip(path, this));
            }

            if (expandedName.endsWith(".dex")) {
                classPathReaders.add(ClassPathReader.fromDex(path, this));
            }

            if (expandedName.endsWith(".class")) {
                classPathReaders.add(ClassPathReader.fromClass(path, this));
            }
        }
    }

    public ClassPathReader loadClassDataFromClassPath(String name) {
        for (int i = 0; i < classPathReaders.size(); i++) {
            ClassPathReader f = classPathReaders.get(i);
            if (f.exists(name)) {
                return f;
            }
        }
        return null;
    }

    public boolean copyTo(String srcDisplayName, byte[] bytes, String dest) throws IOException {
        File f = new File(dest);
        f.getParentFile().mkdirs();
        //Files.copy(Paths.get(src), Paths.get(dest), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        if (f.exists()) {
            logger.info("file sync:" + srcDisplayName + " exists. skip !!!");
            return false;
        }
        if (f.createNewFile() && f.canWrite()) {
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(bytes);
            }
        }
        logger.info("file sync:" + srcDisplayName + " ---> " + dest);
        return true;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (loadedClassObjects.containsKey(name)) {
            return loadedClassObjects.get(name);
        }
        byte[] b = loadClassData(name);

        Class<?> r = null;
        try {
            r = Class.forName(name, true, this);
        } catch (ClassNotFoundException e) {
            r = defineClass(name, b, 0, b.length);
        } catch (ClassCircularityError e) {
            r = defineClass(name, b, 0, b.length);
        }

        if (r != null) {
            loadedClassObjects.put(name, findLoadedClass(name));
        }
        return r;
    }

    public void trace() throws ClassNotFoundException {
        for (Class<?> t : loadedClassObjects.values()) {
            addClassToTraceSet(t, traceClasses);
        }

        int sizeOfClasses = traceClasses.size();
        int lastSizeOfClasses = 0;
        while (sizeOfClasses > lastSizeOfClasses) {
            lastSizeOfClasses = sizeOfClasses;
            Set<Class<?>> newClasses = new HashSet<>();

            for (Class<?> c : traceClasses.toArray(new Class<?>[0])) {
                trace0(c, newClasses);
            }

            for (Class<?> c : newClasses) {
                addClassToTraceSet(c, traceClasses);
            }
            sizeOfClasses = traceClasses.size();
        }
    }

    public void trace(Class<?> clazz) throws ClassNotFoundException {
        if (clazz == null) {
            return;
        }

        int sizeOfClasses = traceClasses.size();
        int lastSizeOfClasses = 0;
        while (sizeOfClasses > lastSizeOfClasses) {
            lastSizeOfClasses = sizeOfClasses;
            Set<Class<?>> newClasses = new HashSet<>();

            for (Class<?> c : traceClasses.toArray(new Class<?>[0])) {
                trace0(c, newClasses);
            }

            for (Class<?> c : newClasses) {
                addClassToTraceSet(c, traceClasses);
            }
            sizeOfClasses = traceClasses.size();
        }
    }

    private void trace0(Class<?> clazz, Set<Class<?>> classes) throws ClassNotFoundException {
        addClassToTraceSet(clazz, classes);
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            for (Class<?> c : method.getParameterTypes()) {
                addClassToTraceSet(c, classes);
            }
            addClassToTraceSet(method.getReturnType(), classes);
        }
        for (Class<?> c : clazz.getClasses()) {
            addClassToTraceSet(c, classes);
        }

        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            Class<?> t = field.getType();
            addClassToTraceSet(t, classes);
        }
    }

    private byte[] loadClassData(String name) throws ClassNotFoundException {
        ClassPathReader classPathReader = loadClassDataFromClassPath(name);
        if (classPathReader == null) {
            throw new ClassNotFoundException("class not found:" + name);
        }
        String displayName = classPathReader.getDisplayName(name);
        loadedClassNames.put(name, displayName);
        System.out.println("load class:" + displayName + "," + loadedClassNames.size());
        loaderCache.put("loader", name, classPathReader.readAllBytes(name));

        byte[] result = classPathReader.readAllBytes(name);
        return classPathReader.readAllBytes(name);
    }

    public boolean dumpToDir(String path) throws ClassNotFoundException {
        loaderCache.putIsolate("loader");
        trace();
        System.out.println("dump to dir:" + path + "," + loadedClassNames.size());
        for (Map.Entry<String, String> entry : loadedClassNames.entrySet()) {
            String name = entry.getKey();
            String filename = outputDirByPackageName(path, name);
            String srcDisplayName = entry.getValue();
            byte[] classBytes = null;
            try {
                classBytes = loaderCache.get("loader", name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (classBytes == null) {
                logger.error("Error reading file: " + filename);
                return false;
            }
            try {

                copyTo(srcDisplayName, classBytes, filename);
            } catch (IOException e) {
                logger.error("Error writing file: " + filename + " " + e.getMessage());
            }
        }
        return true;
    }

    public boolean dumpToJar(String jarFileName) throws IOException, ClassNotFoundException {
        return dumpToJar(jarFileName, null);
    }

    public boolean dumpToJar(String jarFileName, String mainClass) throws IOException, ClassNotFoundException {
        trace();

        Manifest manifest = new Manifest();
        if (mainClass != null) {
            manifest.getMainAttributes().putValue("Main-Class", mainClass);
        }
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFileName), manifest);
        for (Map.Entry<String, String> entry : loadedClassNames.entrySet()) {
            String name = entry.getKey();
            String jarpath = outputJarPathByPackageName(name);
            String srcDisplayName = entry.getValue();
            byte[] classBytes = loaderCache.get("loader", name);
            if (classBytes == null) {
                logger.error("Error reading file: " + jarpath);
                return false;
            }
            try {

                JarEntry jarEntry = new JarEntry(jarpath);
                jarEntry.setTime(System.currentTimeMillis());
                jarOutputStream.putNextEntry(jarEntry);
                jarOutputStream.write(classBytes, 0, classBytes.length);
                jarOutputStream.closeEntry();
                logger.info("jar sync:" + srcDisplayName + " ---> " + jarFileName + "!" + jarpath);
            } catch (IOException e) {
                logger.error("Error writing file: " + jarpath + " " + e.getMessage());
            }
        }
        jarOutputStream.close();
        return true;
    }


    public boolean dumpToJar(JarOutputStream jarOutputStream) throws IOException, ClassNotFoundException {
        trace();

        for (Map.Entry<String, String> entry : loadedClassNames.entrySet()) {
            String name = entry.getKey();
            String jarpath = outputJarPathByPackageName(name);
            String srcDisplayName = entry.getValue();
            byte[] classBytes = loaderCache.get("loader", name);
            if (classBytes == null) {
                logger.error("Error reading file: " + jarpath);
                return false;
            }
            try {

                JarEntry jarEntry = new JarEntry(jarpath);
                jarEntry.setTime(System.currentTimeMillis());
                jarOutputStream.putNextEntry(jarEntry);
                jarOutputStream.write(classBytes, 0, classBytes.length);
                jarOutputStream.closeEntry();
                logger.info("jar append:" + jarpath + " <--- " + srcDisplayName);
            } catch (IOException e) {
                logger.error("Error writing file: " + jarpath + " " + e.getMessage());
            }
        }
        return true;
    }

}
