package dev.efnilite.ip.style;

import dev.efnilite.ip.session.Session;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Represents a style.
 *
 * @param name             The style name.
 * @param materials        The possible style {@link BlockData}s.
 * @param category         The name of this style's category, like "default" or "incremental".
 * @param materialSelector The function that selects the {@link BlockData}.
 *                         Provided arguments are the current possible {@link BlockData}s used by this style and the session.
 *                         Returns the material that will be used for the next parkour block.
 */
public record Style(@NotNull String name, @NotNull List<BlockData> materials, @NotNull String category,
                    @NotNull BiFunction<List<BlockData>, Session, BlockData> materialSelector) {

    public Style {
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("Materials can't be empty");
        }
    }

    /**
     * @param session The session.
     * @return The {@link BlockData}
     */
    @NotNull
    public BlockData get(@NotNull Session session) {
        return materialSelector.apply(materials, session);
    }
}
