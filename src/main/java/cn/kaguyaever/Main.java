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
            System.out.println("Object1." + field + "=" + wrapper.getPropertyValue(o1,field));
            System.out.println("Object2." + field + "=" + wrapper.getPropertyValue(o2,field));
        }
    }
}