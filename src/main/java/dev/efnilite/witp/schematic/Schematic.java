package dev.efnilite.witp.schematic;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.schematic.selection.Dimensions;
import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
    private boolean read;

    /**
     * Stores values of location
     */
    private Dimensions dimensions;

    /**
     * The blocks if present
     */
    private List<SchematicBlock> blocks;

    /**
     * The file associated if present
     */
    private File file;

    /**
     * The constructor while creating a new schematic from 2 positions
     *
     * @param   pos1
     *          The first position
     *
     * @param   pos2
     *          The second position
     */
    public Schematic(@NotNull Location pos1, @NotNull Location pos2) {
        this.dimensions = new Dimensions(pos1, pos2);
        this.blocks = new ArrayList<>();
        this.read = false;
    }

    public Schematic(@NotNull Selection selection) {
        this.dimensions = new Dimensions(selection.getPos1(), selection.getPos2());
        this.blocks = new ArrayList<>();
        this.read = false;
    }

    public Schematic() {
        this.read = false;
    }

    public Schematic file(@NotNull String fileName) {
        File folder = new File(WITP.getInstance().getDataFolder(), "schematics");
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
    public void save() {
        Tasks.asyncTask(() -> {
            try {
                if (dimensions == null || blocks == null) {
                    Verbose.error("Data of schematic is null while trying to save!");
                    return;
                }

                for (Block currentBlock : Util.getBlocks(dimensions.getMaximumPoint(), dimensions.getMinimumPoint())) {
                    if (currentBlock.getType() == Material.AIR) { // skip air if enabled
                        continue;
                    }
                    Vector3D relativeOffset = Vector3D.fromBukkit(currentBlock.getLocation().subtract(dimensions.getMinimumPoint()).toVector());
                    blocks.add(new SchematicBlock(currentBlock, relativeOffset));
                }

                file.createNewFile();

                FileWriter writer = new FileWriter(file);
                String separator = System.lineSeparator();

                writer.write(dimensions.toString()); // write dimensions to first line
                writer.write(separator); // is basically an enter

                writer.write("*");
                writer.write(separator);

                HashSet<String> filtered = new HashSet<>();
                Map<String, Integer> palette = new HashMap<>();
                blocks.forEach(block -> filtered.add(block.getData().getAsString(true))); // get each of the block types

                int index = 0;
                for (String data : filtered) {
                    palette.put(data, index);
                    writer.write(index + ">" + data);
                    writer.write(separator);
                    index++;
                }

                writer.write("~");
                writer.write(separator);

                StringJoiner joiner = new StringJoiner("/");
                for (SchematicBlock block : blocks) {
                    String current = block.getData().getAsString();
                    String id = Integer.toString(palette.get(current));

                    joiner.add(id + block.getRelativePosition().toString()); // id(x,y,z) -> 3(2,3,-3)
                }

                writer.write(joiner.toString());
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while saving");
            }
        });
    }

    /**
     * Reads a Schematic from a file
     */
    public void read() {
        Verbose.verbose("Reading schematic " + file.getName() + "...");
        Tasks.time("individualSchemRead");
        if (read) {
            return;
        }
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            Verbose.error("File doesn't exist!");
            return;
        }
        this.read = true;
        List<String> lines = reader.lines().collect(Collectors.toList()); // read the lines of the file

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
                palette.put(Integer.parseInt(elements[0]), Bukkit.createBlockData(elements[1]));
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
            Vector3D vector = null;
            while (vectorMatcher.find()) {
                vector = Util.parseVector(vectorMatcher.group());
            }

            blocks.add(new SchematicBlock(palette.get(id), vector));
        }
        this.blocks = blocks;

        Vector3D readDimensions = Util.parseVector(lines.get(0));
        this.dimensions = new Dimensions(readDimensions.x, readDimensions.y, readDimensions.y);
        Verbose.verbose("Finished reading in " + Tasks.end("individualSchemRead") + "ms!");
    }

    public List<Block> paste(Location at, RotationAngle angle) {
        read();
        // update dimensions to match min location, giving you an idea where it will be pasted
        this.dimensions = new Dimensions(at, at.clone().add(dimensions.getDimensions().toBukkitVector()));

        Location min = dimensions.getMinimumPoint();
        List<Block> affectedBlocks = new ArrayList<>();
        for (SchematicBlock block : blocks) {
            Vector3D relativeOffset = block.getRelativePosition();
            relativeOffset = relativeOffset.rotateAround(angle);

            // all positions are saved to be relative to the minimum location
            Location pasteLocation = min.clone().add(relativeOffset.toBukkitVector());
            Block affectedBlock = pasteLocation.getBlock();
            affectedBlock.setBlockData(block.getData());
            affectedBlocks.add(affectedBlock);
        }
        return affectedBlocks;
    }

    /**
     * Pastes a Schematic at a location and with a certain angle, adjusted to be usable in parkour.
     * Todo -> optimize and clean up
     *
     * @param   at
     *          The location at which the Schematic will be pasted
     *
     * @param   angle
     *          The angle of the Schematic (0 is default)
     *
     * @return  A list of the affected blocks during the pasting
     *
     */
    public @Nullable List<Block> pasteAdjusted(Location at, RotationAngle angle) {
        read();
        this.dimensions = new Dimensions(at, at.clone().add(dimensions.getDimensions().toBukkitVector())); // update dimensions to match min location, giving you an idea where it will be pasted

        // -- Preparing for paste --

        Location min = dimensions.getMinimumPoint();
        Location other = null;
        Map<Location, BlockData> rotated = new HashMap<>();
        for (SchematicBlock block : blocks) { // go through blocks
            Vector3D relativeOffset = block.getRelativePosition().clone();
            relativeOffset = relativeOffset.rotateAround(angle);

            // all positions are saved to be relative to the minimum location
            Location pasteLocation = min.clone().add(relativeOffset.toBukkitVector());

            if (block.getData().getMaterial() == Material.LIME_WOOL) { // finds the lime wool pasting location
                other = pasteLocation.clone();
            }
            rotated.put(pasteLocation.clone(), block.getData()); // put the final locations back
        }

        if (other == null) { // if no lime wool
            Verbose.error("No lime wool found in file " + file.getName());
            return null;
        }

        // -- Pasting with angle --

        // the difference between the location of lime wool and `at` so lime wools of angles matches
        Vector difference = other.clone().subtract(at).toVector();

        // get the opposite angle of the one being pasted at
        RotationAngle opposite = RotationAngle.getFromInteger(angle.getOpposite());
        // turn it in a specific way
        Vector3D turn = Vector3D.fromBukkit(difference).defaultRotate(opposite);
        // add it to the difference
        difference.add(turn.toBukkitVector());

        List<Block> affectedBlocks = new ArrayList<>();
        Pattern pattern = Pattern.compile("facing=(\\w+)");
        for (Location location : rotated.keySet()) {
            // align block to where it will actually be set (final step)
            Block block = location.clone().add(0, difference.getBlockY(),0).subtract(difference).getBlock();
            // sets the block data

            String finalBlockData = rotated.get(location).getAsString();
            Matcher matcher = pattern.matcher(finalBlockData);
            while (matcher.find()) {
                String facing = matcher.group().replaceAll("facing=", "");
                if (facing.equals("up") || facing.equals("down")) {
                    break;
                }
                String updated = getFaceFromAngle(facing, angle);
                finalBlockData = finalBlockData.replaceAll(pattern.toString(), "facing=" + updated);
            }

            block.setBlockData(Bukkit.createBlockData(finalBlockData), true);
            affectedBlocks.add(block);
        }

        return affectedBlocks;
    }

    /**
     * Finds a Material in a schematic
     *
     * @param   material
     *          The material
     *
     * @return the {@link SchematicBlock} with this material
     */
    public SchematicBlock findFromMaterial(Material material) {
        read();
        for (SchematicBlock block : blocks) {
            if (block.getData().getMaterial() == material) {
                return block;
            }
        }
        return null;
    }

    private String getFaceFromAngle(String original, RotationAngle rotationAngle) {
        int angle = rotationAngle.getAngle();
        switch (original) {
            case "north":
                if (angle % 270 == 0) {
                    return "east";
                } else if (angle % 180 == 0) {
                    return "south";
                } else if (angle % 90 == 0) {
                    return "west";
                } else {
                    return "north";
                }
            case "west":
                if (angle % 270 == 0) {
                    return "north";
                } else if (angle % 180 == 0) {
                    return "east";
                } else if (angle % 90 == 0) {
                    return "south";
                } else {
                    return "west";
                }
            case "south":
                if (angle % 270 == 0) {
                    return "west";
                } else if (angle % 180 == 0) {
                    return "north";
                } else if (angle % 90 == 0) {
                    return "east";
                } else {
                    return "south";
                }
            case "east":
                if (angle % 270 == 0) {
                    return "south";
                } else if (angle % 180 == 0) {
                    return "west";
                } else if (angle % 90 == 0) {
                    return "north";
                } else {
                    return "east";
                }
            default:
                return "north";
        }
    }

    public Dimensions getDimensions() {
        read();
        return dimensions;
    }

    public File getFile() {
        return file;
    }
}