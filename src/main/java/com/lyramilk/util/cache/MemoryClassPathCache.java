package com.lyramilk.util.cache;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MemoryClassPathCache implements ClassPathCache {
    Map<String, byte[]> classBytes = new LinkedHashMap<>();
    Set<String> isolates = new HashSet<>();

    @Override
    public boolean containsIsolate(String isolate) {
        return isolates.contains(isolate);
    }

    @Override
    public boolean putIsolate(String isolate) {
        isolates.add(isolate);
        return true;
    }

    @Override
    public boolean containsKey(String isolate, String nameWithPackage) {
        String key = isolate + ":" + nameWithPackage;
        return classBytes.containsKey(nameWithPackage);
    }

    @Override
    public void put(String isolate, String nameWithPackage, byte[] data) {
        String key = isolate + ":" + nameWithPackage;
        classBytes.put(key, data);

    }

    @Override
    public byte[] get(String isolate, String nameWithPackage) throws IOException {
        if (!isolates.contains(isolate)) {
            return null;
        }
        String key = isolate + ":" + nameWithPackage;
        return classBytes.get(key);
    }
}
