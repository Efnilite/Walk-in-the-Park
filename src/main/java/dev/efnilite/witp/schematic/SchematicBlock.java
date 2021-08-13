package dev.efnilite.witp.schematic;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public class SchematicBlock {

    private Vector relativePosition;
    private BlockData data;

    public SchematicBlock(Block block, Vector relativePosition) {
        this.relativePosition = relativePosition;
        this.data = block.getBlockData();
    }

    public SchematicBlock(BlockData data, Vector relativePosition) {
        this.relativePosition = relativePosition;
        this.data = data;
    }

    public Vector getRelativePosition() {
        return relativePosition;
    }

    public void setRelativePosition(Vector relativePosition) {
        this.relativePosition = relativePosition;
    }

    public BlockData getData() {
        return data;
    }
}