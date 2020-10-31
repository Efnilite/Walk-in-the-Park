package dev.efnilite.witp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.util.Verbose;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Wrapper class for regular player to store plugin-usable data
 */
public class ParkourPlayer {

    public int points;
    @Expose
    public short mmr;
    @Expose
    public short style;
    private final File file;
    private final Player player;
    private final ParkourGenerator generator;
    private static final HashMap<Player, ParkourPlayer> players = new HashMap<>();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();

    public ParkourPlayer(Player player, int mmr, short style) {
        this.mmr = (short) mmr;
        this.file = new File(WITP.getInstance().getDataFolder() + "/players/" + player.getUniqueId().toString() + ".json");
        this.points = 0;
        this.style = style;
        this.player = player;
        this.generator = new ParkourGenerator(this);
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
        Verbose.info("register");
        if (data.exists()) {
            Verbose.info("exist");
            FileReader reader = new FileReader(data);
            ParkourPlayer from = gson.fromJson(reader, ParkourPlayer.class);
            players.put(player, new ParkourPlayer(player, from.mmr, from.style));
            reader.close();
        } else {
            Verbose.info("no lol");
            ParkourPlayer pp = new ParkourPlayer(player, 0, (short) 1);
            players.put(player, pp);
            pp.save();
        }
    }

    public static @Nullable ParkourPlayer getPlayer(Player player) {
        for (Player p : players.keySet()) {
            if (p == player) {
                return players.get(p);
            }
        }
        return null;
    }

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
