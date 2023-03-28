package dev.efnilite.ip.schematic.data;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Colls;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles sign data processing
 */
public class InstanceDataSign implements InstanceData {

    @Override
    public @Nullable String serialize(BlockData data) {
        if (!(data instanceof Sign sign)) {
            return null;
        }

        return IP.getGson().toJson(new SignDataContainer(sign.getLines(), sign.isGlowingText()));
    }

    @Override
    public @NotNull BlockData deserialize(BlockData data, String extra) {
        Sign sign = (Sign) data;

        SignDataContainer container = IP.getGson().fromJson(extra, SignDataContainer.class);
        Colls.range(container.lines.length).forEach(idx -> sign.setLine(idx, container.lines[idx]));
        sign.setGlowingText(container.isGlowing);

        return (BlockData) sign;
    }

    private record SignDataContainer(@Expose String[] lines, @Expose boolean isGlowing) {}
}