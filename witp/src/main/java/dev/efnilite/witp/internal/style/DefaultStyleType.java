package dev.efnilite.witp.internal.style;

import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.witp.IP;
import dev.efnilite.witp.api.StyleType;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultStyleType extends StyleType {

    @Override
    public @NotNull String getName() {
        return "default";
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return IP.getConfiguration().getFromItemData(locale, "styles.default");
    }

    @Override
    public Material get(String style) {
        List<Material> materials = styles.get(style);
        return materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
    }
}