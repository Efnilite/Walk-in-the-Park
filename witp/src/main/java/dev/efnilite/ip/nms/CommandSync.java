package dev.efnilite.ip.nms;

import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Syncs commands or something
 */
public class CommandSync {
    private final Method syncCommands;

    public CommandSync() {
        try {
            syncCommands = getCBClass().getMethod("syncCommands");
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void sync() {
        if (syncCommands == null) {
            return;
        }

        try {
            syncCommands.invoke(Bukkit.getServer());
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Class<?> getCBClass() {
        try {
            return Class.forName("org.bukkit.craftbukkit." + getVersion() + ".CraftServer");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Couldn't find CraftBukkit class " + "CraftServer");
        }
    }

    private String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }

}
