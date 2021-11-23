package dev.efnilite.witp.api.style;

import dev.efnilite.witp.WITP;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class DefaultStyleType extends StyleType {

    @Override
    public @NotNull String getName() {
        return "default";
    }

    @Override
    public @NotNull ItemStack getItem(String locale) {
        return WITP.getConfiguration().getFromItemData(locale, "styles.default");
    }

    @Override
    public Material get(String style) {
        return styles.get(style).get(ThreadLocalRandom.current().nextInt(styles.size()));
    }
}