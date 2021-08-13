package dev.efnilite.witp.schematic;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class SchematicBlock {

    private Vector3D relativePosition;
    private BlockData data;

    public SchematicBlock(Block block, Vector3D relativePosition) {
        this.relativePosition = relativePosition;
        this.data = block.getBlockData();
    }

    public SchematicBlock(BlockData data, Vector3D relativePosition) {
        this.relativePosition = relativePosition;
        this.data = data;
    }

    public Vector3D getRelativePosition() {
        return relativePosition;
    }

    public void setRelativePosition(Vector3D relativePosition) {
        this.relativePosition = relativePosition;
    }

    public BlockData getData() {
        return data;
    }
}