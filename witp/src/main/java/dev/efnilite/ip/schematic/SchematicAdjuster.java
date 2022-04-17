package dev.efnilite.ip.schematic;

import dev.efnilite.vilib.vector.Vector3D;
import dev.efnilite.ip.generator.Direction;
import dev.efnilite.ip.util.config.Option;
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

        return schematic.pasteAdjusted(adjustTo, getAngle(Option.HEADING.get()));
    }

    /**
     * Gets the angle from a specific Vector heading
     *
     * @param   heading
     *          The vector heading
     *
     * @return the associated angle
     */
    private static RotationAngle getAngle(Direction heading) {
        switch (heading) {
            case SOUTH: // south
                return RotationAngle.ANGLE_180;
            case NORTH: // north
                return RotationAngle.ANGLE_0;
            case EAST: // east
                return RotationAngle.ANGLE_270;
            case WEST: // west
                return RotationAngle.ANGLE_90;
        }
        return RotationAngle.ANGLE_270;
    }
}