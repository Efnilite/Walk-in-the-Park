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
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.*;
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
    public @Expose int blockLead;
    public @Expose boolean useDifficulty;
    public @Expose Boolean useParticles;
    public @Expose Boolean useSpecial;
    public @Expose Boolean showDeathMsg;
    public @Expose Boolean useStructure;
    public @Expose String time;
    public @Expose String style;
    public @Expose String lang;


    /**
     * The player's points
     */
    public UUID openInventory;
    private FastBoard board;
    private List<Material> possibleStyle;
    private final Location previousLocation;
    private final HashMap<Integer, ItemStack> previousInventory;
    private final File file;
    private final Player player;
    private final ParkourGenerator generator;
    private static final HashMap<Player, ParkourPlayer> players = new HashMap<>();
    private static HashMap<UUID, Integer> highScores = new LinkedHashMap<>();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link WITPAPI#registerPlayer(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, int highScore, String time, String style, int blockLead, boolean useParticles,
                         boolean useDifficulty, boolean useStructure, boolean showScoreboard, boolean showDeathMsg) {
        this.previousLocation = player.getLocation().clone();
        this.previousInventory = new HashMap<>();
        int index = 0;
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                previousInventory.put(index, item);
            }
            index++;
        }
        this.useSpecial = showScoreboard;
        this.showDeathMsg = showDeathMsg;
        this.highScore = highScore;
        this.blockLead = blockLead;
        this.style = style;
        this.useParticles = useParticles;
        this.time = time;
        this.useDifficulty = useDifficulty;
        this.useStructure = useStructure;

        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + player.getUniqueId().toString() + ".json");
        this.player = player;
        this.possibleStyle = new ArrayList<>();
        this.board = new FastBoard(player);
        setStyle(style);
        this.generator = new ParkourGenerator(this);

        player.setPlayerTime(getTime(time), false);
        WITP.getDivider().generate(this);
        if (showDeathMsg && ParkourGenerator.Configurable.SCOREBOARD) {
            updateScoreboard();
        }
        if (player.isOp() && WITP.isOutdated) {
            send("&4&l!!! &fThe WITP plugin version you are using is outdated. Please check the Spigot page for updates.");
        }
        if (highScores.size() == 0) {
            try {
                fetchHighScores();
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to fetch the high scores!");
            }
            highScores = Util.sortByValue(highScores);
        }
    }


    /**
     * Gets a message from lang.yml
     *
     * @param   path
     *          The path name in lang.yml (for example: 'time-preference')
     *
     * @param   replaceable
     *          What can be replaced (for example: %s to yes)
     */
    public void sendTranslated(String path, String... replaceable) {
        path = "lang." + lang + "." + path;
        String string = WITP.getConfiguration().getString("lang", path);
        if (string == null) {
            Verbose.error("Unknown path: " + path);
            return;
        }
        for (String s : replaceable) {
            string = string.replaceAll("%[a-z]", s);
        }
        send(string);
    }

    /**
     * Gets the scoreboard of the player
     *
     * @return the {@link FastBoard} of the player
     */
    public FastBoard getBoard() {
        return board;
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
        highScores.put(player.getUniqueId(), score);
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
            builder.setItem(9, new ItemBuilder(Material.END_STONE, "&a&lParkour style")
                    .setLore("&7The style of your parkour.", "&7(which blocks will be used)", "", "&7Currently: &a" + style).build(), (t, e) -> {
                List<String> styles = Util.getNode(WITP.getConfiguration().getFile("config"), "styles.list");
                if (styles == null) {
                    Verbose.error("Error while trying to fetch possible styles from config.yml");
                    return;
                }
                int i = 0;
                for (String style : styles) {
                    if (i == 26) {
                        Verbose.error("There are too many styles to display!");
                        return;
                    }
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
                    builder2.setItem(26, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
                }
                builder2.build();
            });
        }
        builder.setItem(10, new ItemBuilder(Material.GLASS, "&a&lLead")
                .setLore("&7How many blocks will", "&7be generated ahead of you.", "", "&7Currently: &a" + blockLead + " blocks").build(), (t, e) -> {
            for (int i = 10; i < 17; i++) {
                builder1.setItem(i, new ItemBuilder(Material.PAPER, "&b&l" + (i - 9) + " block(s)").build(), (t2, e2) -> {
                    int amount = t2.getSlot() - 9;
                    blockLead = amount;
                    send("&7You selected a " + amount + " block lead.");
                    saveStats();
                });
            }
            builder1.setItem(26, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
            builder1.build();
        });
        builder.setItem(11, new ItemBuilder(Material.CLOCK, "&a&lTime")
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
            builder3.setItem(26, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
            builder3.build();
        });
        Material difficulty = useDifficulty ? Material.GREEN_WOOL : Material.RED_WOOL;
        String difficultyString = Boolean.toString(useDifficulty);
        String difficultyValue = Util.normalizeBoolean(Util.colorBoolean(difficultyString));
        builder.setItem(12, new ItemBuilder(difficulty, "&a&lUse difficulty")
                .setLore("&7If enabled having a higher score will mean", "&7the parkour becomes more difficult.", "",
                        "&7Currently: " + difficultyValue).build(), (t2, e2) -> {
                    useDifficulty = !useDifficulty;
                    send("&7You changed your usage of difficulty to " +
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
            send("&7You changed your usage of particles to " +
                    Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(particlesString))));
            saveStats();
            player.closeInventory();
        });
        Material deathMsg = showDeathMsg ? Material.GREEN_WOOL : Material.RED_WOOL;
        String deathString = Boolean.toString(showDeathMsg);
        String deathValue = Util.normalizeBoolean(Util.colorBoolean(deathString));
        builder.setItem(15, new ItemBuilder(deathMsg, "&a&lShow fall message & scoreboard")
                .setLore("&7If enabled shows a message when you fall", "&7with extra info and the scoreboard", "",
                        "&7Currently: " + deathValue).build(), (t2, e2) -> {
            showDeathMsg = !showDeathMsg;
            if (showDeathMsg) {
                board = new FastBoard(player);
                updateScoreboard();
            } else {
                board.delete();
            }
            send("&7You changed your showing of the fall message and scoreboard to " +
                    Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(deathString))));
            saveStats();
            player.closeInventory();
        });
        Material special = useSpecial ? Material.GREEN_WOOL : Material.RED_WOOL;
        String specialString = Boolean.toString(useSpecial);
        String specialValue = Util.normalizeBoolean(Util.colorBoolean(specialString));
        builder.setItem(16, new ItemBuilder(special, "&a&lUse special blocks")
                .setLore("&7If enabled uses special blocks like ice and slabs.", "",
                        "&7Currently: " + specialValue).build(), (t2, e2) -> {
            useSpecial = !useSpecial;
            send("&7You changed your usage of special blocks to " +
                    Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(specialString))));
            saveStats();
            player.closeInventory();
        });
        Material structures = useStructure ? Material.GREEN_WOOL : Material.RED_WOOL;
        String structuresString = Boolean.toString(useStructure);
        String structuresValue = Util.normalizeBoolean(Util.colorBoolean(structuresString));
        builder.setItem(17, new ItemBuilder(structures, "&a&lUse structures")
                .setLore("&7If enabled static structures", "&7will appear throughout the parkour.", "",
                        "&7Currently: " + structuresValue).build(), (t2, e2) -> {
                    useStructure = !useStructure;
                    send("&7You changed your usage of structures to " +
                            Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(structuresString))));
                    saveStats();
                    player.closeInventory();
        });
        Integer score = highScores.get(player.getUniqueId());
        builder.setItem(22, new ItemBuilder(Material.GOLD_BLOCK, "&6&lLeaderboard")
                .setLore("&7Your rank: &f#" + getRank(player.getUniqueId()) + " &7(" + (score == null ? 0 : score) + ")").build(), (t2, e2) -> {
            scoreboard(1);
            player.closeInventory();
        });
        builder.setItem(26, new ItemBuilder(Material.BARRIER, "&4&lQuit").build(), (t2, e2) -> {
            try {
                ParkourPlayer.unregister(this, true);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to quit player " + player.getName());
            }
        });
        builder.setItem(25, new ItemBuilder(Material.ARROW, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
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
     * Shows the scoreboard (as a chat message)
     */
    public void scoreboard(int page) {
        if (highScores.size() == 0) {
            try {
                fetchHighScores();
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to fetch the high scores!");
            }
        }

        int lowest = page * 10;
        int highest = (page - 1) * 10;
        if (page < 1) {
            return;
        }
        if (page > 1 && highest > highScores.size()) {
            return;
        }

        HashMap<UUID, Integer> sorted = Util.sortByValue(highScores);
        highScores = sorted;
        List<UUID> uuids = new ArrayList<>(sorted.keySet());

        send("", "", "", "", "", "", "", "");
        send("&7----------------------------------------");
        for (int i = highest; i < lowest; i++) {
            if (i == uuids.size()) {
                break;
            }
            UUID uuid = uuids.get(i);
            if (uuid == null) {
                continue;
            }
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            int rank = i + 1;
            send("&c#" + rank + ". &7" + name + " &f- " + highScores.get(uuid));
        }
        send("&7Your rank: &f#" + getRank(player.getUniqueId()) + " &7(" + highScores.get(player.getUniqueId()) + ")");
        send("");

        int prevPage = page - 1;
        int nextPage = page + 1;
        BaseComponent[] previous = new ComponentBuilder()
                .append("<< Previous page").color(net.md_5.bungee.api.ChatColor.RED)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/witp leaderboard " + prevPage))
                .append(" | ").color(net.md_5.bungee.api.ChatColor.GRAY)
                .event((ClickEvent) null)
                .append("Next page >>").color(net.md_5.bungee.api.ChatColor.RED)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/witp leaderboard " + nextPage))
                .create();

        player.spigot().sendMessage(previous);
        send("&7----------------------------------------");
    }

    /**
     * Gets the high score of a player
     *
     * @param   player
     *          The player
     *
     * @return the high score of the player
     */
    public static int getHighScore(@NotNull UUID player) {
        return highScores.get(player);
    }

    /**
     * Gets the player at a certain place
     * Note: places are indicated in normal fashion (a.k.a. #1 is the first)
     *
     * @param   place
     *          The place
     *
     * @return the player at that place
     */
    public static UUID getAtPlace(int place) {
        return new ArrayList<>(highScores.keySet()).get(place);
    }

    private int getRank(UUID player) {
        return new ArrayList<>(highScores.keySet()).indexOf(player) + 1;
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
                if (from.showDeathMsg == null) {
                    from.showDeathMsg = true;
                }
                if (from.useSpecial == null) {
                    from.useSpecial = true;
                }
                if (from.useStructure == null) {
                    from.useStructure = true;
                }
                ParkourPlayer pp = new ParkourPlayer(player, from.highScore, from.time, from.style, from.blockLead,
                        from.useParticles, from.useDifficulty, from.useStructure, from.useSpecial, from.showDeathMsg);
                pp.save();
                players.put(player, pp);
                reader.close();
                return pp;
            } else {
                ParkourPlayer pp = new ParkourPlayer(player, 0, "Day",
                        WITP.getConfiguration().getString("config", "styles.default"),
                        4, true, true, true, true, true);
                players.put(player, pp);
                pp.save();
                return pp;
            }
        }
        return players.get(player);
    }

    // todo scoreboard
    /**
     * Gets the highscores of all player
     *
     * @throws  IOException
     *          When creating the file reader goes wrong
     */
    public static void fetchHighScores() throws IOException {
        File folder = new File(WITP.getInstance().getDataFolder() + "/players/");
        if (!(folder.exists())) {
            folder.mkdirs();
            return;
        }
        for (File file : folder.listFiles()) {
            FileReader reader = new FileReader(file);
            ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);
            String name = file.getName();
            highScores.put(UUID.fromString(name.substring(0, name.lastIndexOf('.'))), from.highScore);
        }
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

    /**
     * Gets the time from a string
     *
     * @param   time
     *          The time as a string
     *
     * @return the int value used to set the time
     */
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
    public static void unregister(@NotNull ParkourPlayer player, boolean sendBack) throws IOException {
        new PlayerLeaveEvent(player).call();
        player.generator.reset(false);
        if (!player.getBoard().isDeleted()) {
            player.getBoard().delete();
        }
        player.save();
        WITP.getDivider().leave(player);
        players.remove(player.getPlayer());
        if (sendBack) {
            if (WITP.getConfiguration().getFile("config").getBoolean("bungeecord.enabled")) {
                Util.sendPlayer(player.getPlayer(), WITP.getConfiguration().getString("config", "bungeecord.return_server"));
            } else {
                Player pl = player.getPlayer();
                pl.getInventory().clear();
                pl.teleport(player.getPreviousLocation());
                for (int slot : player.getPreviousInventory().keySet()) {
                    pl.getInventory().setItem(slot, player.getPreviousInventory().get(slot));
                }
                pl.resetPlayerTime();
            }
        }
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

    public HashMap<Integer, ItemStack> getPreviousInventory() {
        return previousInventory;
    }

    public Location getPreviousLocation() {
        return previousLocation;
    }
}
