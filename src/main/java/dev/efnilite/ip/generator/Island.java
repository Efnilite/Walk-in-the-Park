package dev.efnilite.ip.generator;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.schematic.Schematic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Spawn island handler.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public final class Island {

    /**
     * The session.
     */
    public final Session session;
    /**
     * The schematic.
     */
    public final Schematic schematic;

    /**
     * The blocks that have been affected by the schematic.
     */
    public List<Block> blocks;

    public Island(@NotNull Session session, @Nullable Schematic schematic) {
        this.session = session;
        this.schematic = schematic;
    }

    /**
     * Builds the island and teleports the player.
     */
    public void build(Location location) {
        if (schematic == null) {
            return;
        }
        IP.log("Building island");

        blocks = schematic.paste(location.subtract(0, schematic.getDimensions().getY(), 0));

        Material playerMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.spawn.player-block").toUpperCase());
        Material parkourMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.parkour.begin-block").toUpperCase());

        try {
            Block player = blocks.stream().filter(block -> block.getType() == playerMaterial).findAny().orElseThrow();
            Block parkour = blocks.stream().filter(block -> block.getType() == parkourMaterial).findAny().orElseThrow();

            player.setType(Material.AIR);
            parkour.setType(Material.AIR);

            var ps = player.getLocation().add(0.5, 0, 0.5).toVector();

            session.generator.generateFirst(ps, parkour.getLocation().subtract(session.generator.heading).subtract(0, 1, 0));
            session.generator.startTick();
            session.getPlayers().forEach(pp -> pp.getData().setup(ps));
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