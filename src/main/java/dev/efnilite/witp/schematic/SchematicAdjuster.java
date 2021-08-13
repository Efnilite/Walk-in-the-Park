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
    public static void pasteAdjusted(Schematic schematic, Location adjustTo, Vector3D.RotationAngle angle) throws IOException {
        if (!schematic.hasFile() && adjustTo == null) {
            return;
        }
        SchematicBlock start = schematic.findFromMaterial(Material.LIME_WOOL);
        Vector3D to = start.getRelativePosition();
        adjustTo = adjustTo.subtract(to.toBukkitVector());

        schematic.pasteAdjusted(adjustTo, angle);
    }

    /**
     * Gets the angle from a specific Vector heading
     *
     * @param   heading
     *          The vector heading
     *
     * @return the associated angle
     */
    private static Vector3D.RotationAngle getAngle(Vector heading) {
        if (heading.getBlockZ() != 0) { // north/south
            switch (heading.getBlockZ()) {
                case 1: // south
                    return Vector3D.RotationAngle.ANGLE_180;
                case -1: // north
                    return Vector3D.RotationAngle.ANGLE_0;
            }
        } else if (heading.getBlockX() != 0) { // east/west
            switch (heading.getBlockX()) {
                case 1: // east
                    return Vector3D.RotationAngle.ANGLE_90;
                case -1: // west
                    return Vector3D.RotationAngle.ANGLE_270;
            }
        }
        return Vector3D.RotationAngle.ANGLE_0;
    }
}