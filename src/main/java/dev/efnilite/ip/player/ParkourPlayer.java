package dev.efnilite.ip.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.generator.Profile;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.mode.MultiMode;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.storage.Storage;
import dev.efnilite.ip.world.Divider;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.util.Colls;
import dev.efnilite.vilib.util.Task;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode.
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
        PLAYER_COLUMNS.put("useSpecial", new OptionContainer(ParkourOption.SPECIAL_BLOCKS, (player, v) -> player.useSpecialBlocks = parseBoolean(v)));
        PLAYER_COLUMNS.put("showFallMsg", new OptionContainer(ParkourOption.FALL_MESSAGE, (player, v) -> player.showFallMessage = parseBoolean(v)));
        PLAYER_COLUMNS.put("showScoreboard", new OptionContainer(ParkourOption.SCOREBOARD, (player, v) -> player.showScoreboard = parseBoolean(v)));
        PLAYER_COLUMNS.put("selectedTime", new OptionContainer(ParkourOption.TIME, (player, v) -> player.selectedTime = Integer.parseInt(v)));
        PLAYER_COLUMNS.put("collectedRewards", new OptionContainer(null, (player, v) -> {
            player.collectedRewards = new ArrayList<>();

            if (!v.isEmpty()) {
                player.collectedRewards.addAll(Arrays.stream(v.replaceAll("[ \\[\\]]", "").split(","))
                        .distinct()
                        .toList());
            }
        }));
        PLAYER_COLUMNS.put("locale", new OptionContainer(ParkourOption.LANG, (player, v) -> {
            player._locale = v;
            player.locale = v;
        }));
        PLAYER_COLUMNS.put("schematicDifficulty", new OptionContainer(ParkourOption.SCHEMATICS, (player, v) -> player.schematicDifficulty = Double.parseDouble(v)));
        PLAYER_COLUMNS.put("sound", new OptionContainer(ParkourOption.SOUND, (player, v) -> player.sound = parseBoolean(v)));
    }

    public @Expose Double schematicDifficulty;
    public @Expose Integer blockLead;
    public @Expose Boolean particles;
    public @Expose Boolean sound;
    public @Expose Boolean useSpecialBlocks;
    public @Expose Boolean showFallMessage;
    public @Expose Boolean showScoreboard;
    public @Expose Integer selectedTime;
    public @Expose String style;
    public @Expose String _locale;
    public @Expose List<String> collectedRewards;
    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player, Session)} instead
     */
    public ParkourPlayer(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, session, previousData);

        this._locale = locale;

        // generic player settings
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvisible(false);
    }

    private static boolean parseBoolean(String string) {
        return string == null
                || string.equals("1") // for MySQL
                || string.equals("true"); // for disk
    }

    /**
     * @param player The player.
     * @return True when this player is a {@link ParkourPlayer}, false if not.
     */
    public static boolean isPlayer(@Nullable Player player) {
        return player != null && getPlayers().stream().anyMatch(other -> other.player == player);
    }

    /**
     * @param player The player.
     * @return player as a {@link ParkourPlayer}, null if not found.
     */
    public static @Nullable ParkourPlayer getPlayer(@NotNull Player player) {
        return getPlayers().stream()
                .filter(other -> other.getUUID() == player.getUniqueId())
                .findAny()
                .orElse(null);
    }

    /**
     * @return List with all players.
     */
    public static List<ParkourPlayer> getPlayers() {
        return Divider.sections.keySet().stream()
                .flatMap(session -> session.getPlayers().stream())
                .toList();
    }

    @Override
    public void unregister() {
        IP.log("Unregistering player %s".formatted(player.getName()));

        if (session.generator.getMode() instanceof MultiMode mode) {
            mode.leave(player, session);
        }

        session.removePlayers(this);

        save(IP.getPlugin().isEnabled());
    }

    /**
     * Sets the user's settings. If an item is not included, the setting gets reset.
     *
     * @param settings The settings map.
     */
    public void setSettings(@NotNull Map<String, Object> settings) {
        for (String key : PLAYER_COLUMNS.keySet()) {
            Object value = settings.get(key);
            OptionContainer container = PLAYER_COLUMNS.get(key);

            if (container.consumer == null) {
                continue;
            }

            if (value == null || !Option.OPTIONS_ENABLED.getOrDefault(container.option, true)) {
                container.consumer.accept(this, Option.OPTIONS_DEFAULTS.getOrDefault(container.option, ""));
                continue;
            }

            container.consumer.accept(this, String.valueOf(value));
        }
    }

    /**
     * Forces this player's generator to match the settings of this player.
     */
    public void updateGeneratorSettings(ParkourGenerator generator) {
        Profile profile = generator.profile;

        profile.set("schematicDifficulty", schematicDifficulty.toString())
                .set("blockLead", blockLead.toString())
                .set("particles", particles.toString())
                .set("sound", sound.toString())
                .set("useSpecialBlocks", useSpecialBlocks.toString())
                .set("showFallMessage", showFallMessage.toString())
                .set("showScoreboard", showScoreboard.toString())
                .set("selectedTime", selectedTime.toString())
                .set("style", style);

        generator.overrideProfile();
    }

    /**
     * Saves the player's data to their file
     */
    public void save(boolean async) {
        Runnable write = () -> Storage.writePlayer(this);

        if (async) {
            Task.create(IP.getPlugin()).async().execute(write).run();
        } else {
            write.run();
        }
    }

    public void setup(Location to) {
        IP.log("Setting up player %s".formatted(player.getName()));

        if (to != null) {
            teleport(to);
        }

        player.setGameMode(GameMode.ADVENTURE);

        // -= Inventory =-
        if (Config.CONFIG.getBoolean("options.inventory-handling")) {
            Task.create(IP.getPlugin()).delay(5).execute(() -> {
                IP.log("Setting up inventory for player %s".formatted(player.getName()));

                player.getInventory().clear();

                List<Item> items = new ArrayList<>();

                if (ParkourOption.PLAY.mayPerform(player)) items.add(Locales.getItem(locale, "play.item"));
                if (ParkourOption.COMMUNITY.mayPerform(player)) items.add(Locales.getItem(locale, "community.item"));
                if (ParkourOption.SETTINGS.mayPerform(player)) items.add(Locales.getItem(locale, "settings.item"));
                if (ParkourOption.LOBBY.mayPerform(player)) items.add(Locales.getItem(locale, "lobby.item"));

                if (ParkourOption.QUIT.mayPerform(player)) items.add(Locales.getItem(locale, "other.quit"));

                List<Integer> slots = Menu.getEvenlyDistributedSlots(items.size());
                Colls.range(0, items.size()).forEach(idx -> player.getInventory().setItem(slots.get(idx), items.get(idx).build()));
            }).run();
        } else {
            sendTranslated("other.customize");
        }
    }

    public record OptionContainer(ParkourOption option, BiConsumer<ParkourPlayer, String> consumer) {

    }
}