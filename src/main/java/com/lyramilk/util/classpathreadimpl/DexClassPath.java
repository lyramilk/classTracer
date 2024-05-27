package com.lyramilk.util.classpathreadimpl;

import com.googlecode.d2j.Method;
import com.googlecode.d2j.converter.IR2JConverter;
import com.googlecode.d2j.dex.*;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.d2j.reader.MultiDexFileReader;
import com.googlecode.dex2jar.ir.IrMethod;
import com.googlecode.dex2jar.ir.stmt.LabelStmt;
import com.googlecode.dex2jar.ir.stmt.Stmt;
import com.lyramilk.util.ClassPathReader;
import com.lyramilk.util.ClassTracer;
import com.lyramilk.util.Md5Util;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class DexClassPath extends ClassPathReader {
    private static final Logger logger = Logger.getLogger(ClassTracer.class);
    String dexFileName;
    String isolate;
    ClassTracer classTracer;
    DexExceptionHandler exceptionHandler = new DexExceptionHandler() {
        @Override
        public void handleFileException(Exception e) {

        }

        @Override
        public void handleMethodTranslateException(Method method, DexMethodNode dexMethodNode, MethodVisitor methodVisitor, Exception e) {

        }
    };
    boolean isInit = false;
    int readerConfig = DexFileReader.SKIP_DEBUG | DexFileReader.KEEP_CLINIT;
    int v3Config = V3.REUSE_REGISTER | V3.TOPOLOGICAL_SORT;

    public DexClassPath(String s, ClassTracer classTracer) {
        this.classTracer = classTracer;
        this.dexFileName = s;
    }

    void init() {

        byte[] fileContent = null;
        File f = new File(dexFileName);
        try {
            fileContent = Files.readAllBytes(f.toPath());
            String md5value = Md5Util.getMD5(fileContent);
            isolate = f.getName() + "." + md5value;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (classTracer.getCache().containsIsolate(isolate)) {
            isInit = true;
            return;
        }
        try {
            BaseDexFileReader reader = MultiDexFileReader.open(fileContent);
            DexFileNode fileNode = new DexFileNode();
            reader.accept(fileNode, readerConfig | DexFileReader.IGNORE_READ_EXCEPTION);

            ClassVisitorFactory cvf = new ClassVisitorFactory() {
                @Override
                public ClassVisitor create(final String name) {
                    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    final LambadaNameSafeClassAdapter rca = new LambadaNameSafeClassAdapter(cw, false);
                    return new ClassVisitor(Opcodes.ASM9, rca) {
                        @Override
                        public void visitEnd() {
                            super.visitEnd();
                            String className = rca.getClassName();
                            byte[] data;
                            try {
                                // FIXME handle 'java.lang.RuntimeException: Method code too large!'
                                data = cw.toByteArray();

                                String packageName = className.replace("/", ".");

                                classTracer.getCache().put(isolate, packageName, data);
                                logger.debug("parsing dex file:" + dexFileName + " ---> " + className);
                            } catch (Exception ex) {
                                logger.error("ASM fail to generate .class file: " + className);
                                return;
                            }

                        }
                    };
                }
            };


            (new ExDex2Asm(this.exceptionHandler) {
                public void convertCode(DexMethodNode methodNode, MethodVisitor mv, Dex2Asm.ClzCtx clzCtx) {
                    if ((readerConfig & DexFileReader.SKIP_CODE) != 0 && methodNode.method.getName().equals("<clinit>")) {
                        // also skip clinit
                        return;
                    }
                    super.convertCode(methodNode, mv, clzCtx);
                }

                @Override
                public void optimize(IrMethod irMethod) {
                    T_CLEAN_LABEL.transform(irMethod);
                    T_DEAD_CODE.transform(irMethod);
                    T_REMOVE_LOCAL.transform(irMethod);
                    T_REMOVE_CONST.transform(irMethod);
                    T_ZERO.transform(irMethod);
                    if (T_NPE.transformReportChanged(irMethod)) {
                        T_DEAD_CODE.transform(irMethod);
                        T_REMOVE_LOCAL.transform(irMethod);
                        T_REMOVE_CONST.transform(irMethod);
                    }

                    T_NEW.transform(irMethod);
                    T_FILL_ARRAY.transform(irMethod);
                    T_AGG.transform(irMethod);
                    T_MULTI_ARRAY.transform(irMethod);
                    T_VOID_INVOKE.transform(irMethod);
                    if (0 != (v3Config & V3.PRINT_IR)) {
                        int i = 0;
                        for (Stmt p : irMethod.stmts) {
                            if (p.st == Stmt.ST.LABEL) {
                                LabelStmt labelStmt = (LabelStmt) p;
                                labelStmt.displayName = "L" + i++;
                            }
                        }
                    }

                    T_DEAD_CODE.transform(irMethod);
                    T_REMOVE_LOCAL.transform(irMethod);
                    T_REMOVE_CONST.transform(irMethod);
                    T_TYPE.transform(irMethod);
                    T_UNSSA.transform(irMethod);
                    T_IR_2_J_REG_ASSIGN.transform(irMethod);
                    T_TRIM_EX.transform(irMethod);
                }

                @Override
                public void ir2j(IrMethod irMethod, MethodVisitor mv, ClzCtx clzCtx) {
                    new IR2JConverter()
                            .optimizeSynchronized(0 != (V3.OPTIMIZE_SYNCHRONIZED & v3Config))
                            .clzCtx(clzCtx)
                            .ir(irMethod)
                            .asm(mv)
                            .convert();
                }
            }).convertDex(fileNode, cvf);

            classTracer.getCache().putIsolate(isolate);
            isInit = true;
        } catch (IOException e) {
            isolate = null;
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
        return classTracer.getCache().containsKey(isolate, name);
    }

    @Override
    public byte[] readAllBytes(String name) throws ClassNotFoundException {
        if (!isInit) {
            init();
        }
        if (isolate == null) {
            throw new ClassNotFoundException("dex file fail:" + dexFileName);
        }
        if (classTracer.getCache().containsKey(isolate, name)) {
            try {
                return classTracer.getCache().get(isolate, name);
            } catch (IOException e) {
                throw new ClassNotFoundException("cache fail:" + dexFileName + "!" + name);
            }
        }
        throw new ClassNotFoundException("class not found in dex file:" + dexFileName + "!" + name);
    }

    @Override
    public String getDisplayName(String name) {
        return dexFileName + "!" + name;
    }
}
