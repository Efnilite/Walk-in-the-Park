package dev.efnilite.witp.player;

import com.google.gson.annotations.Expose;
import dev.efnilite.witp.WITP;
import dev.efnilite.witp.api.WITPAPI;
import dev.efnilite.witp.generator.DefaultGenerator;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.config.Configuration;
import dev.efnilite.witp.util.config.Option;
import dev.efnilite.witp.util.fastboard.FastBoard;
import dev.efnilite.witp.util.inventory.InventoryBuilder;
import dev.efnilite.witp.util.inventory.ItemBuilder;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.ChatColor;
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

    private ParkourGenerator generator;
    private List<Material> possibleStyle;
    private final File file;
    public final HashMap<String, ParkourSpectator> spectators;

    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link WITPAPI#registerPlayer(Player)} instead
     */
    public ParkourPlayer(@NotNull Player player, @Nullable ParkourGenerator generator) {
        super(player);
        Verbose.verbose("Init of Player " + player.getName());
        this.spectators = new HashMap<>();
        this.generator = generator;

        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + player.getUniqueId().toString() + ".json");
        this.possibleStyle = new ArrayList<>();

        WITP.getDivider().generate(this);
        if (player.isOp() && WITP.OUTDATED) {
            send("&4&l!!! &fThe WITP plugin version you are using is outdated. Please check the Spigot page for updates.");
        }
    }

    public void setDefaults(int highScore, String time, String style, String name, String highScoreTime,
                            int blockLead, boolean useParticles, boolean useDifficulty, boolean useStructure, boolean useSpecial,
                            boolean showDeathMsg, boolean showScoreboard) {
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
        this.name = name;

        setStyle(style);
        player.setPlayerTime(getTime(time), false);
        updateScoreboard();
        if (generator instanceof DefaultGenerator) {
            ((DefaultGenerator) generator).generate(blockLead);
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
        Configuration config = WITP.getConfiguration();
        boolean styles = config.getFile("config").getBoolean("styles.enabled");
        boolean times = Option.TIME;
        boolean leadEnabled = Option.LEAD;
        ItemStack close = config.getFromItemData("general.close");

        int amount = 9;
        if (!styles) {
            amount--;
        }
        if (!times) {
            amount--;
        }
        if (!leadEnabled) {
            amount--;
        }
        InventoryBuilder.DynamicInventory dynamic = new InventoryBuilder.DynamicInventory(amount, 1);

        if (styles) {
            builder.setItem(dynamic.next(), config.getFromItemData("options.styles", style), (t, e) -> {
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
                        styling.setItem(26, close, (t2, e2) -> menu());
                    }
                    styling.build();
                }
            });
        }
        if (leadEnabled) {
            List<Integer> possible = Option.POSSIBLE_LEADS;
            InventoryBuilder.DynamicInventory dynamicLead = new InventoryBuilder.DynamicInventory(possible.size(), 1);
            builder.setItem(dynamic.next(), config.getFromItemData("options.lead", Integer.toString(blockLead)), (t, e) -> {
                if (checkPermission("witp.lead")) {
                    for (Integer integer : possible) {
                        lead.setItem(dynamicLead.next(), new ItemBuilder(Material.PAPER, "&b&l" + integer).build(), (t2, e2) -> {
                            if (e2.getItemMeta() != null) {
                                blockLead = Integer.parseInt(ChatColor.stripColor(e2.getItemMeta().getDisplayName()));
                                sendTranslated("selected-block-lead", Integer.toString(blockLead));
                                saveStats();
                            }
                        });
                    }
                    lead.setItem(26, close, (t2, e2) -> menu());
                    lead.build();
                }
            });
        }
        if (times) {
            builder.setItem(dynamic.next(), config.getFromItemData("options.time", time.toLowerCase()), (t, e) -> {
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
                    timeofday.setItem(26, close, (t2, e2) -> menu());
                    timeofday.build();
                }
            });
        }

        String difficultyString = Boolean.toString(useDifficulty);
        ItemStack item = config.getFromItemData("options.difficulty", Util.normalizeBoolean(Util.colorBoolean(difficultyString)));
        item.setType(useDifficulty ? Material.GREEN_WOOL : Material.RED_WOOL);
        builder.setItem(dynamic.next(), item, (t2, e2) -> {
            if (checkPermission("witp.difficulty")) {
                useDifficulty = !useDifficulty;
                sendTranslated("selected-difficulty", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(difficultyString))));
                saveStats();
                menu();
            }
        });

        String particlesString = Boolean.toString(useParticles);
        item = config.getFromItemData("options.particles", Util.normalizeBoolean(Util.colorBoolean(particlesString)));
        item.setType(useParticles ? Material.GREEN_WOOL : Material.RED_WOOL);
        builder.setItem(dynamic.next(), item, (t2, e2) -> {
            if (checkPermission("witp.particles")) {
                useParticles = !useParticles;
                sendTranslated("selected-particles", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(particlesString))));
                saveStats();
                menu();
            }
        });
        String scoreboardString = Boolean.toString(showScoreboard);
        item = config.getFromItemData("options.scoreboard", Util.normalizeBoolean(Util.colorBoolean(scoreboardString)));
        item.setType(showScoreboard ? Material.GREEN_WOOL : Material.RED_WOOL);
        builder.setItem(dynamic.next(), item, (t2, e2) -> {
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
        String deathString = Boolean.toString(showDeathMsg);
        item = config.getFromItemData("options.death-msg", Util.normalizeBoolean(Util.colorBoolean(deathString)));
        item.setType(showDeathMsg ? Material.GREEN_WOOL : Material.RED_WOOL);
        builder.setItem(dynamic.next(), item, (t2, e2) -> {
            if (checkPermission("witp.fall")) {
                showDeathMsg = !showDeathMsg;
                sendTranslated("selected-fall-message", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(deathString))));
                saveStats();
                menu();
            }
        });
        String specialString = Boolean.toString(useSpecial);
        item = config.getFromItemData("options.special", Util.normalizeBoolean(Util.colorBoolean(specialString)));
        item.setType(useSpecial ? Material.GREEN_WOOL : Material.RED_WOOL);
        builder.setItem(dynamic.next(), item, (t2, e2) -> {
            if (checkPermission("witp.special")) {
                useSpecial = !useSpecial;
                sendTranslated("selected-special-blocks", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(specialString))));
                saveStats();
                menu();
            }
        });
        String structuresString = Boolean.toString(useStructure);
        item = config.getFromItemData("options.structure", Util.normalizeBoolean(Util.colorBoolean(structuresString)));
        item.setType(useStructure ? Material.GREEN_WOOL : Material.RED_WOOL);
        builder.setItem(dynamic.next(), item, (t2, e2) -> {
            if (checkPermission("witp.structures")) {
                useStructure = !useStructure;
                sendTranslated("selected-structures", Util.normalizeBoolean(Util.colorBoolean(Util.reverseBoolean(structuresString))));
                saveStats();
                menu();
            }
        });
        builder.setItem(18, WITP.getConfiguration().getFromItemData("options.gamemode"), (t2, e2) -> {
            if (checkPermission("witp.gamemode")) {
                gamemode();
            }
        });
        Integer score = highScores.get(player.getUniqueId());
        builder.setItem(19, WITP.getConfiguration().getFromItemData("options.leaderboard",
                getTranslated("your-rank", Integer.toString(getRank(player.getUniqueId())),
                Integer.toString(score == null ? 0 : score))), (t2, e2) -> {
            if (checkPermission("witp.leaderboard")) {
                leaderboard(1);
                player.closeInventory();
            }
        });
        builder.setItem(26, WITP.getConfiguration().getFromItemData("general.quit"), (t2, e2) -> {
            player.closeInventory();
            try {
                ParkourPlayer.unregister(this, true, true);
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to quit player " + player.getName());
            }
        });
        builder.setItem(25, close, (t2, e2) -> player.closeInventory());
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
                ParkourPlayer pp = new ParkourPlayer(player, null);
                pp.setDefaults(from.highScore, from.time, from.style, from.name, from.highScoreTime, from.blockLead,
                        from.useParticles, from.useDifficulty, from.useStructure, from.useSpecial, from.showDeathMsg, from.showScoreboard);
                pp.save();
                players.put(player, pp);
                reader.close();
                return pp;
            } else {
                ParkourPlayer pp = new ParkourPlayer(player, null);
                pp.setDefaults(0, "Day", WITP.getConfiguration().getString("config", "styles.default"),
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
     * Gets the player's {@link ParkourGenerator}
     *
     * @return the ParkourGenerator associated with this player
     */
    public ParkourGenerator getGenerator() {
        return generator;
    }
}
