package dev.efnilite.ip.internal.style;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.StyleType;
import dev.efnilite.vilib.inventory.item.Item;
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
        return new Item(Material.POPPY, "<#348EDB><bold>Default").lore("<dark_gray>Standard • 默认 • 默認", "<dark_gray>• Défaut • デフォルト • Standaard");
    }

    @Override
    public Material get(String style) {
        List<Material> materials = styles.get(style);

        if (materials == null) {
            IP.logging().error("Materials for style '" + style + "' not found!");
            IP.logging().error("Check your config.yml file for invalid items.");
            return Material.STONE;
        }

        return materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
    }
}