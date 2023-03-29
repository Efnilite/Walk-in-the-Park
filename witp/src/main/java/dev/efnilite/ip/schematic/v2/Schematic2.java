package dev.efnilite.ip.schematic.v2;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Map;

public record Schematic2(Map<Vector, BlockData> offsets) {

    public void paste(Location location) {
        offsets.forEach((vector, blockData) -> location.clone()
                .add(vector)
                .getBlock()
                .setBlockData(blockData, false));
    }

}
