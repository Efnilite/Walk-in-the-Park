package dev.efnilite.witp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.task.Tasks;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wrapper class for regular player to store plugin-usable data
 */
public class ParkourPlayer {

    public @Expose int mmr;
    public @Expose int blockLead;
    public @Expose boolean useDifficulty;
    public @Expose boolean useStructures;
    public @Expose String style;

    public int points;
    public UUID openInventory;
    private List<Material> possibleStyle;
    private final File file;
    private final Player player;
    private final ParkourGenerator generator;
    private final FastBoard board;
    private static final HashMap<Player, ParkourPlayer> players = new HashMap<>();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link #register(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, int mmr, String style, int blockLead, boolean useDifficulty, boolean useStructures) {
        this.mmr = mmr;
        this.blockLead = blockLead;
        this.style = style;
        this.useDifficulty = useDifficulty;
        this.useStructures = useStructures;

        this.points = 0;
        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + player.getUniqueId().toString() + ".json");
        this.player = player;
        this.possibleStyle = new ArrayList<>();
        this.generator = new ParkourGenerator(this);
        this.board = new FastBoard(player);
        updateScoreboard();

        setStyle(style);
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

    public void menu() {
        InventoryBuilder builder = new InventoryBuilder(this, 3, "Customize").open();
        InventoryBuilder builder1 = new InventoryBuilder(this, 3, "Lead").open();
        InventoryBuilder builder2 = new InventoryBuilder(this, 3, "Parkour style").open();

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
                        Verbose.error("Possible materials is null");
                        return;
                    }
                    Material material = possible.get(possible.size() - 1);
                    builder2.setItem(i, new ItemBuilder(material, "&c" + Util.capitalizeFirst(style)).build(), (t2, e2) -> {
                        String selected = ChatColor.stripColor(e2.getItemMeta().getDisplayName()).toLowerCase();
                        this.setStyle(selected);
                        this.send("&7You selected style &c" + selected + "&7!");
                        this.saveStats();
                    });
                    i++;
                    builder2.setItem(22, new ItemBuilder(Material.BARRIER, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
                }
                builder2.build();
            });
        }
        builder.setItem(12, new ItemBuilder(Material.GLASS, "&a&lLead")
                .setLore("&7How many blocks will", "&7be generated ahead of you.", "", "&7Currently: &a" + blockLead + " blocks").build(), (t, e) -> {
            for (int i = 1; i < 8; i++) {
                builder1.setItem(i + 9, new ItemBuilder(Material.ANVIL, "&b&l" + i + " block(s)").build(), (t2, e2) -> {
                    int amount = t2.getSlot() - 9;
                    this.blockLead = amount;
                    this.send("&7You selected a " + amount + " block lead.");
                    this.saveStats();
                }); // todo number as head
            }
            builder1.setItem(22, new ItemBuilder(Material.BARRIER, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
            builder1.build();
        });
        Material difficulty = useDifficulty ? Material.GREEN_WOOL : Material.RED_WOOL;
        String difficultyString = Boolean.toString(useDifficulty);
        String difficultyValue = Util.normalizeBoolean(Util.colorBoolean(difficultyString));
        builder.setItem(13, new ItemBuilder(difficulty, "&a&lUse difficulty")
                .setLore("&7If enabled having a higher score will mean", "&7the parkour becomes more difficult.", "",
                        "&7Currently: " + difficultyValue).build(), (t2, e2) -> {
                    useDifficulty = !useDifficulty;
                    this.send("&7You changed your changed your usage of difficulty to " + Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(difficultyString))));
                    this.saveStats();
                    player.closeInventory();
        });
        Material structures = useStructures ? Material.GREEN_WOOL : Material.RED_WOOL;
        String structuresString = Boolean.toString(useStructures);
        String structuresValue = Util.normalizeBoolean(Util.colorBoolean(difficultyString));
        builder.setItem(14, new ItemBuilder(structures, "&a&lUse structures")
                .setLore("&7If enabled static structures", "&7will appear throughout the parkour.", "",
                        "&7Currently: " + structuresValue).build(), (t2, e2) -> {
                    useStructures = !useStructures;
                    this.send("&7You changed your changed your usage of structures to " + Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(structuresString))));
                    this.saveStats();
                    player.closeInventory();
        });
        builder.setItem(22, new ItemBuilder(Material.BARRIER, "&c&lClose").build(), (t2, e2) -> player.closeInventory());
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

    /**
     * Gets all possible blocks from a specific styles
     *
     * @return the possible blocks
     */
    public @Nullable List<Material> getPossibleMaterials(String style) {
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
                Verbose.error("Unknown material (" + material + ") in style " + style);
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
     * Sends a message or array of it
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
    public static void register(Player player) throws IOException {
        UUID uuid = player.getUniqueId();
        File data = new File(WITP.getInstance().getDataFolder() + "/players/" + uuid.toString() + ".json");
        if (data.exists()) {
            FileReader reader = new FileReader(data);
            ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);
            players.put(player, new ParkourPlayer(player, from.mmr, from.style, from.blockLead, from.useDifficulty, from.useStructures));
            reader.close();
        } else {
            ParkourPlayer pp = new ParkourPlayer(player, 0, "red", 4, true, true);
            players.put(player, pp);
            pp.save();
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
     * Unregisters a ParkourPlayer
     *
     * @param   player
     *          The ParkourPlayer
     *
     * @throws  IOException
     *          When saving the player's file goes wrong
     */
    public static void unregister(ParkourPlayer player) throws IOException {
        player.save();
        players.remove(player.getPlayer());
    }

    public ParkourGenerator getGenerator() {
        return generator;
    }

    public Player getPlayer() {
        return player;
    }
}
