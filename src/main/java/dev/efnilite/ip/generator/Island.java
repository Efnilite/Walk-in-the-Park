package dev.efnilite.ip.generator;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.world.WorldDivider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Spawn island handler.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public final class Island {

    /**
     * The blocks that have been affected by the schematic.
     */
    public List<Block> blocks;

    /**
     * The session.
     */
    public final Session session;

    /**
     * The schematic.
     */
    public final Schematic schematic;

    public Island(@NotNull Session session, @Nullable Schematic schematic) {
        this.session = session;
        this.schematic = schematic;
    }

    /**
     * Builds the island and teleports the player.
     */
    public void build() {
        if (schematic == null) {
            return;
        }

        blocks = schematic.paste(WorldDivider.toLocation(session).subtract(0, schematic.getDimensions().getY(), 0));

        Material playerMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.spawn.player-block").toUpperCase());
        Material parkourMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.parkour.begin-block").toUpperCase());

        try {
            Block player = blocks.stream().filter(block -> block.getType() == playerMaterial).findAny().get();
            Block parkour = blocks.stream().filter(block -> block.getType() == parkourMaterial).findAny().get();

            player.setType(Material.AIR);
            parkour.setType(Material.AIR);

            Location ps = player.getLocation().add(0.5, 0, 0.5);
            ps.setYaw(Config.GENERATION.getInt("advanced.island.spawn.yaw"));
            ps.setPitch(Config.GENERATION.getInt("advanced.island.spawn.pitch"));

            session.generator.generateFirst(ps, parkour.getLocation().subtract(session.generator.heading).subtract(0, 1, 0));
            session.generator.startTick();
            session.getPlayers().forEach(pp -> pp.setup(ps));
        } catch (NoSuchElementException ex) {
            IP.logging().stack("Error while trying to find parkour or player spawn in schematic %s".formatted(schematic.getFile().getName()),
                    "check if you used the same material as the one in generation.yml", ex);

            blocks.forEach(block -> block.setType(Material.AIR));
        }
    }

    /**
     * Destroys the island.
     */
    public void destroy() {
        if (blocks == null) {
            return;
        }

        blocks.forEach(block -> block.setType(Material.AIR, false));
    }
}