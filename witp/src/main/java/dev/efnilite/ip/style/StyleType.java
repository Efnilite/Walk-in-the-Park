package dev.efnilite.ip.style;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class for style types
 */
public abstract class StyleType {

    /**
     * The registered styles under this type, along with the possible materials they feature
     */
    public HashMap<String, List<Material>> styles;

    /**
     * Constructor for this type
     */
    public StyleType() {
        this.styles = new HashMap<>();
    }

    /**
     * @return The internal name used for this type. Must be unique.
     */
    public abstract String getName();

    /**
     * @param locale The locale of the menu, used to adjust the name.
     * @return The item used in menus to show this style.
     */
    public abstract @NotNull Item getItem(String locale);

    /**
     * @param style The style from which a material should be picked
     * @return A random material from a style
     */
    @Nullable
    public abstract Material get(String style);

    /**
     * Reads a specific file at a specific path to get all the styles.
     *
     * @param path The path at which the styles are in the config
     * @param file The file to read from
     */
    public void addConfigStyles(@NotNull String path, @NotNull FileConfiguration file) {
        List<String> list = Util.getChildren(file, path, false);

        if (list.isEmpty()) {
            IP.logging().error("Style path " + path + " not found");
            return;
        }

        for (String style : list) { // get all styles in the path
            styles.put(style, getPossibleMaterials(style, path, file));
        }
    }

    /**
     * Adds a style without reading from config
     *
     * @param name      The name of this style
     * @param materials The materials belonging to this style
     */
    public void addStyle(@NotNull String name, @NotNull List<Material> materials) {
        styles.put(name.toLowerCase(), materials);
    }

    /**
     * Gets a map of all the registered styles under this style type
     *
     * @return the style type
     */
    public HashMap<String, List<Material>> getStyles() {
        return styles;
    }

    @Nullable
    private List<Material> getPossibleMaterials(@NotNull String name, @NotNull String path, @NotNull FileConfiguration file) {
        List<Material> materials = new ArrayList<>();
        String possible = file.getString(path + "." + name);

        if (possible == null) {
            IP.logging().warn("Style at path " + path + " doesn't exist but is registered!");
            return null;
        }

        for (String material : possible.replaceAll("[\\[\\]]", "").split(", ")) {
            Material mat = Material.getMaterial(material.toUpperCase());
            if (mat == null) {
                continue;
            }
            materials.add(mat);
        }

        return materials;
    }
}