# classTracer
## 功能
- ClassTracer是一个可以从apk/dex/class目录/class中查找类和依赖链的ClassLoader。并且可以将加载到的类提取出到目录或jar文件中。
- 主要是用于提取文件，不建议作为常规ClassLoader来用，因为它会在内存中额外缓存一部分不用于执行的信息。
- 适用于已知入口，从众多的类文件中精简出简单的依赖关系。
## 用法
```
package com.lyramilk.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, IOException, NoSuchMethodException {
        String baseDir = "E:\\test\\";
        ClassTracer classTracer = new ClassTracer();
        // 添加查找目标，支持 文件夹/class文件/jar文件/dex文件/apk文件
        classTracer.appendClassPath(baseDir + "myapk_64.apk");
        classTracer.appendClassPath(baseDir + "android-34.jar");
        // 设置缓存目录
        classTracer.setCacheDir(baseDir + "cache");

        // 像普通的ClassLoader一样查找java类
        Class<?> clazz = classTracer.findClass("com.lyramilk.test.Test");
        // 将查找过程中加载的java类dump到目录中，需要注意的是这个功能没有办法追踪到执行过程中加载的类。所以实际跑起来的时候还是会有class not found，到时候把找不到的类用上面的findclass再找一下后再次dump，循环这个步骤直到dump出来的文件可以跑通就ok了。
        classTracer.dumpToDir(baseDir + "dump");
        // 将查找过程中加载的java类dump成一个jar文件
        classTracer.dumpToJar(baseDir + "dump" + File.separator + "mydump.jar");
        Method[] methods = clazz.getMethods();
        System.out.println("Hello World! " + clazz.getPackageName() + " with " + methods.length + " methods");

    }
}
```
