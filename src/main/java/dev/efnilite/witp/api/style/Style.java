package dev.efnilite.witp.api.style;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class Style {

    public List<Material> possible;

    public Style(@NotNull List<Material> possible) {
        this.possible = possible;
    }

    public abstract Material get();

}
