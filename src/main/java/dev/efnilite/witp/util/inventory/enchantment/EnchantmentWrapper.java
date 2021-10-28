package dev.efnilite.witp.util.inventory.enchantment;

import dev.efnilite.witp.util.Verbose;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Wrapper for enchantments
 */
public abstract class EnchantmentWrapper extends Enchantment {

    protected String name;
    protected static Enchantment enchantment;

    public EnchantmentWrapper(String name, Plugin plugin) {
        super(new NamespacedKey(plugin, name));
        this.name = name;

        enchantment = this;

        register();
    }

    public void register() {
        if (!Enchantment.isAcceptingRegistrations()) {
            try {
                Field field = Enchantment.class.getDeclaredField("acceptingNew");
                field.setAccessible(true);
                field.set(null, true);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                Verbose.error("Couldn't init Enchantment");
                return;
            }

        }
        Enchantment.registerEnchantment(this);
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public boolean isCursed() {
        return false;
    }
}
