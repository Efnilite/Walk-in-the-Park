package dev.efnilite.witp.schematic;

import dev.efnilite.witp.WITP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class SchematicAdjuster {

    /**
     * Pastes a Schematic, adjusted to Lime wool
     *
     * @param   schematic
     *          The schematic
     *
     * @param   adjustTo
     *          Which location
     *
     * @throws IOException if something goes wrong with pasting
     */
    public static @Nullable List<Block> pasteAdjusted(Schematic schematic, Location adjustTo) throws IOException {
        if (!schematic.hasFile() && adjustTo == null) {
            return null;
        }
        SchematicBlock start = schematic.findFromMaterial(Material.LIME_WOOL);
        Vector3D to = start.getRelativePosition();
        adjustTo = adjustTo.subtract(to.toBukkitVector());

        return schematic.pasteAdjusted(adjustTo, getAngle(WITP.getDivider().getHeading()));
    }

    /**
     * Gets the angle from a specific Vector heading
     *
     * @param   heading
     *          The vector heading
     *
     * @return the associated angle
     */
    private static RotationAngle getAngle(Vector3D heading) {
        if (heading.z != 0) { // north/south
            switch (heading.z) {
                case 1: // south
                    return RotationAngle.ANGLE_180;
                case -1: // north
                    return RotationAngle.ANGLE_0;
            }
        } else if (heading.x != 0) { // east/west
            switch (heading.x) {
                case 1: // east
                    return RotationAngle.ANGLE_270;
                case -1: // west
                    return RotationAngle.ANGLE_90;
            }
        }
        return RotationAngle.ANGLE_270;
    }
}