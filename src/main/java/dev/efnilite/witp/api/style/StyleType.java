package dev.efnilite.witp.api.style;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Logging;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
     * The internal name of the style type. Can be anything.
     * Make sure it doesn't match another name.
     *
     * @return the internal name used for this type.
     */
    public abstract String getName();

    /**
     * Gets the item used in menus to show this style.
     *
     * @param   locale
     *          The locale of the menu, used to adjust the name.
     *
     * @return the item.
     */
    public abstract @NotNull ItemStack getItem(String locale);

    /**
     * Gets a random material from a style
     *
     * @param   style
     *          The style from which a material should be picked
     *
     * @return A random material
     */
    public abstract Material get(String style);

    /**
     * Adds a style from config
     *
     * @param   name
     *          The config name of this style
     */
    public void addConfigStyle(@NotNull String name) {
        styles.put(name.toLowerCase(), getPossibleMaterials(name));
    }

    /**
     * Adds a style without reading from config
     *
     * @param   name
     *          The name of this style
     *
     * @param   materials
     *          The materials belonging to this style
     */
    public void addStyle(@NotNull String name, @NotNull List<Material> materials) {
        styles.put(name.toLowerCase(), materials);
    }

    private @Nullable List<Material> getPossibleMaterials(String style) {
        if (style == null) {
            return null;
        }
        List<Material> materials = new ArrayList<>();
        String possible = WITP.getConfiguration().getFile("config").getString("styles.list." + style);
        if (possible == null) {
            Logging.warn("Style '" + style + "' doesn't exist in config.yml but is registered!");
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