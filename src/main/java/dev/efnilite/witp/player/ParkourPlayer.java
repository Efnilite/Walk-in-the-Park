package dev.efnilite.witp.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.WITPAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.util.Option;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.inventory.DynamicInventory;
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wrapper class for a regular player to store plugin-usable data
 *
 * @author Efnilite
 */
public class ParkourPlayer extends ParkourUser {

    /**
     * Player data used in saving
     */
    public @Expose int highScore;
    public @Expose String highScoreTime;
    public @Expose int blockLead;
    public @Expose boolean useDifficulty;
    public @Expose Boolean useParticles;
    public @Expose Boolean useSpecial;
    public @Expose Boolean showDeathMsg;
    public @Expose Boolean showScoreboard;
    public @Expose Boolean useStructure;
    public @Expose String time;
    public @Expose String style;
    public @Expose String lang;
    public @Expose String name; // for fixing null in leaderboard

    private DefaultGenerator generator;
    private List<Material> possibleStyle;
    private final File file;
    public final HashMap<String, ParkourSpectator> spectators;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link WITPAPI#registerPlayer(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, int highScore, String time, String style, String name, String highScoreTime,
                         int blockLead, boolean useParticles, boolean useDifficulty, boolean useStructure, boolean useSpecial,
                         boolean showDeathMsg, boolean showScoreboard) {
        super(player);
        Verbose.verbose("Init of Player " + player.getName());
        this.highScoreTime = highScoreTime;
        this.useSpecial = useSpecial;
        this.showDeathMsg = showDeathMsg;
        this.highScore = highScore;
        this.blockLead = blockLead;
        this.style = style;
        this.useParticles = useParticles;
        this.time = time;
        this.useDifficulty = useDifficulty;
        this.useStructure = useStructure;
        this.showScoreboard = showScoreboard;
        this.spectators = new HashMap<>();
        this.name = name;

        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + player.getUniqueId().toString() + ".json");
        this.possibleStyle = new ArrayList<>();
        setStyle(style);

        player.setPlayerTime(getTime(time), false);
        WITP.getDivider().generate(this);
        updateScoreboard();
        if (player.isOp() && WITP.isOutdated) {
            send("&4&l!!! &fThe WITP plugin version you are using is outdated. Please check the Spigot page for updates.");
        }
    }

    public void setGenerator(DefaultGenerator generator) {
        this.generator = generator;
    }

    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            this.spectators.remove(spectator.getPlayer().getName());
        }
    }

    public void addSpectator(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            this.spectators.put(spectator.getPlayer().getName(), spectator);
        }
    }

    /**
     * Updates the stats for spectators
     */
    public void updateSpectators() {
        for (ParkourSpectator spectator : spectators.values()) {
            spectator.checkDistance();
            spectator.updateScoreboard();
        }
    }

    /**
     * Updates the scoreboard
     */
    @Override
    public void updateScoreboard() {
        if (showScoreboard && Option.SCOREBOARD) {
            board.updateTitle(Option.SCOREBOARD_TITLE);
            List<String> list = new ArrayList<>();
            List<String> lines = Option.SCOREBOARD_LINES;
            if (lines == null) {
                Verbose.error("Scoreboard lines are null! Check your config!");
                return;
            }
            Integer rank = getHighScore(player.getUniqueId());
            UUID one = getAtPlace(1);
            Integer top = 0;
            Highscore highscore = null;
            if (one != null) {
                top = getHighScore(one);
                highscore = scoreMap.get(one);
            }
            for (String s : lines) {
                list.add(s.replaceAll("%score%", Integer.toString(generator.score))
                        .replaceAll("%time%", generator.time)
                        .replaceAll("%highscore%", rank != null ? rank.toString() : "0")
                        .replaceAll("%topscore%", top != null ? top.toString() : "0")
                        .replaceAll("%topplayer%", highscore != null && highscore.name != null ? highscore.name : "N/A"));
            }

            board.updateLines(list);
        }
    }

    /**
     * Returns a random material from the possible styles
     * @see DefaultGenerator#generate()
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
    public void setHighScore(int score, String time) {
        this.highScore = score;
        highScoreTime = time;
        UUID uuid = player.getUniqueId();
        if (scoreMap.get(uuid) == null) {
            scoreMap.put(uuid, new Highscore(player.getName(), highScoreTime));
        } else {
            scoreMap.get(uuid).time = highScoreTime;
        }
        highScores.put(player.getUniqueId(), score);
        highScores = Util.sortByValue(highScores);
        saveStats();
    }

    /**
     * Opens the menu
     */
    public void menu() {
        InventoryBuilder builder = new InventoryBuilder(this, 3, "Customize").open();
        InventoryBuilder lead = new InventoryBuilder(this, 3, "Lead").open();
        InventoryBuilder styling = new InventoryBuilder(this, 3, "Parkour style").open();
        InventoryBuilder timeofday = new InventoryBuilder(this, 3, "Time").open();
        String close = getTranslated("item-close");
        boolean styles = WITP.getConfiguration().getFile("config").getBoolean("styles.enabled");
        boolean times = WITP.getConfiguration().getFile("config").getBoolean("time.enabled");

        int amount = 9;
        if (!styles) {
            amount--;
        }
        if (!times) {
            amount--;
        }
        DynamicInventory dynamic = new DynamicInventory(amount, 1);

        if (styles) {
            builder.setItem(dynamic.next(), new ItemBuilder(Material.END_STONE, "&a&lParkour style")
                    .setLore("&7The style of your parkour.", "&7(which blocks will be used)", "", "&7Currently: &a" + style).build(), (t, e) -> {
                        if (checkPermission("witp.style")) {
                            List<String> pos = Util.getNode(WITP.getConfiguration().getFile("config"), "styles.list");
                            if (pos == null) {
                                Verbose.error("Error while trying to fetch possible styles from config.yml");
                                return;
                            }
                            int i = 0;
                            for (String style : pos) {
                                if (i == 26) {
                                    Verbose.error("There are too many styles to display!");
                                    return;
                                }
                                List<Material> possible = this.getPossibleMaterials(style);
                                if (possible == null) {
                                    continue;
                                }
                                Material material = possible.get(possible.size() - 1);
                                styling.setItem(i, new ItemBuilder(material, "&b&l" + Util.capitalizeFirst(style)).build(), (t2, e2) -> {
                                    String selected = ChatColor.stripColor(e2.getItemMeta().getDisplayName()).toLowerCase();
                                    setStyle(selected);
                                    sendTranslated("selected-style", selected);
                                    saveStats();
                                });
                                i++;
                                styling.setItem(26, new ItemBuilder(Material.ARROW, close).build(), (t2, e2) -> menu());
                            }
                            styling.build();
                        }
            });
        }
        builder.setItem(dynamic.next(), new ItemBuilder(Material.GLASS, "&a&lLead")
                .setLore("&7How many blocks will", "&7be generated ahead of you.", "", "&7Currently: &a" + blockLead + " blocks").build(), (t, e) -> {
            if (checkPermission("witp.lead")) {
                for (int i = 10; i < 17; i++) {
                    lead.setItem(i, new ItemBuilder(Material.PAPER, "&b&l" + (i - 9) + " block(s)").build(), (t2, e2) -> {
                        blockLead = t2.getSlot() - 9;
                        sendTranslated("selected-block-lead", Integer.toString(blockLead));
                        saveStats();
                    });
                }
                lead.setItem(26, new ItemBuilder(Material.ARROW, close).build(), (t2, e2) -> menu());
                lead.build();
            }
        });
        if (times) {
            builder.setItem(dynamic.next(), new ItemBuilder(Material.CLOCK, "&a&lTime").setLore("&7The time of day.", "",
                    "&7Currently: &a" + time.toLowerCase()).build(), (t, e) -> {
                if (checkPermission("witp.time")) {
                    List<String> pos = Arrays.asList("Day", "Noon", "Dawn", "Night", "Midnight");
                    int i = 11;
                    for (String time : pos) {
                        timeofday.setItem(i, new ItemBuilder(Material.PAPER, "&b&l" + time).build(), (t2, e2) -> {
                            if (e2.getItemMeta() != null) {
                                String name = ChatColor.stripColor(e2.getItemMeta().getDisplayName());
                                this.time = name;
                                sendTranslated("selected-time", time.toLowerCase());
                                player.setPlayerTime(getTime(name), false);
                                saveStats();
                            }
                        });
                        i++;
                    }
                    timeofday.setItem(26, new ItemBuilder(Material.ARROW, close).build(), (t2, e2) -> menu());
                    timeofday.build();
                }
            });
        }
        Material difficulty = useDifficulty ? Material.GREEN_WOOL : Material.RED_WOOL;
        String difficultyString = Boolean.toString(useDifficulty);
        String difficultyValue = Util.normalizeBoolean(Util.colorBoolean(difficultyString));
        builder.setItem(dynamic.next(), new ItemBuilder(difficulty, "&a&lUse difficulty")
                .setLore("&7If enabled having a higher score will mean", "&7the parkour becomes more difficult.", "",
                        "&7Currently: " + difficultyValue).build(), (t2, e2) -> {
            if (checkPermission("witp.difficulty")) {
                useDifficulty = !useDifficulty;
                sendTranslated("selected-difficulty", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(difficultyString))));
                saveStats();
                menu();
            }
        });
        Material particles = useParticles ? Material.GREEN_WOOL : Material.RED_WOOL;
        String particlesString = Boolean.toString(useParticles);
        String particlesValue = Util.normalizeBoolean(Util.colorBoolean(particlesString));
        builder.setItem(dynamic.next(), new ItemBuilder(particles, "&a&lUse particles and sounds")
                .setLore("&7If enabled every generated block", "&7will show particles and play a sound.", "",
                        "&7Currently: " + particlesValue).build(), (t2, e2) -> {
            if (checkPermission("witp.particles")) {
                useParticles = !useParticles;
                sendTranslated("selected-particles", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(particlesString))));
                saveStats();
                menu();
            }
        });
        Material scoreboard = showScoreboard ? Material.GREEN_WOOL : Material.RED_WOOL;
        String scoreboardString = Boolean.toString(showScoreboard);
        String scoreboardValue = Util.normalizeBoolean(Util.colorBoolean(scoreboardString));
        builder.setItem(dynamic.next(), new ItemBuilder(scoreboard, "&a&lShow scoreboard")
                .setLore("&7If enabled shows the scoreboard", "",
                        "&7Currently: " + scoreboardValue).build(), (t2, e2) -> {
            if (checkPermission("witp.scoreboard")) {
                if (Option.SCOREBOARD) {
                    showScoreboard = !showScoreboard;
                    if (showScoreboard) {
                        board = new FastBoard(player);
                        updateScoreboard();
                    } else {
                        board.delete();
                    }
                    sendTranslated("selected-scoreboard", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(scoreboardString))));
                    saveStats();
                    menu();
                } else {
                    sendTranslated("cant-do");
                }
            }
        });
        Material deathMsg = showDeathMsg ? Material.GREEN_WOOL : Material.RED_WOOL;
        String deathString = Boolean.toString(showDeathMsg);
        String deathValue = Util.normalizeBoolean(Util.colorBoolean(deathString));
        builder.setItem(dynamic.next(), new ItemBuilder(deathMsg, "&a&lShow fall message")
                .setLore("&7If enabled shows a message when you fall", "&7with extra info", "",
                        "&7Currently: " + deathValue).build(), (t2, e2) -> {
            if (checkPermission("witp.fall")) {
                showDeathMsg = !showDeathMsg;
                sendTranslated("selected-fall-message", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(deathString))));
                saveStats();
                menu();
            }
        });
        Material special = useSpecial ? Material.GREEN_WOOL : Material.RED_WOOL;
        String specialString = Boolean.toString(useSpecial);
        String specialValue = Util.normalizeBoolean(Util.colorBoolean(specialString));
        builder.setItem(dynamic.next(), new ItemBuilder(special, "&a&lUse special blocks")
                .setLore("&7If enabled uses special blocks like ice and slabs.", "",
                        "&7Currently: " + specialValue).build(), (t2, e2) -> {
            if (checkPermission("witp.special")) {
                useSpecial = !useSpecial;
                sendTranslated("selected-special-blocks", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(specialString))));
                saveStats();
                menu();
            }
        });
        Material structures = useStructure ? Material.GREEN_WOOL : Material.RED_WOOL;
        String structuresString = Boolean.toString(useStructure);
        String structuresValue = Util.normalizeBoolean(Util.colorBoolean(structuresString));
        builder.setItem(dynamic.next(), new ItemBuilder(structures, "&a&lUse structures")
                .setLore("&7If enabled static structures", "&7will appear throughout the parkour.", "", "&7Currently: " + structuresValue).build(), (t2, e2) -> {
            if (checkPermission("witp.structures")) {
                useStructure = !useStructure;
                sendTranslated("selected-structures", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(structuresString))));
                saveStats();
                menu();
            }
        });
        builder.setItem(18, new ItemBuilder(Material.PAPER, "&c&lGamemode").build(), (t2, e2) -> {
            if (checkPermission("witp.gamemode")) {
                gamemode();
            }
        });
        Integer score = highScores.get(player.getUniqueId());
        builder.setItem(19, new ItemBuilder(Material.GOLD_BLOCK, "&6&lLeaderboard")
                .setLore(getTranslated("your-rank", Integer.toString(getRank(player.getUniqueId())), Integer.toString(score == null ? 0 : score)))
                .build(), (t2, e2) -> {
            if (checkPermission("witp.leaderboard")) {
                leaderboard(1);
                player.closeInventory();
            }
        });
        builder.setItem(26, new ItemBuilder(Material.BARRIER, getTranslated("item-quit")).build(), (t2, e2) -> {
            player.closeInventory();
            try {
                ParkourPlayer.unregister(this, true, true);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to quit player " + player.getName());
            }
        });
        builder.setItem(25, new ItemBuilder(Material.ARROW, close).build(), (t2, e2) -> player.closeInventory());
        builder.build();
    }

    private boolean checkPermission(String perm) {
        if (Option.PERMISSIONS) {
            boolean check = player.hasPermission(perm);
            if (!check) {
                sendTranslated("cant-do");
            }
            return check;
        }
        return true;
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
     * Gets the high score of a player
     *
     * @param   player
     *          The player
     *
     * @return the high score of the player
     */
    public static @Nullable Integer getHighScore(@NotNull UUID player) {
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
    public static @Nullable UUID getAtPlace(int place) {
        List<UUID> scores = new ArrayList<>(highScores.keySet());
        place--;
        if (scores.size() > place) {
            return scores.get(place);
        }
        return null;
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
                if (from.showScoreboard == null) {
                    from.showScoreboard = true;
                }
                if (from.name == null || !from.name.equals(player.getName())) {
                    from.name = player.getName();
                }
                if (from.highScoreTime == null) {
                    from.highScoreTime = "0.0s";
                }
                ParkourPlayer pp = new ParkourPlayer(player, from.highScore, from.time, from.style, from.name, from.highScoreTime, from.blockLead,
                        from.useParticles, from.useDifficulty, from.useStructure, from.useSpecial, from.showDeathMsg, from.showScoreboard);
                pp.save();
                players.put(player, pp);
                reader.close();
                return pp;
            } else {
                ParkourPlayer pp = new ParkourPlayer(player, 0, "Day", WITP.getConfiguration().getString("config", "styles.default"),
                        player.getName(), "0.0s", 4, true, true, true, true,
                        true, true);
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

    public void nullify() {
        generator = null;
    }

    /**
     * Gets the player's {@link DefaultGenerator}
     *
     * @return the ParkourGenerator associated with this player
     */
    public DefaultGenerator getGenerator() {
        return generator;
    }
}
