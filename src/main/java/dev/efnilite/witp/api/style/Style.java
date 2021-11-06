package dev.efnilite.witp.api.style;

import org.bukkit.Material;

import java.util.List;

public abstract class Style {

    public List<Material> possible;

    public Style(List<Material> possible) {
        this.possible = possible;
    }

    public abstract Material get();

}
