package dev.efnilite.witp.schematic;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.io.IOException;

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
    public static void pasteAdjusted(Schematic schematic, Location adjustTo) throws IOException {
        if (!schematic.hasFile() && adjustTo == null) {
            return;
        }
        SchematicBlock start = schematic.findFromMaterial(Material.LIME_WOOL);
        Vector to = start.getRelativePosition();
        adjustTo = adjustTo.subtract(to);

        schematic.paste(adjustTo, Schematic.RotationAngle.ANGLE_0);
        schematic.paste(adjustTo, Schematic.RotationAngle.ANGLE_90);
        schematic.paste(adjustTo, Schematic.RotationAngle.ANGLE_180);
        schematic.paste(adjustTo, Schematic.RotationAngle.ANGLE_270);
//        schematic.paste(adjustTo, getAngle(WITP.getDivider().getHeading())); // already rotated
    }

    /**
     * Gets the angle from a specific Vector heading
     *
     * @param   heading
     *          The vector heading
     *
     * @return the associated angle
     */
    private static Schematic.RotationAngle getAngle(Vector heading) {
        if (heading.getBlockZ() != 0) { // north/south
            switch (heading.getBlockZ()) {
                case 1: // south
                    return Schematic.RotationAngle.ANGLE_180;
                case -1: // north
                    return Schematic.RotationAngle.ANGLE_0;
            }
        } else if (heading.getBlockX() != 0) { // east/west
            switch (heading.getBlockX()) {
                case 1: // east
                    return Schematic.RotationAngle.ANGLE_90;
                case -1: // west
                    return Schematic.RotationAngle.ANGLE_270;
            }
        }
        return Schematic.RotationAngle.ANGLE_0;
    }
}