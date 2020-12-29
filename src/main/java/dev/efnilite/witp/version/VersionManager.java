package dev.efnilite.witp.version;

import dev.efnilite.witp.generator.ParkourGenerator;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;

/**
 * Interface for version-specific dealings
 *
 * @author Efnilite
 */
public interface VersionManager {

    /**
     * Places a schematic
     *
     * @param   file
     *          The file
     *
     * @param   to
     *          The location
     *
     * @return Structure data, including the blocks of the structure and where it ends
     */
    ParkourGenerator.StructureData placeAt(File file, Location to);

    void pasteStructure(File file, Location to);

    /**
     * Gets the dimensions of a structure file
     *
     * @param   file
     *          The file
     *
     * @param   to
     *          A location (which is somehow required to determine the sizes). It is recommended you put the coords of
     *          where you want to paste this structure.
     *
     * @return  the 3D dimensions (all positive)
     */
    Vector getDimensions(File file, Location to);

    /**
     * Sets the world border for a player using NMS packets.
     *
     * @param   player
     *          The player
     *
     * @param   vector
     *          The x and z value of this vector will be used to determine the center of the world border, so
     *          (100, 50, 100) will put the center of the world border at (100, 100)
     *
     * @param   size
     *          The length of one of the sizes of the world border (the playable area will be size/2 in all directions)
     */
    void setWorldBorder(Player player, Vector vector, double size);

}