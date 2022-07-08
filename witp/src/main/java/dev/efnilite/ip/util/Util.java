package dev.efnilite.ip.util;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.efnilite.ip.IP;
import dev.efnilite.ip.generator.Direction;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.schematic.VectorUtil;
import dev.efnilite.ip.schematic.selection.Dimensions;
import dev.efnilite.ip.schematic.selection.Selection;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.chat.Message;
import dev.efnilite.vilib.particle.ParticleData;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * General utilities
 *
 * @author Efnilite
 */
public class Util {

    private static Economy economy;
    private static final char[] OID = "1234567890abcdefghijklmnopqrstuvwxyz".toCharArray(); // Online IDentifier
    private static final char[] RANDOM_DIGITS = "1234567890".toCharArray();

    public static <T> T getRandom(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * Random ID for game logging
     *
     * @return a string with a random ID
     */
    public static String randomOID() {
        StringBuilder random = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            random.append(OID[ThreadLocalRandom.current().nextInt(OID.length - 1)]);
        }
        return random.toString();
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
     * Gets the direction from a facing (e.g. north, south, west)
     *
     * @param   face
     *          The string direction (north, south, east and west)
     *
     * @return a vector that indicates the direction
     */
    public static @NotNull Direction getDirection(@Nullable String face) {
        try {
            return Direction.valueOf(face == null ? "-" : face.toUpperCase());
        } catch (Throwable throwable) {
            IP.logging().stack(face + " is not a direction!", "check generation.yml for misinputs", throwable);
            return Direction.EAST;
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
    public static @Nullable List<String> getNode(FileConfiguration file, String path, boolean deep) {
        ConfigurationSection section = file.getConfigurationSection(path);
        if (section == null) {
            return null;
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
     * Gets the max of the locations
     *
     * @param   pos1
     *          The first location
     *
     * @param   pos2
     *          The second location
     *
     * @return  the max values of the locations
     */
    public static Location max(Location pos1, Location pos2) {
        World world = pos1.getWorld() == null ? pos2.getWorld() : pos1.getWorld();
        return new Location(world, Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    /**
     * Gets the min of the locations
     *
     * @param   pos1
     *          The first location
     *
     * @param   pos2
     *          The second location
     *
     * @return  the min values of the locations
     */
    public static Location min(Location pos1, Location pos2) {
        World world = pos1.getWorld() == null ? pos2.getWorld() : pos1.getWorld();
        return new Location(world, Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
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
     * Creates a string version of a Location.
     *
     * @param   location
     *          The location
     *
     * @return string version
     */
    public static String toString(Location location, boolean formatted) {
        if (!formatted) {
            return "(" + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getWorld().getName() + ")";
        } else {
            return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
        }
    }

    /**
     * Send a text to a player from the messages-v3.yml file in the default language
     * (if the player isn't a {@link ParkourUser}, knowing their preferred language is impossible)
     *
     * @param   sender
     *          The sender
     *
     * @param   path
     *          The path
     */
    public static void sendDefaultLang(CommandSender sender, String path, String... replaceable) {
        String message = IP.getConfiguration().getString("lang", "messages." + Option.DEFAULT_LOCALE + "." + path);
        for (String s : replaceable) {
            message = message.replaceFirst("%[a-z]", s);
        }
        Message.send(sender, message);
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

    /**
     * Returns the heading opposite to the axes with the smallest distance. Only works for the x and z-axis.
     *
     * @param   location
     *          The current location
     *
     * @param   selection
     *          The selection.
     *
     * @return the new heading
     */
    public static Direction opposite(Location location, Selection selection) {
        Dimensions dimensions = selection.getDimensions();
        Vector point = location.toVector();

        double[] distances = selection.distanceToBoundaries(location);
        double dx = distances[0]; // x distance from border
        double dz = distances[2]; // z distance from border

        Vector center = dimensions.getMinimumPoint().toVector(); // get the location of the min coordinates to act as O(0,0,0)
        Vector centerOffset = point.clone().subtract(center); // get the offset compared to center

        Vector mirroredPoint = point.clone(); // point mirrored on axis
        if (dx <= dz) {
            if (centerOffset.getX() > dimensions.getWidth()) { // if x is heading west, turn it around
                dx *= -1;
            }

            // turn away from x
            mirroredPoint.add(new Vector(dx, 0, 0)); // todo fix for directions to west
        } else {
            if (centerOffset.getZ() > dimensions.getLength()) { // if x is heading north, turn it around
                dz *= -1;
            }

            // turn away from z
            mirroredPoint.add(new Vector(0, 0, dz));
        }

        Vector toBorder = mirroredPoint.subtract(point).normalize(); // a' - a = AA'
        Vector opposite = toBorder.multiply(-1); // reverse vector
        return VectorUtil.fromVector(opposite);
    }

    public static boolean isLatest(String latest, String current) {
        int latestVs = Integer.parseInt(stripLatest(latest));
        int currentVs = Integer.parseInt(stripLatest(current));

        return latestVs <= currentVs;
    }

    private static String stripLatest(String string) {
        return string.toLowerCase().replace("v", "").replace(".", "");
    }

    public static <T> void box(BoundingBox box, @NotNull World world, ParticleData<T> data, double distanceBetween) {
        Location point1 = box.getMin().toLocation(world);
        Location point2;
        Location point3;
        Location point4;
        Location point5;
        Location point6;
        Location point7;
        Location point8;
        if (box.getWidthX() == 1.0 && box.getWidthZ() == 1.0) {
            point2 = point1.clone().add(box.getWidthX(), 0.0, 0.0);
            point3 = point2.clone().add(0.0, 0.0, box.getWidthZ());
            point4 = point1.clone().add(0.0, 0.0, box.getWidthZ());
            point5 = point1.clone().add(0.0, box.getHeight(), 0.0);
            point6 = point2.clone().add(0.0, box.getHeight(), 0.0);
            point7 = point3.clone().add(0.0, box.getHeight(), 0.0);
            point8 = point4.clone().add(0.0, box.getHeight(), 0.0);
        } else {
            point2 = point1.clone().add(box.getWidthX() + 1.0, 0.0, 0.0);
            point3 = point2.clone().add(0.0, 0.0, box.getWidthZ() + 1.0);
            point4 = point1.clone().add(0.0, 0.0, box.getWidthZ() + 1.0);
            point5 = point1.clone().add(0.0, box.getHeight() + 1.0, 0.0);
            point6 = point2.clone().add(0.0, box.getHeight() + 1.0, 0.0);
            point7 = point3.clone().add(0.0, box.getHeight() + 1.0, 0.0);
            point8 = point4.clone().add(0.0, box.getHeight() + 1.0, 0.0);
        }

        line(point1, point2, data, distanceBetween);
        line(point2, point3, data, distanceBetween);
        line(point3, point4, data, distanceBetween);
        line(point4, point1, data, distanceBetween);
        line(point5, point6, data, distanceBetween);
        line(point6, point7, data, distanceBetween);
        line(point7, point8, data, distanceBetween);
        line(point5, point8, data, distanceBetween);
        line(point1, point5, data, distanceBetween);
        line(point2, point6, data, distanceBetween);
        line(point3, point7, data, distanceBetween);
        line(point4, point8, data, distanceBetween);
    }

    public static <T> void line(Location one, Location two, ParticleData<T> data, double distanceBetween) {
        World world = one.getWorld();
        if (world == null) {
            throw new NullPointerException("World is null (Particles#draw)");
        } else {
            double dist = one.distance(two);
            Vector p1 = one.toVector();
            Vector p2 = two.toVector();
            Vector vec = p2.clone().subtract(p1).normalize().multiply(distanceBetween);
            world.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(), data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
            world.spawnParticle(data.getType(), p2.getX(), p2.getY(), p2.getZ(), data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
            double length = 0.0;

            while(length < dist) {
                world.spawnParticle(data.getType(), p1.getX(), p1.getY(), p1.getZ(), data.getSize(), data.getOffsetX(), data.getOffsetY(), data.getOffsetZ(), data.getSpeed(), data.getData());
                length += distanceBetween;
                p1.add(vec);
            }

        }
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