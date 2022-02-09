package dev.efnilite.witp.internal.style;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.StyleType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultStyleType extends StyleType {

    @Override
    public @NotNull String getName() {
        return "default";
    }

    @Override
    public @NotNull ItemStack getItem(String locale) {
        return WITP.getConfiguration().getFromItemData(locale, "styles.default").build();
    }

    @Override
    public Material get(String style) {
        List<Material> materials = styles.get(style);
        return materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
    }
}