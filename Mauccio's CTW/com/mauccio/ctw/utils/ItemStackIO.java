package com.mauccio.ctw.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class ItemStackIO {

    public static String toBase64(ItemStack[] items) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream out = new BukkitObjectOutputStream(byteStream);
        out.writeInt(items.length);
        for (ItemStack item : items) {
            out.writeObject(item);
        }
        out.close();
        return Base64.getEncoder().encodeToString(byteStream.toByteArray());
    }

    public static ItemStack[] fromBase64(String data) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        BukkitObjectInputStream in = new BukkitObjectInputStream(inputStream);
        int length = in.readInt();
        ItemStack[] items = new ItemStack[length];
        for (int i = 0; i < length; i++) {
            items[i] = (ItemStack) in.readObject();
        }
        in.close();
        return items;
    }
}
