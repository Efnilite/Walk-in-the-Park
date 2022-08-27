package dev.efnilite.ip.util;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.efnilite.ip.IP;
import dev.efnilite.vilib.chat.Message;
import dev.efnilite.vilib.vector.Vector3D;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * General utilities
 *
 * @author Efnilite
 */
public class Util {

    private static Economy economy;
    private static final char[] RANDOM_DIGITS = "1234567890".toCharArray();

    public static boolean listContains(List<String> list, String... strings) {
        for (String s : list) {
            for (String string : strings) {
                if (s.contains(string)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static <T> T getRandom(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * Random digits
     *
     * @return a string with an amount of random digits
     */
    public static String randomDigits(int amount) {
        StringBuilder random = new StringBuilder();
        for (int i = 0; i < amount; i++) {
            random.append(RANDOM_DIGITS[ThreadLocalRandom.current().nextInt(RANDOM_DIGITS.length - 1)]);
        }
        return random.toString();
    }

    /**
     * Gets the difficulty of a schematic according to schematics.yml
     *
     * @param   fileName
     *          The name of the file (parkour-x.nbt)
     *
     * @return the difficulty, ranging from 0 to 1
     */
    public static double getDifficulty(String fileName) {
        int index = Integer.parseInt(fileName.split("-")[1].replace(".witp", ""));
        return IP.getConfiguration().getFile("structures").getDouble("difficulty." + index);
    }

    public static String parseDifficulty(double difficulty) {
        if (difficulty > 1) {
            IP.logging().error("Invalid difficuly, above 1: " + difficulty);
            return "unknown";
        }
        if (difficulty <= 0.3) {
            return "easy";
        } else if (difficulty <= 0.5) {
            return "medium";
        } else if (difficulty <= 0.7) {
            return "hard";
        } else if (difficulty >= 0.8) {
            return "very hard";
        } else {
            return "unknown";
        }
    }

    /**
     * Deposits money to a player using Vault
     *
     * @param   player
     *          The player
     *
     * @param   amount
     *          The amount
     */
    public static void depositPlayer(Player player, double amount) {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            if (economy == null) {
                RegisteredServiceProvider<Economy> service = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (service != null) {
                    economy = service.getProvider();
                    economy.depositPlayer(player, amount);
                } else {
                    IP.logging().error("There was an error while trying to fetch the Vault economy!");
                }
            } else {
                economy.depositPlayer(player, amount);
            }
        }
    }

    /**
     * Sends a player to a BungeeCord server
     *
     * @param   player
     *          The player to be sent
     *
     * @param   server
     *          The server name
     */
    public static void sendPlayer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        try {
            player.sendPluginMessage(IP.getPlugin(), "BungeeCord", out.toByteArray());
        } catch (ChannelNotRegisteredException ex) {
            IP.logging().error("Tried to send " + player.getName() + " to server " + server + " but this server is not registered!");
            player.kickPlayer("There was an error while trying to move you to server " + server + ", please rejoin.");
        }
    }

    /**
     * Gets the size of a ConfigurationSection
     *
     * @param   file
     *          The file
     *
     * @param   path
     *          The path
     *
     * @return the size
     */
    public static @NotNull List<String> getNode(FileConfiguration file, String path, boolean deep) {
        ConfigurationSection section = file.getConfigurationSection(path);
        if (section == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(section.getKeys(deep));
    }

    /**
     * Color something
     */
    public static String color(String string) {
        if (!string.equals("")) {
            return Message.parseFormatting(string);
        }
        return string;
    }

    /**
     * Color a list of strings (uses and sign as color marker)
     *
     * @param   strings
     *          The string to be colored
     *
     * @return the strings, but colored
     */
    public static List<String> colorList(List<String> strings) {
        List<String> ret = new ArrayList<>();
        for (String string : strings) {
            ret.add(Util.color(string));
        }
        return ret;
    }

    /**
     * Gets the blocks between 2 locations
     *
     * @param   position
     *          The first position
     *
     * @param   position2
     *          The second position
     *
     * @return the locations of all the blocks between the positions
     */
    public static List<Block> getBlocks(Location position, Location position2) {
        World w = position.getWorld();
        List<Block> add = new ArrayList<>();
        Location location = new Location(w, 0, 0, 0);
        int max = Math.max(position.getBlockX(), position2.getBlockX());
        int mix = Math.min(position.getBlockX(), position2.getBlockX());
        int may = Math.max(position.getBlockY(), position2.getBlockY());
        int miy = Math.min(position.getBlockY(), position2.getBlockY());
        int maz = Math.max(position.getBlockZ(), position2.getBlockZ());
        int miz = Math.min(position.getBlockZ(), position2.getBlockZ());
        for (int x = mix; x <= max; x++) {
            for (int y = miy; y <= may; y++) {
                for (int z = miz; z <= maz; z++) {
                    location.setX(x);
                    location.setY(y);
                    location.setZ(z);

                    if (location.getBlock().getType() != Material.AIR) {
                        add.add(location.clone().getBlock());
                    }
                }
            }
        }
        return add;
    }

    /**
     * Gets the player's held item
     *
     * @param   player
     *          The player
     *
     * @return the player's held item
     */
    public static ItemStack getHeldItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.AIR ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }

    /**
     * Get a location from a string
     *
     * @param   location
     *          The string
     *
     * @return the location from the string
     */
    public static Location parseLocation(String location) {
        String[] values = location.replaceAll("[()]", "").replace(", ", " ").replace(",", " ").split(" ");
        World world = Bukkit.getWorld(values[3]);
        if (world == null) {
            IP.logging().error("Detected an invalid world: " + values[3]);
            return new Location(Bukkit.getWorlds().get(0), Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]));
        }
        return new Location(Bukkit.getWorld(values[3]), Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]));
    }

    /**
     * Returns whether the provided player is a Bedrock player.
     * This check is provided by Floodgate
     *
     * @param   player
     *          The player
     *
     * @return true if the player is a Bedrock player, false if not.
     */
    public static boolean isBedrockPlayer(Player player) {
        return IP.getFloodgateHook() != null && IP.getFloodgateHook().isBedrockPlayer(player);
    }

    /**
     * Gets a spiral
     *
     * @param   n
     *          The number of  value
     *
     * @return the coords of this value
     */
    // https://math.stackexchange.com/a/163101
    public static int[] spiralAt(int n) {
        n++; // one-index
        int k = (int) Math.ceil((Math.sqrt(n) - 1) / 2);
        int t = 2 * k + 1;
        int m = t * t;
        t--;

        if (n > m - t) {
            return new int[]{k - (m - n), -k};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k, -k + (m - n)};
        } else {
            m -= t;
        }

        if (n > m - t) {
            return new int[]{-k + (m - n), k};
        } else {
            return new int[]{k, k - (m - n - t)};
        }
    }

    public static double angle(Vector3D base, Vector3D other) {
        if (base == other) {
            return 0;
        }

        return Math.toDegrees(Math.atan2(base.z, base.x) - Math.atan2(other.z, other.x));
    }

    public static List<Integer> getEvenlyDistributedSlots(int amountInRow) {
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

    /**
     * Translates any placeholders found by PlaceholderAPI
     *
     * @param   player
     *          The player
     *
     * @param   string
     *          The string that will be translated
     *
     * @return the translated string
     */
    public static String translate(Player player, String string) {
        if (IP.getPlaceholderHook() == null) {
            return string;
        }
        return PlaceholderAPI.setPlaceholders(player, string);
    }
}