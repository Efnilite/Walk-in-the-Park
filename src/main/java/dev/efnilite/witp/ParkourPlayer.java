package dev.efnilite.witp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import dev.efnilite.witp.events.PlayerLeaveEvent;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.task.Tasks;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wrapper class for a regular player to store plugin-usable data
 *
 * @author Efnilite
 */
public class ParkourPlayer {

    /**
     * Player data used in saving
     */
    public @Expose int highScore;
    /**
     * Player data used in saving
     */
    public @Expose int blockLead;
    /**
     * Player data used in saving
     */
    public @Expose boolean useDifficulty;
    /**
     * Player data used in saving
     */
    public @Expose Boolean useParticles;
    /**
     * Player data used in saving
     */
    public @Expose boolean useStructures;
    /**
     * Player data used in saving
     */
    public @Expose String time;
    /**
     * Player data used in saving
     */
    public @Expose String style;

    /**
     * The player's points
     */
    public UUID openInventory;
    private final Location previousLocation;
    private HashMap<Integer, ItemStack> previousInventory;
    private List<Material> possibleStyle;
    private final File file;
    private final Player player;
    private final ParkourGenerator generator;
    private final FastBoard board;
    private static final HashMap<Player, ParkourPlayer> players = new HashMap<>();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link WITPAPI#registerPlayer(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, int highScore, String time, String style, int blockLead, boolean useParticles,
                         boolean useDifficulty, boolean useStructures) {
        this.previousLocation = player.getLocation().clone();
        this.previousInventory = new HashMap<>();
        int index = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                previousInventory.put(index, item);
            }
        }

        this.highScore = highScore;
        this.blockLead = blockLead;
        this.style = style;
        this.useParticles = useParticles;
        this.time = time;
        this.useDifficulty = useDifficulty;
        this.useStructures = useStructures;

        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + player.getUniqueId().toString() + ".json");
        this.player = player;
        this.possibleStyle = new ArrayList<>();
        this.board = new FastBoard(player);
        setStyle(style);
        this.generator = new ParkourGenerator(this);

        player.setPlayerTime(getTime(time), false);
        WITP.getDivider().generate(this);
        updateScoreboard();
        if (player.isOp() && WITP.isOutdated) {
            send("&4&l!!! &fThe WITP plugin version you are using is outdated. Please check the Spigot page for updates.");
        }
    }

    /**
     * Updates the scoreboard
     */
    public void updateScoreboard() {
        board.updateTitle(WITP.getConfiguration().getString("config", "scoreboard.title"));
        List<String> list = new ArrayList<>();
        List<String> lines = WITP.getConfiguration().getStringList("config", "scoreboard.lines");
        if (lines == null) {
            return;
        }
        for (String s : lines) {
            list.add(s
                    .replaceAll("%score%", Integer.toString(generator.score))
                    .replaceAll("%time%", generator.time));
        }

        board.updateLines(list);
    }

    /**
     * Returns a random material from the possible styles
     * @see ParkourGenerator#generateNext()
     *
     * @return a random material
     */
    public Material randomMaterial() {
        return possibleStyle.get(ThreadLocalRandom.current().nextInt(possibleStyle.size()));
    }

    /**
     * Sets the style and updates the possibleStyle variable to update the Material style
     *
     * @param   style
     *          The style as listed in config.yml
     */
    public void setStyle(String style) {
        this.style = style;
        possibleStyle = getPossibleMaterials(style);
    }

    /**
     * Sets the high score of a player
     *
     * @param   score
     *          The score
     */
    public void setHighScore(int score) {
        this.highScore = score;
        saveStats();
    }

    /**
     * Opens the menu
     */
    public void menu() {
        InventoryBuilder builder = new InventoryBuilder(this, 3, "Customize").open();
        InventoryBuilder builder1 = new InventoryBuilder(this, 3, "Lead").open();
        InventoryBuilder builder2 = new InventoryBuilder(this, 3, "Parkour style").open();
        InventoryBuilder builder3 = new InventoryBuilder(this, 3, "Time").open();

        if (WITP.getConfiguration().getFile("config").getBoolean("styles.enabled")) {
            builder.setItem(11, new ItemBuilder(Material.END_STONE, "&a&lParkour style")
                    .setLore("&7The style of your parkour.", "&7(which blocks will be used)", "", "&7Currently: &a" + style).build(), (t, e) -> {
                List<String> styles = Util.getNode(WITP.getConfiguration().getFile("config"), "styles.list");
                if (styles == null) {
                    Verbose.error("Error while trying to fetch possible styles from config.yml");
                    return;
                }
                int i = 0;
                for (String style : styles) {
                    List<Material> possible = this.getPossibleMaterials(style);
                    if (possible == null) {
                        continue;
                    }
                    Material material = possible.get(possible.size() - 1);
                    builder2.setItem(i, new ItemBuilder(material, "&b&l" + Util.capitalizeFirst(style)).build(), (t2, e2) -> {
                        String selected = ChatColor.stripColor(e2.getItemMeta().getDisplayName()).toLowerCase();
                        this.setStyle(selected);
                        this.send("&7You selected style &c" + selected + "&7!");
                        this.saveStats();
                    });
                    i++;
                    builder2.setItem(22, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
                }
                builder2.build();
            });
        }
        builder.setItem(12, new ItemBuilder(Material.GLASS, "&a&lLead")
                .setLore("&7How many blocks will", "&7be generated ahead of you.", "", "&7Currently: &a" + blockLead + " blocks").build(), (t, e) -> {
            for (int i = 10; i < 17; i++) {
                builder1.setItem(i, new ItemBuilder(Material.PAPER, "&b&l" + (i - 9) + " block(s)").build(), (t2, e2) -> {
                    int amount = t2.getSlot() - 9;
                    blockLead = amount;
                    send("&7You selected a " + amount + " block lead.");
                    saveStats();
                });
            }
            builder1.setItem(22, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
            builder1.build();
        });
        builder.setItem(13, new ItemBuilder(Material.CLOCK, "&a&lTime")
                .setLore("&7The time of day.", "", "&7Currently: &a" + time.toLowerCase()).build(), (t, e) -> {
            List<String> times = Arrays.asList("Day", "Noon", "Dawn", "Night", "Midnight");
            int i = 11;
            for (String time : times) {
                builder3.setItem(i, new ItemBuilder(Material.PAPER, "&b&l" + time).build(), (t2, e2) -> {
                    if (e2.getItemMeta() != null) {
                        String name = ChatColor.stripColor(e2.getItemMeta().getDisplayName());
                        this.time = name;
                        send("&7You changed your time preference to &a" + time.toLowerCase());
                        player.setPlayerTime(getTime(name), false);
                        saveStats();
                    }
                });
                i++;
            }
            builder3.setItem(22, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
            builder3.build();
        });
        Material difficulty = useDifficulty ? Material.GREEN_WOOL : Material.RED_WOOL;
        String difficultyString = Boolean.toString(useDifficulty);
        String difficultyValue = Util.normalizeBoolean(Util.colorBoolean(difficultyString));
        builder.setItem(15, new ItemBuilder(difficulty, "&a&lUse difficulty")
                .setLore("&7If enabled having a higher score will mean", "&7the parkour becomes more difficult.", "",
                        "&7Currently: " + difficultyValue).build(), (t2, e2) -> {
                    useDifficulty = !useDifficulty;
                    send("&7You changed your changed your usage of difficulty to " +
                            Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(difficultyString))));
                    saveStats();
                    player.closeInventory();
        });
        Material particles = useParticles ? Material.GREEN_WOOL : Material.RED_WOOL;
        String particlesString = Boolean.toString(useParticles);
        String particlesValue = Util.normalizeBoolean(Util.colorBoolean(particlesString));
        builder.setItem(14, new ItemBuilder(particles, "&a&lUse particles and sounds")
                .setLore("&7If enabled every generated block", "&7will show particles and play a sound.", "",
                        "&7Currently: " + particlesValue).build(), (t2, e2) -> {
            useParticles = !useParticles;
            send("&7You changed your changed your usage of particles to " +
                    Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(particlesString))));
            saveStats();
            player.closeInventory();
        });
//        Material structures = useStructures ? Material.GREEN_WOOL : Material.RED_WOOL;
//        String structuresString = Boolean.toString(useStructures);
//        String structuresValue = Util.normalizeBoolean(Util.colorBoolean(useStructures));
//        builder.setItem(15, new ItemBuilder(structures, "&a&lUse structures")
//                .setLore("&7If enabled static structures", "&7will appear throughout the parkour.", "",
//                        "&7Currently: " + structuresValue).build(), (t2, e2) -> {
//                    useStructures = !useStructures;
//                    send("&7You changed your changed your usage of structures to " +
        //                    Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(structuresString))));
//                    saveStats();
//                    player.closeInventory();
//        });
        builder.setItem(3 * 9 - 1, new ItemBuilder(Material.BARRIER, "&4&lQuit").build(), (t2, e2) -> {
            try {
                ParkourPlayer.unregister(this);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to quit player " + player.getName());
            }
        });
        builder.setItem(22, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
        builder.build();
    }

    private void saveStats() {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    save();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Verbose.error("Error while trying to save the file of player " + player.getName());
                }
            }
        };
        Tasks.asyncTask(runnable);
    }

    private @Nullable List<Material> getPossibleMaterials(String style) {
        List<Material> possibleStyles = new ArrayList<>();
        String possible = WITP.getConfiguration().getFile("config").getString("styles.list." + style);
        if (possible == null) {
            Verbose.error("Style selected (" + style + ") doesn't exist in config.yml!");
            return null;
        }
        String[] materials = possible.replaceAll("[\\[\\]]", "").split(", ");
        for (String material : materials) {
            Material mat = Material.getMaterial(material.toUpperCase());
            if (mat == null) {
                return null;
            }
            possibleStyles.add(mat);
        }

        return possibleStyles;
    }

    /**
     * Saves the player's data to their file
     *
     * @throws  IOException
     *          Thrown if creation of file, new FileWriter fails or writing to the file fails
     */
    public void save() throws IOException {
        if (!file.exists()) {
            File folder = new File(WITP.getInstance().getDataFolder() + "/players");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        gson.toJson(this, writer);
        writer.flush();
        writer.close();
    }

    /**
     * Sends a message or array of it - coloured allowed, using '&'
     *
     * @param   messages
     *          The message
     */
    public void send(@NotNull String... messages) {
        for (String msg : messages) {
            player.sendMessage(Util.color(msg));
        }
    }

    /**
     * Registers a player
     *
     * @param   player
     *          The player
     *
     * @throws  IOException
     *          Thrown if the reader fails or the getting fails
     */
    public static @NotNull ParkourPlayer register(Player player) throws IOException {
        if (players.get(player) == null) {
            UUID uuid = player.getUniqueId();
            File data = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid.toString() + ".json");
            if (data.exists()) {
                FileReader reader = new FileReader(data);
                ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);
                if (from.useParticles == null) { // outdated file format
                    from.useParticles = true;
                }
                ParkourPlayer pp = new ParkourPlayer(player, from.highScore, from.time, from.style, from.blockLead,
                        from.useParticles, from.useDifficulty, from.useStructures);
                pp.save();
                players.put(player, pp);
                reader.close();
                return pp;
            } else {
                ParkourPlayer pp = new ParkourPlayer(player, 0, "Day",
                        WITP.getConfiguration().getString("config", "styles.default"),
                        4, true, true, false);
                players.put(player, pp);
                pp.save();
                return pp;
            }
        }
        return players.get(player);
    }

    /**
     * Gets a ParkourPlayer from a regular Player
     *
     * @param   player
     *          The Bukkit Player
     * @return the ParkourPlayer
     */
    public static @Nullable ParkourPlayer getPlayer(Player player) {
        for (Player p : players.keySet()) {
            if (p == player) {
                return players.get(p);
            }
        }
        return null;
    }

    public int getTime(String time) {
        switch (time.toLowerCase()) {
            case "noon":
                return 6000;
            case "dawn":
                return 12500;
            case "night":
                return 15000;
            case "midnight":
                return 18000;
            case "day":
            default:
                return 1000;
        }
    }

    /**
     * Unregisters a ParkourPlayer
     *
     * @param   player
     *          The ParkourPlayer
     *
     * @throws  IOException
     *          When saving the player's file goes wrong
     */
    public static void unregister(@NotNull ParkourPlayer player) throws IOException {
        new PlayerLeaveEvent(player).call();
        player.generator.reset(false);
        player.generator.finish();
        player.save();
        WITP.getDivider().leave(player);
        players.remove(player.getPlayer());
        if (WITP.getConfiguration().getFile("config").getBoolean("bungeecord.enabled")) {
            Util.sendPlayer(player.getPlayer(), WITP.getConfiguration().getString("config", "bungeecord.return_server"));
        } else {
            player.getPlayer().teleport(player.getPreviousLocation());
            for (int slot : player.getPreviousInventory().keySet()){
                player.getPlayer().getInventory().setItem(slot, player.getPreviousInventory().get(slot));
            }
        }
    }

    public HashMap<Integer, ItemStack> getPreviousInventory() {
        return previousInventory;
    }

    public Location getPreviousLocation() {
        return previousLocation;
    }

    /**
     * Gets the player's {@link ParkourGenerator}
     *
     * @return the ParkourGenerator associated with this player
     */
    public ParkourGenerator getGenerator() {
        return generator;
    }

    /**
     * Gets the Bukkit version of the player
     *
     * @return the player
     */
    public @NotNull Player getPlayer() {
        return player;
    }
}
