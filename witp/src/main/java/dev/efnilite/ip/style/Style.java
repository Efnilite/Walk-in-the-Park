package dev.efnilite.ip.style;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Style {

    public final String name;
    public final List<Material> materials;
    public final String category;

    public Style(@NotNull String name, @NotNull List<Material> materials, @NotNull String category) {
        this.name = name;
        this.materials = materials;
        this.category = category;
    }
}
