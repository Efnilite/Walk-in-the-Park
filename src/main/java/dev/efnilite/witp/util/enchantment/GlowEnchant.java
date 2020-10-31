package dev.efnilite.witp.util.enchantment;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.wrapper.EnchantmentWrapper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GlowEnchant extends EnchantmentWrapper {

    public GlowEnchant() {
        super("glow", WITP.getInstance());
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getStartLevel() {
        return 1;
    }

    @Override
    public @NotNull EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.ALL;
    }

    @Override
    public boolean conflictsWith(@NotNull Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean canEnchantItem(@NotNull ItemStack itemStack) {
        return true;
    }

    public static Enchantment getEnchantment() {
        return enchantment;
    }
}
