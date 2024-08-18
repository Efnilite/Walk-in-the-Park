package dev.efnilite.ip.style;

import org.bukkit.Material;

/**
 * Represents a parkour style.
 */
public interface Style {

    /**
     * The material to be used for the next block.
     * @return A material.
     */
    Material getNext();

    /**
     * The name of the style.
     * @return A name.
     */
    String getName();

}
