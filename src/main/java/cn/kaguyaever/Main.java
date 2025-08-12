package cn.kaguyaever;

import cn.kaguyaever.utils.RandomObject;
import cn.kaguyaever.utils.Wrapper;

public class Main {
    public static void main(String[] args) throws NoSuchFieldException {
        Object o1 = RandomObject.getObject();
        Object o2 = RandomObject.getObject();
        Wrapper wrapper = Wrapper.getWrapper(o1.getClass());
        String[] fields = wrapper.getPropertyNames();
        for (String field : fields) {
            System.out.println("Object1." + field + "=" + wrapper.getPropertyValue(o1, field));
            System.out.println("Object2." + field + "=" + wrapper.getPropertyValue(o2, field));
        }
        try {
            System.out.println("Object1.idAndName=" + wrapper.invokeMethod(o1, "idAndName", new Class[0], new Object[0]));
            System.out.println("Object1.itemPrice=" + wrapper.invokeMethod(o1, "itemPrice", new Class[]{Integer.class}, new Object[]{3}));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}