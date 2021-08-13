package dev.efnilite.witp.schematic;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.schematic.selection.Dimensions;
import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
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

    public boolean hasFile() {
        return file != null;
    }

    /**
     * Saves a schematic file
     */
    public void save() throws IOException {
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
        blocks.forEach(block -> filtered.add(block.getData().getAsString())); // get each of the block types

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
    }

    /**
     * Reads a Schematic from a file
     *
     */
    private void read() {
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
    }

    public List<Block> paste(Location at, int angle) throws IOException {
        read();
        this.dimensions = new Dimensions(at, at.clone().add(dimensions.getDimensions().toBukkitVector())); // update dimensions to match min location, giving you an idea where it will be pasted

        Location min = dimensions.getMinimumPoint();
        List<Block> affectedBlocks = new ArrayList<>();
        for (SchematicBlock block : blocks) {
            Vector3D relativeOffset = block.getRelativePosition();
            relativeOffset = relativeOffset.rotateAround(Vector3D.RotationAngle angle);

            Location pasteLocation = min.clone().add(relativeOffset); // all positions are saved to be relative to the minimum location
            Block affectedBlock = pasteLocation.getBlock();
            affectedBlock.setBlockData(block.getData());
            affectedBlocks.add(affectedBlock);
        }
        return affectedBlocks;
    }

    /**
     * Pastes a Schematic at a location and with a certain angle, adjusted to be usable in parkour
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
    public @Nullable List<Block> pasteAdjusted(Location at, Vector3D.RotationAngle angle) {
        read();
        this.dimensions = new Dimensions(at, at.clone().add(dimensions.getDimensions().toBukkitVector())); // update dimensions to match min location, giving you an idea where it will be pasted

        Location min = dimensions.getMinimumPoint();
        Location other = null;
        Map<Location, BlockData> rotated = new HashMap<>();
        for (SchematicBlock block : blocks) {
            Vector3D relativeOffset = block.getRelativePosition();
            relativeOffset = relativeOffset.rotateAround(angle);

            Location pasteLocation = min.clone().add(relativeOffset.toBukkitVector()); // all positions are saved to be relative to the minimum location

            if (block.getData().getMaterial() == Material.LIME_WOOL) {
                other = pasteLocation.clone();
                System.out.println(other);
            }
            rotated.put(pasteLocation.clone(), block.getData());
        }
        if (other == null) {
            Verbose.error("No lime wool found in file " + file.getName());
            return null;
        }

        System.out.println(at);

        Vector difference = other.clone().subtract(at).toVector();
        Vector3D.RotationAngle opposite = Vector3D.RotationAngle.getFromInteger(angle.getOpposite());
        Vector3D turn = Vector3D.fromBukkit(difference).defaultRotate(opposite);
        difference.add(turn.toBukkitVector());
        System.out.println(difference);
        List<Block> affectedBlocks = new ArrayList<>();
        for (Location location : rotated.keySet()) {
            Block block = location.clone().subtract(difference).getBlock();
            block.setBlockData(rotated.get(location));
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

    public Dimensions getDimensions() {
        read();
        return dimensions;
    }

    public File getFile() {
        return file;
    }
}