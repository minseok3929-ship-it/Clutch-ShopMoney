package com.clutch.shopmoney.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class SerializationUtil {
    private SerializationUtil() {}

    public static String itemToBase64(ItemStack item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out)) {
            dataOut.writeObject(item);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ItemStack itemFromBase64(String base64) {
        byte[] raw = Base64.getDecoder().decode(base64);
        try (ByteArrayInputStream in = new ByteArrayInputStream(raw); BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in)) {
            Object obj = dataIn.readObject();
            return obj instanceof ItemStack item ? item : null;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
