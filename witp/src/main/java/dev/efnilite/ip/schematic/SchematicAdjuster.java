package dev.efnilite.ip.schematic;

import dev.efnilite.ip.config.Option;
import dev.efnilite.vilib.vector.Vector3D;
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

        return schematic.pasteAdjusted(adjustTo, getAngle(Option.HEADING));
    }

    /**
     * Gets the angle from a specific Vector heading
     *
     * @param   heading
     *          The vector heading
     *
     * @return the associated angle
     */
    public static RotationAngle getAngle(String heading) {
        return switch (heading.toLowerCase()) {
            case "south" -> // south
                    RotationAngle.ANGLE_180;
            case "north" -> // north
                    RotationAngle.ANGLE_0;
            case "east" -> // east
                    RotationAngle.ANGLE_270;
            case "west" -> // west
                    RotationAngle.ANGLE_90;

            default ->
                    RotationAngle.ANGLE_270;
        };
    }
}