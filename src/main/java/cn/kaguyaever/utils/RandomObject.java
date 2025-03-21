package cn.kaguyaever.utils;

import cn.kaguyaever.entity.Item;

import java.util.Random;

public class RandomObject {

    private static Random rand = new Random();

    public static Object getObject() {
        Item item = new Item();
        item.setId(rand.nextInt(1000) + 1);
        item.setItemName(generateRandomString(10, rand));
        item.setPrice(1.0 + (1000.0 - 1.0) * rand.nextDouble());
        item.setDelete(rand.nextBoolean());
        return item;
    }

    private static String generateRandomString(int length, Random random) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
}
