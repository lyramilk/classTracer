package com.lyramilk.util.cache;

import java.io.IOException;

public interface ClassPathCache {

    boolean containsIsolate(String isolate);

    boolean putIsolate(String isolate) throws IOException;

    boolean containsKey(String isolate, String nameWithPackage);

    void put(String isolate, String nameWithPackage, byte[] data) throws IOException;

    byte[] get(String isolate, String nameWithPackage) throws IOException;
}
