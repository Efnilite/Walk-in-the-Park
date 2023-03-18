package dev.efnilite.ip.style;

import dev.efnilite.ip.api.StyleType;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultStyleType extends StyleType {

    @Override
    public @NotNull String getName() {
        return "default";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return Locales.getItem(locale, "styles.default");
    }

    @Nullable
    @Override
    public Material get(String style) {
        List<Material> materials = styles.get(style);

        if (materials == null) {
            return null;
        }

        return materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
    }
}