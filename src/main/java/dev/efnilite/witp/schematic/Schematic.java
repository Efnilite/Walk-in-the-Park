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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Custom schematic type
 */
public class Schematic {

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
    }

    public Schematic(@NotNull Selection selection) {
        this.dimensions = new Dimensions(selection.getPos1(), selection.getPos2());
        this.blocks = new ArrayList<>();
    }

    public Schematic() {

    }

    public Schematic file(@NotNull String fileName) {
        File folder = new File(WITP.getInstance().getDataFolder(), "schematics");
        folder.mkdirs();
        fileName = fileName.endsWith(".witp") ? fileName : fileName + ".witp";
        file = new File(folder, fileName + ".witp");
        return this;
    }

    /**
     * Saves a schematic file
     *
     * @param   saveOptions
     *          The options while saving
     */
    public void save(@Nullable SaveOptions... saveOptions) throws IOException {
        if (dimensions == null || blocks == null) {
            Verbose.error("Data of schematic is null while trying to save!");
            return;
        }
        List<SaveOptions> options = Arrays.asList(saveOptions);

        for (Block currentBlock : Util.getBlocks(dimensions.getMaximumPoint(), dimensions.getMinimumPoint())) {
            if (options.contains(SaveOptions.SKIP_AIR) && currentBlock.getType() == Material.AIR) { // skip air if enabled
                continue;
            }
            Vector relativeOffset = currentBlock.getLocation().subtract(dimensions.getMinimumPoint()).toVector();
            blocks.add(new SchematicBlock(currentBlock, relativeOffset));
        }

        file.createNewFile();

        FileWriter writer = new FileWriter(file);
        String separator = System.lineSeparator();

        writer.write(dimensions.toString()); // write dimensions to first line
        writer.write(separator); // is basically an enter

        writer.write("*");
        writer.write(separator);

        List<String> filtered = new ArrayList<>();
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

        StringJoiner joiner = new StringJoiner("-");
        for (SchematicBlock block : blocks) {
            String current = block.getData().getAsString();
            String id = Integer.toString(palette.get(current));

            joiner.add(id + Util.toString(block.getRelativePosition(), true)); // id(x,y,z) -> 3(2,3,-3)
        }

        writer.write(joiner.toString());
        writer.flush();
        writer.close();
    }

    /**
     * Reads a Schematic from a file
     *
     * @return the blocks present in the Schematic
     */
    public Schematic read() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
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
                String[] elements = string.split(" > ");
                palette.put(Integer.parseInt(elements[0]), Bukkit.createBlockData(elements[1]));
            }
        }

        String fileBlocks = lines.get(lines.size() - 1);
        String[] splitBlocks = fileBlocks.split("-");
        Pattern idPattern = Pattern.compile("^\\d+"); // matches the id
        Pattern vectorPattern = Pattern.compile("\\(-?\\d+,-?\\d+,-?\\d+\\)"); // matches the vector

        List<SchematicBlock> blocks = new ArrayList<>();
        for (String block : splitBlocks) { // parse the SchematicBlocks
            Integer readId = Integer.parseInt(idPattern.matcher(block).group());
            BlockData blockData = palette.get(readId);
            Vector readVector = Util.parseVector(vectorPattern.matcher(block).group());

            blocks.add(new SchematicBlock(blockData, readVector));
        }
        this.blocks = blocks;

        Vector readDimensions = Util.parseVector(lines.get(0));
        this.dimensions = new Dimensions(readDimensions.getBlockX(), readDimensions.getBlockY(), readDimensions.getBlockZ());
        return this;
    }

    /**
     * Pastes a Schematic at a location and with a certain angle.
     *
     * @param   at
     *          The location at which the Schematic will be pasted
     *
     * @param   angle
     *          The angle of the Schematic (0 is default)
     *
     * @return  A list of the affected blocks during the pasting
     *
     * @throws IOException When something goes wrong during the reading of the file.
     */
    public List<Block> paste(Location at, int angle) throws IOException {
        read();
        this.dimensions = new Dimensions(at, at.clone().add(dimensions.getDimensions())); // update dimensions to match min location, giving you an idea where it will be pasted

        Location min = dimensions.getMinimumPoint();
        List<Block> affectedBlocks = new ArrayList<>();
        for (SchematicBlock block : blocks) {
            Vector relativeOffset = block.getRelativePosition();
            relativeOffset.rotateAroundY(angle);

            Location pasteLocation = min.clone().add(relativeOffset); // all positions are saved to be relative to the minimum location
            Block affectedBlock = pasteLocation.getBlock();
            affectedBlock.setBlockData(block.getData());
            affectedBlocks.add(affectedBlock);
        }
        return affectedBlocks;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public File getFile() {
        return file;
    }

    public enum SaveOptions {

        /**
         * Skips air in saving to massively reduce file size
         */
        SKIP_AIR

    }
}