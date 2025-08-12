package cn.kaguyaever.utils;

import javassist.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Wrapper {
    private static ConcurrentMap<Class<?>, Wrapper> WRAPPER_MAP = new ConcurrentHashMap<>();
    private static String METHOD_PREFIX = "get";

    public static Wrapper getWrapper(Class<?> clazz) {
        return WRAPPER_MAP.computeIfAbsent(clazz, Wrapper::makeWrapper);
    }

    private static Wrapper makeWrapper(Class<?> clazz) {
        String name = clazz.getName();
        StringBuilder getProperty = new StringBuilder("public Object getPropertyValue(Object o, String n){ ")
                .append(name).append(" w; try{ w = ((").append(name).append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");
        StringBuilder invokeMethod = new StringBuilder("public Object invokeMethod(Object o, String n, Class[] p, Object[] v) throws "
                + InvocationTargetException.class.getName() + "{ ").append(name).append(" w; try{ w = ((")
                .append(name).append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");
        Map<String, Class<?>> pts = new HashMap<>(); // <property name, property types>
        Method[] methods = clazz.getMethods();
        if (hasMethods(methods)) {
            invokeMethod.append(" try{");
            for (Method method : methods) {
                String methodName = method.getName();
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }
                invokeMethod.append(" if( \"").append(methodName).append("\".equals( $2 ) ");
                int len = method.getParameterTypes().length;
                invokeMethod.append(" && ").append(" $3.length == ").append(len).append(" ) { ");
                if (method.getReturnType() == Void.TYPE) {
                    invokeMethod.append(" w.")
                            .append(methodName)
                            .append('(')
                            .append(args(method.getParameterTypes()))
                            .append(");")
                            .append(" return null;");
                } else {
                    invokeMethod.append(" return ($w)w.")
                            .append(methodName)
                            .append('(')
                            .append(args(method.getParameterTypes()))
                            .append(");");
                }
                invokeMethod.append(" }");
            }
            invokeMethod.append(" } catch(Throwable e) { throw new java.lang.reflect.InvocationTargetException(e); }");
        }
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith(METHOD_PREFIX)) {
                if ("getClass".equals(methodName)) {
                    continue;
                }
                if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                    String remaining = methodName.substring(METHOD_PREFIX.length());
                    String propertyName = Character.toLowerCase(remaining.charAt(0)) + remaining.substring(1);
                    getProperty.append(" if( $2.equals(\"")
                            .append(propertyName)
                            .append("\") ){ return ")
                            .append("w.")
                            .append(methodName)
                            .append("(); }");
                    pts.put(propertyName, method.getReturnType());
                }
            }
        }
        invokeMethod.append(" throw new ")
                .append(NoSuchMethodException.class.getName())
                .append("(\"Not found method \\\"\"+$2+\"\\\" in class ")
                .append(name)
                .append(".\"); }");
        getProperty.append(" throw new ")
                .append(NoSuchFieldException.class.getName())
                .append("(\"Not found property \\\"\"+$2+\"\\\" field or getter method in class ")
                .append(name)
                .append(".\"); }");
        ClassPool pool = ClassPool.getDefault();
        CtClass cc;
        Class<?> wc;
        try {
            cc = pool.makeClass(name + "Wrapper");
            cc.setSuperclass(pool.get(Wrapper.class.getName()));
            cc.addField(CtField.make("public static String[] pns;", cc)); // property name array.
            cc.addField(CtField.make("public static " + Map.class.getName() + " pts;", cc)); // property type map.
            cc.addConstructor(CtNewConstructor.defaultConstructor(cc));
            cc.addMethod(CtMethod.make("public String[] getPropertyNames(){ return pns; }", cc));
            cc.addMethod(CtMethod.make("public boolean hasProperty(String n){ return pts.containsKey($1); }", cc));
            cc.addMethod(CtMethod.make("public Class getPropertyType(String n){ return (Class)pts.get($1); }", cc));
            cc.addMethod(CtMethod.make(getProperty.toString(), cc));
            cc.addMethod(CtMethod.make(invokeMethod.toString(), cc));
            wc = cc.toClass();
            wc.getField("pns").set(null, pts.keySet().toArray(new String[0]));
            wc.getField("pts").set(null, pts);
            byte[] bytecode = cc.toBytecode();
            saveBytecodeToFile(name, bytecode);
            return (Wrapper) wc.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveBytecodeToFile(String className, byte[] bytecode) throws IOException {
        String fileName = className.replace('.', '/') + ".class";
        File outputFile = new File("src/main/java", fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(bytecode);
        }
    }

    public static boolean hasMethods(Method[] methods) {
        if (methods == null) {
            return false;
        }
        for (Method m : methods) {
            if (m.getDeclaringClass() != Object.class) {
                return true;
            }
        }
        return false;
    }

    private static String args(Class<?>[] cs) {
        int len = cs.length;
        if (len == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(arg(cs[i], "$4" + "[" + i + "]"));
        }
        return sb.toString();
    }

    private static String arg(Class<?> cl, String name) {
        if (cl.isPrimitive()) {
            if (cl == Boolean.TYPE) {
                return "((Boolean)" + name + ").booleanValue()";
            }
            if (cl == Byte.TYPE) {
                return "((Byte)" + name + ").byteValue()";
            }
            if (cl == Character.TYPE) {
                return "((Character)" + name + ").charValue()";
            }
            if (cl == Double.TYPE) {
                return "((Number)" + name + ").doubleValue()";
            }
            if (cl == Float.TYPE) {
                return "((Number)" + name + ").floatValue()";
            }
            if (cl == Integer.TYPE) {
                return "((Number)" + name + ").intValue()";
            }
            if (cl == Long.TYPE) {
                return "((Number)" + name + ").longValue()";
            }
            if (cl == Short.TYPE) {
                return "((Number)" + name + ").shortValue()";
            }
            throw new RuntimeException("Unknown primitive type: " + cl.getName());
        }
        return "(" + getName(cl) + ")" + name;
    }

    public static String getName(Class<?> c) {
        if (c.isArray()) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append("[]");
                c = c.getComponentType();
            } while (c.isArray());

            return c.getName() + sb;
        }
        return c.getName();
    }

    public abstract Object getPropertyValue(Object instance, String pn)
            throws NoSuchFieldException, IllegalArgumentException;

    public abstract String[] getPropertyNames();

    public abstract Class<?> getPropertyType(String pn);

    public abstract boolean hasProperty(String name);

    public abstract Object invokeMethod(Object instance, String mn, Class<?>[] types, Object[] args)
            throws NoSuchMethodException, InvocationTargetException;
}
