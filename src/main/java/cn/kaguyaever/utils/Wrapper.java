package cn.kaguyaever.utils;

import javassist.*;

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
        Map<String, Class<?>> pts = new HashMap<>(); // <property name, property types>
        Method[] methods = clazz.getMethods();
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
            wc = cc.toClass();
            wc.getField("pns").set(null, pts.keySet().toArray(new String[0]));
            wc.getField("pts").set(null, pts);
            return (Wrapper) wc.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract Object getPropertyValue(Object instance, String pn)
            throws NoSuchFieldException, IllegalArgumentException;

    public abstract String[] getPropertyNames();

    public abstract Class<?> getPropertyType(String pn);

    public abstract boolean hasProperty(String name);
}
