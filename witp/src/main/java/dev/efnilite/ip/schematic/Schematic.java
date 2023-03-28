package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Task;
import dev.efnilite.vilib.util.Time;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Custom schematic type
 *
 * @author Efnilite
 */
public class Schematic {

    /**
     * If the schematic has been read already
     */
    public boolean read;

    /**
     * Signifies the bounding box of a schematic.
     * Array where the first item is the smallest location and second item is the largest.
     */
    private Location[] zone;

    public Vector dimensions;

    /**
     * The blocks if present
     */
    @Nullable
    public List<SchematicBlock> blocks;

    /**
     * The file associated if present
     */
    @Nullable
    public File file;

    /**
     * Whether this schematic is supported on the current version.
     * If this schematic features an unknown material it will be declared as unsupported.
     */
    public boolean isSupported;

    /**
     * The constructor while creating a new schematic from 2 positions
     *
     * @param pos1 The first position
     * @param pos2 The second position
     */
    public Schematic(@NotNull Location pos1, @NotNull Location pos2) {
        this.zone = new Location[]{Locations.min(pos1, pos2), Locations.max(pos1, pos2)};
        this.dimensions = zone[1].subtract(zone[0]).toVector();

        this.blocks = new ArrayList<>();
        this.read = false;
        this.isSupported = true;
    }

    public Schematic() {
        this.read = false;
        this.isSupported = true;
    }

    /**
     * Gets a schematic from its file name.
     *
     * @param fileName The name of the file
     * @return the instance of this class
     */
    public Schematic file(@NotNull String fileName) {
        File folder = IP.getInFolder("schematics");
        folder.mkdirs();

        fileName = fileName.endsWith(".witp") ? fileName : fileName + ".witp";
        file = new File(folder, fileName);

        return this;
    }

    /**
     * Gets a Schematic from a path and filename. Used for external schematics.
     *
     * @param path     The folder which the file is in
     * @param fileName The file name
     * @return the instance of his class
     */
    public Schematic file(@NotNull String path, @NotNull String fileName) {
        File folder = new File(path);
        folder.mkdirs();

        fileName = fileName.endsWith(".witp") ? fileName : fileName + ".witp";
        file = new File(folder, fileName);

        return this;
    }

    public String getName() {
        return file.getName();
    }

    public boolean hasFile() {
        return file != null;
    }

    /**
     * Saves a schematic file
     */
    public void save(@Nullable Player player) {
        Task.create(IP.getPlugin()).async().execute(() -> {
            try {
                Time.timerStart("save schematic %s".formatted(file.getName()));
                if (zone == null || blocks == null) {
                    IP.logging().error("Data of schematic is null while trying to save!");
                    return;
                }

                for (Block currentBlock : getBlocks(zone[0], zone[1])) {
                    if (currentBlock.getType() == Material.AIR) { // skip air if enabled
                        continue;
                    }
                    Vector relativeOffset = currentBlock.getLocation().subtract(zone[0]).toVector();
                    blocks.add(new SchematicBlock(currentBlock, relativeOffset));
                }

                FileWriter writer = new FileWriter(file);
                String separator = System.lineSeparator();

                // write dimensions to top of file
                writer.write(dimensions.toString());
                writer.write(separator); // enter

                // start palette symbol
                writer.write("*");
                writer.write(separator); // enter

                // filtered to prevent double entries in palette
                HashSet<String> filtered = new HashSet<>();
                Map<String, Integer> palette = new HashMap<>();
                blocks.forEach(block -> filtered.add(block.data.getAsString(true))); // get each of the block types

                // write palette as 0>minecraft:block[data]
                int index = 0;
                for (String data : filtered) {
                    palette.put(data, index);
                    writer.write(index + ">" + data);
                    writer.write(separator); // enter
                    index++;
                }

                // begin vector map
                writer.write("~");
                writer.write(separator); // enter

                // write all vectors per block type
                StringJoiner joiner = new StringJoiner("/");
                for (SchematicBlock block : blocks) {
                    String current = block.data.getAsString();
                    String id = Integer.toString(palette.get(current));

                    Vector rel = block.relativePosition;
                    joiner.add(id + "(" + rel.toString() + ")"); // id(x,y,z) -> 3(2,3,-3)
                }

                // finish
                writer.write(joiner.toString());
                writer.flush();
                writer.close();

                if (player == null) {
                    return;
                }
                Util.send(player, IP.PREFIX + "Your schematic has been saved in <red>" + Time.timerEnd("save schematic %s".formatted(file.getName())) + "ms<gray>!");
            } catch (IOException ex) {
                IP.logging().stack("Error while saving schematic " + file, ex);
            }
        }).run();
    }

    /**
     * Reads a Schematic from a file
     */
    public void read() {
        if (read) {
            return;
        }
        Time.timerStart("schematicRead");
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            lines = reader.lines().collect(Collectors.toList()); // read the lines of the file
        } catch (FileNotFoundException ex) {
            IP.logging().stack("Schematic file does not exist!", ex);
            return;
        } catch (IOException ex) {
            IP.logging().stack("Error while reading file!", ex);
            return;
        }
        read = true;

        // -- Makes palette --

        HashMap<Integer, BlockData> palette = new HashMap<>();
        boolean readingPalette = false; // palette is ? lines long
        for (String string : lines) { // reads the palette
            if (string.contains("*")) {
                readingPalette = true;
                continue;
            } else if (string.contains("~")) {
                break;
            }
            if (readingPalette) {
                String[] elements = string.split(">");

                BlockData data;
                try {
                    data = Bukkit.createBlockData(elements[1]); // if block data can't be created, check for legacy materials
                } catch (IllegalArgumentException ex) {
                    data = checkLegacyMaterials(elements[1], getName()); // if legacy materials can't find something, declare this schematic as unsupported
                }
                if (data == null) {
                    isSupported = false;
                    return;
                }

                palette.put(Integer.parseInt(elements[0]), data);
            }
        }

        String fileBlocks = lines.get(lines.size() - 1);
        String[] splitBlocks = fileBlocks.split("/");
        Pattern idPattern = Pattern.compile("^\\d+");
        Pattern vectorPattern = Pattern.compile("\\(-?\\d+,-?\\d+,-?\\d+\\)");

        // -- Writes it to the file and adds it to the blocks --

        List<SchematicBlock> blocks = new ArrayList<>();
        for (String block : splitBlocks) { // parse the SchematicBlocks

            Matcher idMatcher = idPattern.matcher(block); // finds the id
            int id = 0;
            while (idMatcher.find()) {
                id = Integer.parseInt(idMatcher.group());
            }

            Matcher vectorMatcher = vectorPattern.matcher(block);
            Vector vector = null;
            while (vectorMatcher.find()) {
                vector = VectorUtil.parseVector(vectorMatcher.group());
            }

            blocks.add(new SchematicBlock(palette.get(id), vector));
        }
        this.blocks = blocks;

        this.dimensions = VectorUtil.parseVector(lines.get(0));
    }

    private @Nullable BlockData checkLegacyMaterials(String full, String fileName) {
        IP.logging().info("Checking legacy materials for " + full);
        String[] split = full.split("\\[");
        String material = split[0];
        Material legacy = Material.matchMaterial(material, true);
        if (legacy == null) {
            IP.logging().error("Unknown material: " + material + " in " + fileName);
            return null;
        }
        if (split.length > 1) {
            return Bukkit.createBlockData(legacy, "[" + split[1]);
        }
        return Bukkit.createBlockData(legacy);
    }

    public List<Block> paste(Location at) {
        read();
        // update dimensions to match min location, giving you an idea where it will be pasted
        this.zone = new Location[]{at, at.clone().add(dimensions)};

        Location min = zone[0];
        List<Block> affectedBlocks = new ArrayList<>();
        for (SchematicBlock block : blocks) {
            Vector relativeOffset = block.relativePosition;

            if (relativeOffset == null) {
                IP.logging().error("Failed in reading schematic " + getName());
                IP.logging().error("Please delete your schematics folder and restart the server!");
                return affectedBlocks;
            }

            // all positions are saved to be relative to the minimum location
            Location pasteLocation = min.clone().add(relativeOffset);
            Block affectedBlock = pasteLocation.getBlock();

            setBlock(affectedBlock, block.data);
            affectedBlocks.add(affectedBlock);
        }
        return affectedBlocks;
    }

    /**
     * Pastes a Schematic at a location and with a certain angle, adjusted to be usable in parkour.
     * todo: optimize/clean up, use matrices and get the initial heading to support for multiple directions, prepare for v3 schematics
     *
     * @param at    The location at which the Schematic will be pasted
     * @param angle The angle of the Schematic (0 is default)
     * @return A list of the affected blocks during the pasting
     */
    public @Nullable List<Block> pasteAdjusted(Location at, SchematicAdjuster.RotationAngle angle) {
        read();
        // update dimensions to match min location, giving you an idea where it will be pasted
        this.zone = new Location[]{at, at.clone().add(dimensions)};

        // -- Preparing for paste --

        Location min = zone[0];
        Location other = null;
        Map<Location, BlockData> rotated = new HashMap<>();
        for (SchematicBlock block : blocks) { // go through blocks
            Vector relativeOffset = block.relativePosition.clone();
            relativeOffset = VectorUtil.rotateAround(relativeOffset, angle);

            // all positions are saved to be relative to the minimum location
            Location pasteLocation = min.clone().add(relativeOffset);

            if (block.data.getMaterial() == Material.LIME_WOOL) { // finds the lime wool pasting location
                other = pasteLocation.clone();
            }
            rotated.put(pasteLocation.clone(), block.data); // put the final locations back
        }

        if (other == null) { // if no lime wool
            IP.logging().error("No lime wool found in file " + file.getName());
            return null;
        }

        // -- Pasting with angle --
        // todo optimize

        // the difference between the location of lime wool and `at` so lime wools of angles matches
        Vector difference = other.clone().subtract(at).toVector();

        // get the opposite angle of the one being pasted at
        SchematicAdjuster.RotationAngle opposite = SchematicAdjuster.RotationAngle.getFromInteger(angle.opposite);
        // turn it in a specific way
        Vector turn = VectorUtil.defaultRotate(difference, opposite);
        // add it to the difference
        difference.add(turn);

        Pattern pattern = Pattern.compile("facing=(\\w+)");

        List<Block> affectedBlocks = new ArrayList<>();
        Map<Block, BlockData> toBeSet = new HashMap<>();

        for (Location location : rotated.keySet()) {
            // align block to where it will actually be set (final step)
            Block block = location.clone().add(0, difference.getBlockY(), 0).subtract(difference).getBlock();
            // sets the block data

            String finalBlockData = rotated.get(location).getAsString();
            if (finalBlockData.contains("facing")) {
                Matcher matcher = pattern.matcher(finalBlockData);
                while (matcher.find()) {
                    String facing = matcher.group().replace("facing=", "");
                    if (facing.equals("up") || facing.equals("down")) {
                        break;
                    }
                    String updated = getFaceFromAngle(facing, angle);
                    finalBlockData = finalBlockData.replaceAll(pattern.toString(), "facing=" + updated);
                }
            }

            BlockData blockData = Bukkit.createBlockData(finalBlockData);

            if (block.getType().isBlock()) { // first solid blocks, then things which go on walls, beds, etc.
                setBlock(block, blockData);
            } else {
                toBeSet.put(block, blockData);
            }

            affectedBlocks.add(block);
        }

        for (Block block : toBeSet.keySet()) { // set torches, etc.
            setBlock(block, toBeSet.get(block));
        }

        return affectedBlocks;
    }

    private void setBlock(Block block, BlockData data) {
        if (data instanceof Fence || data instanceof Wall) {
            block.setType(data.getMaterial(), true);
        } else {
            block.setBlockData(data);
        }
    }

    /**
     * Finds a Material in a schematic
     *
     * @param material The material
     * @return the {@link SchematicBlock} with this material
     */
    public SchematicBlock findFromMaterial(Material material) {
        read();
        for (SchematicBlock block : blocks) {
            if (block.data.getMaterial() == material) {
                return block;
            }
        }
        return null;
    }

    private String getFaceFromAngle(String original, SchematicAdjuster.RotationAngle rotationAngle) {
        int angle = rotationAngle.angle;
        switch (original) {
            case "north" -> {
                if (angle % 270 == 0) {
                    return "east";
                } else if (angle % 180 == 0) {
                    return "south";
                } else if (angle % 90 == 0) {
                    return "west";
                } else {
                    return "north";
                }
            }
            case "west" -> {
                if (angle % 270 == 0) {
                    return "north";
                } else if (angle % 180 == 0) {
                    return "east";
                } else if (angle % 90 == 0) {
                    return "south";
                } else {
                    return "west";
                }
            }
            case "south" -> {
                if (angle % 270 == 0) {
                    return "west";
                } else if (angle % 180 == 0) {
                    return "north";
                } else if (angle % 90 == 0) {
                    return "east";
                } else {
                    return "south";
                }
            }
            case "east" -> {
                if (angle % 270 == 0) {
                    return "south";
                } else if (angle % 180 == 0) {
                    return "west";
                } else if (angle % 90 == 0) {
                    return "north";
                } else {
                    return "east";
                }
            }
            default -> {
                return "north";
            }
        }
    }

    public List<Block> getBlocks(Location minL, Location maxL) {
        World w = minL.getWorld();
        List<Block> add = new ArrayList<>();
        Location location = new Location(w, 0, 0, 0);

        for (int x = minL.getBlockX(); x <= maxL.getBlockX(); x++) {
            for (int y = minL.getBlockY(); y <= maxL.getBlockY(); y++) {
                for (int z = minL.getBlockZ(); z <= maxL.getBlockZ(); z++) {
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

    protected static class SchematicBlock {

        public final Vector relativePosition;
        public final BlockData data;

        public SchematicBlock(Block block, Vector relativePosition) {
            this.relativePosition = relativePosition;
            this.data = block.getBlockData();
        }

        public SchematicBlock(BlockData data, Vector relativePosition) {
            this.relativePosition = relativePosition;
            this.data = data;
        }
    }
}