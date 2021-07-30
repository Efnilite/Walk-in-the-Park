package dev.efnilite.witp.schematic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.efnilite.witp.schematic.getter.AsyncBlockGetter;
import dev.efnilite.witp.schematic.queue.SchematicQueue;
import dev.efnilite.witp.schematic.selection.Selection;
import dev.efnilite.witp.schematic.selection.Dimensions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class that handles everything with schematics
 *
 * Taken from: Efnilite/Redaktor
 */
public class Schematic {

    /**
     * The gson instance used for reading/writing to JSON files
     */
    private final Gson gson;

    /**
     * The file
     */
    private final String file;

    /**
     * The selection required for the player
     */
    private final Selection selection;

    /**
     * Creates a new instance from a file
     *
     * @param   file
     *          The file
     */
    public Schematic(String file) {
        this.file = file.endsWith(".witp") ? file : file + ".witp";
        this.selection = null;
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Creates a new instance from a {@link Selection}
     *
     * @param   selection
     *          The selection
     */
    public Schematic(Selection selection) {
        this.file = null;
        this.selection = selection;
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Saves a Schematic to a file using a String.
     *
     * @param   file
     *          The file path where the Schematic should be saved.
     */
    public void save(String file) {
        if (selection != null) {
            SchematicWriter writer = new SchematicWriter(file);
            new AsyncBlockGetter(selection, l -> {
                try {
                    writer.write(l, selection.getDimensions());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            throw new IllegalArgumentException("Cuboid can't be null to save!");
        }
    }

    /**
     * Pastes a Schematic at a location.
     *
     * @param   at
     *          Where the location should be placed.
     *
     * @param   angle
     *          The angle the schematic will be pasted at.
     *          Must be divisible by 90, since you know.. Minecraft is square..
     *
     * @return  The CuboidSelection version of the pasted Schematic.
     *
     * @throws  IOException
     *          For any exceptions during the saving,
     *          this is targeted to the user so they can choose
     *          what to do with the error.
     */
    public Selection paste(Location at, int angle) throws IOException {
        if (file != null) {
            if (angle % 90 == 0) {
                SchematicReader reader = new SchematicReader(file);
                SchematicReader.ReaderReturn readerReturn = reader.read();
                Dimensions dimensions = readerReturn.getDimensions();
                List<BlockData> update = new ArrayList<>();
                SchematicQueue queue = new SchematicQueue(at);

                for (BlockData block : readerReturn.getData()) {
                    String data = block.getAsString();
                    Pattern pattern = Pattern.compile("facing=(\\w+)");

                    Facing facing;
                    Matcher matcher = pattern.matcher(data);
                    if (!matcher.find()) {
                        update.add(Bukkit.createBlockData(data));
                        continue;
                    }
                    String group = matcher.group().replaceAll("facing=", "");
                    facing = Facing.getFromBlockFace(BlockFace.valueOf(group.toUpperCase())).getFaceFromAngle(angle);

                    data = data.replaceAll(pattern.toString(), "facing=" + facing.getString());

                    update.add(Bukkit.createBlockData(data));
                }

                readerReturn.setData(update);
                queue.build(readerReturn);
                return new Selection(dimensions.getMaximumPoint(), dimensions.getMinimumPoint());
            } else {
                throw new IllegalArgumentException("Angle must be divisible by 90!");
            }
        } else {
            throw new IllegalArgumentException("File can't be null to save!");
        }
    }

    /**
     * Returns the selection of a schematic
     *
     * @return  The selection of a schematic
     *
     * @throws  IOException
     *          When something goes wrong with the reading
     */
    public Selection getCuboidSelection() throws IOException {
        return getDimensions().getSelection();
    }

    /**
     * Returns the dimensions of a schematic
     * Advice: use this once, don't call it repeatedly
     *
     * @return  The dimensions of a schematic
     *
     * @throws  IOException
     *          When something goes wrong with the reading
     */
    public Dimensions getDimensions() throws IOException {
        if (file != null) {
            SchematicReader reader = new SchematicReader(file);
            return reader.getDimensions();
        } else {
            throw new IllegalArgumentException("File can't be null!");
        }
    }

    /**
     * Gets the file
     *
     * @return the file
     */
    public String getFile() {
        return file;
    }

    /**
     * A class for calculating faces and angles
     *
     * All classes below this one are used for writing data to json files using gson, and they have
     * no user value.
     */
    private enum Facing {

        NORTH {
            @Override
            public String getString() {
                return "north";
            }
        },
        WEST {
            @Override
            public String getString() {
                return "west";
            }
        },
        SOUTH {
            @Override
            public String getString() {
                return "south";
            }
        },
        EAST {
            @Override
            public String getString() {
                return "east";
            }
        };

        public abstract String getString();

        public static Facing getFromBlockFace(BlockFace face) {
            switch (face) {
                case WEST:
                    return WEST;
                case SOUTH:
                    return SOUTH;
                case EAST:
                    return EAST;
                default:
                    return NORTH;
            }
        }

        public Facing getFaceFromAngle(int angle) {
            switch (this) {
                case NORTH:
                    if (angle % 270 == 0) {
                        return EAST;
                    } else if (angle % 180 == 0) {
                        return SOUTH;
                    } else if (angle % 90 == 0) {
                        return WEST;
                    } else {
                        return NORTH;
                    }
                case WEST:
                    if (angle % 270 == 0) {
                        return NORTH;
                    } else if (angle % 180 == 0) {
                        return EAST;
                    } else if (angle % 90 == 0) {
                        return SOUTH;
                    } else {
                        return WEST;
                    }
                case SOUTH:
                    if (angle % 270 == 0) {
                        return WEST;
                    } else if (angle % 180 == 0) {
                        return NORTH;
                    } else if (angle % 90 == 0) {
                        return EAST;
                    } else {
                        return SOUTH;
                    }
                case EAST:
                    if (angle % 270 == 0) {
                        return SOUTH;
                    } else if (angle % 180 == 0) {
                        return WEST;
                    } else if (angle % 90 == 0) {
                        return NORTH;
                    } else {
                        return EAST;
                    }
                default:
                    return NORTH;
            }
        }
    }
}