package dev.efnilite.ip.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.generator.Profile;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Task;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode
 * besides Spectator Mode. Please note that this is NOT the same as {@link ParkourUser} itself.
 *
 * @author Efnilite
 */
public class ParkourPlayer extends ParkourUser {

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

    public ParkourGenerator generator;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, @Nullable PreviousData previousData) {
        super(player, previousData);

        setLocale((String) Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG));
        this._locale = getLocale();

        // generic player settings
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvisible(false);
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

    public void setSettings(String selectedTime, String style, String locale, String schematicDifficulty, String blockLead, Boolean particles, Boolean sound, Boolean useDifficulty, Boolean useStructure, Boolean useSpecial, Boolean showDeathMsg, Boolean showScoreboard, String collectedRewards) {

        this.collectedRewards = new ArrayList<>();
        if (collectedRewards != null) {
            for (String s : collectedRewards.split(",")) {
                if (!s.isEmpty() && !this.collectedRewards.contains(s)) { // prevent empty strings and duplicates
                    this.collectedRewards.add(s);
                }
            }
        }

        // Adjustable defaults
        this.style = orDefault(style, (String) Option.OPTIONS_DEFAULTS.get(ParkourOption.STYLES), null);
        this._locale = orDefault(locale, (String) Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG), null);
        setLocale(_locale);

        this.useSpecialBlocks = orDefault(useSpecial, (boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.SPECIAL_BLOCKS), ParkourOption.SPECIAL_BLOCKS);
        this.showFallMessage = orDefault(showDeathMsg, (boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.FALL_MESSAGE), ParkourOption.FALL_MESSAGE);
        this.useScoreDifficulty = orDefault(useDifficulty, (boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.SCORE_DIFFICULTY), ParkourOption.SCHEMATIC_DIFFICULTY);
        this.useSchematic = orDefault(useStructure, (boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.USE_SCHEMATICS), ParkourOption.USE_SCHEMATICS);
        this.showScoreboard = orDefault(showScoreboard, (boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD), ParkourOption.SCOREBOARD);
        this.particles = orDefault(particles, (boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.PARTICLES), ParkourOption.PARTICLES);
        this.sound = orDefault(sound, (boolean) Option.OPTIONS_DEFAULTS.get(ParkourOption.SOUND), ParkourOption.SOUND);

        this.blockLead = Integer.parseInt(orDefault(blockLead, String.valueOf(Option.OPTIONS_DEFAULTS.get(ParkourOption.LEADS)), ParkourOption.LEADS));
        this.selectedTime = Integer.parseInt(orDefault(selectedTime, String.valueOf(Option.OPTIONS_DEFAULTS.get(ParkourOption.TIME)), ParkourOption.TIME));

        this.schematicDifficulty = Double.parseDouble(orDefault(schematicDifficulty, String.valueOf(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCHEMATIC_DIFFICULTY)), ParkourOption.SCHEMATIC_DIFFICULTY));
    }

    private <T> T orDefault(T value, T def, @Nullable ParkourOption option) {
        // check for null default values
        if (def == null) {
            IP.logging().stack("Default value is null!", "Please see if there are any errors above. Check your config.");
        }

        // if option is disabled, return the default value
        // this allows users to set unchangeable settings
        if (option != null && !Option.OPTIONS_ENABLED.get(option)) {
            return def;
        }

        return value == null ? def : value;
    }

    public void resetPlayerPreferences() {
        setSettings(null, null, null, null, null, null,
                null, null, null, null, null, null, null);
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
        List<ParkourPlayer> filtered = Colls.filter(other -> other.getUUID() == player.getUniqueId(), getActivePlayers());

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