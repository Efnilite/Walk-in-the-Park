package dev.efnilite.ip.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.generator.Profile;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Task;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode
 * besides Spectator Mode. Please note that this is NOT the same as {@link ParkourUser} itself.
 *
 * @author Efnilite
 */
public class ParkourPlayer extends ParkourUser {

    public static final Map<String, OptionContainer> PLAYER_COLUMNS = new HashMap<>();

    static {
        PLAYER_COLUMNS.put("uuid", new OptionContainer(null, null));
        PLAYER_COLUMNS.put("style", new OptionContainer(ParkourOption.STYLES, (player, v) -> player.style = v));
        PLAYER_COLUMNS.put("blockLead", new OptionContainer(ParkourOption.LEADS, (player, v) -> player.blockLead = Integer.parseInt(v)));
        PLAYER_COLUMNS.put("useParticles", new OptionContainer(ParkourOption.PARTICLES, (player, v) -> player.particles = parseBoolean(v)));
        PLAYER_COLUMNS.put("useDifficulty", new OptionContainer(ParkourOption.SCORE_DIFFICULTY, (player, v) -> player.useScoreDifficulty = parseBoolean(v)));
        PLAYER_COLUMNS.put("useStructure", new OptionContainer(ParkourOption.USE_SCHEMATICS, (player, v) -> player.useSchematic = parseBoolean(v)));
        PLAYER_COLUMNS.put("useSpecial", new OptionContainer(ParkourOption.SPECIAL_BLOCKS, (player, v) -> player.useSpecialBlocks = parseBoolean(v)));
        PLAYER_COLUMNS.put("showFallMsg", new OptionContainer(ParkourOption.FALL_MESSAGE, (player, v) -> player.showFallMessage = parseBoolean(v)));
        PLAYER_COLUMNS.put("showScoreboard", new OptionContainer(ParkourOption.SCOREBOARD, (player, v) -> player.showScoreboard = parseBoolean(v)));
        PLAYER_COLUMNS.put("selectedTime", new OptionContainer(ParkourOption.TIME, (player, v) -> player.selectedTime = Integer.parseInt(v)));
        PLAYER_COLUMNS.put("collectedRewards", new OptionContainer(null, (player, v) -> player.collectedRewards = Arrays.asList(v.split(","))));
        PLAYER_COLUMNS.put("locale", new OptionContainer(ParkourOption.LANG, (player, v) -> {
            player._locale = v;
            player.setLocale(v);
        }));
        PLAYER_COLUMNS.put("schematicDifficulty", new OptionContainer(ParkourOption.SCHEMATIC_DIFFICULTY, (player, v) -> player.schematicDifficulty = Double.parseDouble(v)));
        PLAYER_COLUMNS.put("sound", new OptionContainer(ParkourOption.SOUND, (player, v) -> player.sound = parseBoolean(v)));
    }

    private static boolean parseBoolean(String string) {
        return string == null || string.equals("1");
    }

    public record OptionContainer(ParkourOption option, BiConsumer<ParkourPlayer, String> consumer) {

    }

    public @Expose Double schematicDifficulty;
    public @Expose Integer blockLead;
    public @Expose Boolean useScoreDifficulty;
    public @Expose Boolean particles;
    public @Expose Boolean sound;
    public @Expose Boolean useSpecialBlocks;
    public @Expose Boolean showFallMessage;
    public @Expose Boolean showScoreboard;
    public @Expose Boolean useSchematic;
    public @Expose Integer selectedTime;
    public @Expose String style;
    public @Expose String _locale;
    public @Expose List<String> collectedRewards;

    /**
     * This player's generator.
     */
    public ParkourGenerator generator;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, @Nullable PreviousData previousData) {
        super(player, previousData);

        setLocale(Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG));
        this._locale = getLocale();

        // generic player settings
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvisible(false);
    }

    /**
     * Sets the user's settings. If an item is not included, the setting gets reset.
     * @param settings The settings map.
     */
    public void setSettings(@NotNull Map<String, Object> settings) {
        for (String key : PLAYER_COLUMNS.keySet()) {
            Object value = settings.get(key);
            OptionContainer container = PLAYER_COLUMNS.get(key);

            if (value == null || !Option.OPTIONS_ENABLED.get(container.option)) {
                container.consumer.accept(this, Option.OPTIONS_DEFAULTS.get(container.option));
                continue;
            }

            container.consumer.accept(this, String.valueOf(value));
        }
    }

    /**
     * Forces this player's generator to match the settings of this player.
     */
    public void updateGeneratorSettings() {
        Profile profile = generator.profile;

        profile.set("schematicDifficulty", schematicDifficulty.toString())
                .set("blockLead", blockLead.toString())
                .set("useScoreDifficulty", useScoreDifficulty.toString())
                .set("particles", particles.toString())
                .set("sound", sound.toString())
                .set("useSpecialBlocks", useSpecialBlocks.toString())
                .set("showFallMessage", showFallMessage.toString())
                .set("showScoreboard", showScoreboard.toString())
                .set("useSchematic", useSchematic.toString())
                .set("selectedTime", selectedTime.toString())
                .set("style", style);
    }

    /**
     * Saves the player's data to their file
     */
    public void save(boolean async) {
        Runnable write = () -> IP.getStorage().writePlayer(this);

        if (async) {
            Task.create(IP.getPlugin()).async().execute(write).run();
        } else {
            write.run();
        }
    }

    /**
     * Calculates a score between 0 (inclusive) and 1 (inclusive) to determine how difficult it was for
     * the player to achieve this score using their settings.
     *
     * @return a number from 0 to 1 (both inclusive)
     */
    public String calculateDifficultyScore() {
        try {
            double score = 0.0;
            if (useSpecialBlocks) {
                score += 0.3;          // sum:      0.3
            }
            if (useScoreDifficulty) {
                score += 0.2;       //           0.5
            }
            if (useSchematic) {
                if (schematicDifficulty == 0.3) {
                    score += 0.1;      //    0.6
                } else if (schematicDifficulty == 0.5) {
                    score += 0.3; //    0.8
                } else if (schematicDifficulty == 0.7) {
                    score += 0.4; //    0.9
                } else if (schematicDifficulty == 0.8) {
                    score += 0.5; //    1.0
                }
            }
            return Double.toString(score).substring(0, 3);
        } catch (NullPointerException ex) {
            return "?";
        }
    }

    /**
     * Gets a ParkourPlayer from a regular Player
     *
     * @param player The Bukkit Player
     * @return the ParkourPlayer
     */
    public static @Nullable ParkourPlayer getPlayer(@NotNull Player player) {
        List<ParkourPlayer> filtered = getActivePlayers().stream().filter(other -> other.getUUID() == player.getUniqueId()).toList();

        return filtered.size() > 0 ? filtered.get(0) : null;
    }

    public void setup(Location to, boolean runGenerator) {
        if (to != null) {
            teleport(to);
        }
        player.setGameMode(GameMode.ADVENTURE);

        // -= Inventory =-
        if (Option.INVENTORY_HANDLING) {
            Task.create(IP.getPlugin()).delay(5).execute(() -> {
                List<Item> items = new ArrayList<>();

                player.getInventory().clear();

                if (ParkourOption.PLAY.mayPerform(player)) {
                    items.add(0, Locales.getItem(getLocale(), "play.item"));
                }

                if (ParkourOption.COMMUNITY.mayPerform(player)) {
                    items.add(items.size(), Locales.getItem(getLocale(), "community.item"));
                }

                if (ParkourOption.SETTINGS.mayPerform(player)) {
                    items.add(items.size(), Locales.getItem(getLocale(), "settings.item"));
                }

                if (ParkourOption.LOBBY.mayPerform(player)) {
                    items.add(items.size(), Locales.getItem(getLocale(), "lobby.item"));
                }

                items.add(items.size(), Locales.getItem(getLocale(), "other.quit"));

                List<Integer> slots = getEvenlyDistributedSlots(items.size());
                for (int i = 0; i < items.size(); i++) {
                    player.getInventory().setItem(slots.get(i), items.get(i).build());
                }
            }).run();
        }

        if (!Option.INVENTORY_HANDLING) {
            sendTranslated("other.customize");
        }

        if (runGenerator) {
            generator.startTick();
        }
    }

    private List<Integer> getEvenlyDistributedSlots(int amountInRow) {
        return switch (amountInRow) {
            case 0 -> Collections.emptyList();
            case 1 -> Collections.singletonList(4);
            case 2 -> Arrays.asList(3, 5);
            case 3 -> Arrays.asList(3, 4, 5);
            case 4 -> Arrays.asList(2, 3, 5, 6);
            case 5 -> Arrays.asList(2, 3, 4, 5, 6);
            case 6 -> Arrays.asList(1, 2, 3, 5, 6, 7);
            case 7 -> Arrays.asList(1, 2, 3, 4, 5, 6, 7);
            case 8 -> Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8);
            default -> Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8);
        };
    }
}