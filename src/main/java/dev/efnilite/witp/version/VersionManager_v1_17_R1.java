package dev.efnilite.witp.version;

import dev.efnilite.witp.generator.DefaultGenerator;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Main source used: https://github.com/Shynixn/StructureBlockLib/blob/master/structureblocklib-bukkit-core/bukkit-nms-117R1/src/main/java/com/github/shynixn/structureblocklib/bukkit/v1_17_R1/StructureWorldServiceImpl.java
 * @author Efnilite
 */
@Deprecated
public class VersionManager_v1_17_R1 implements VersionManager {

    @Override
    @Deprecated
    public void setWorldBorder(Player player, Vector vector, double size) {
//        WorldBorder border = new WorldBorder();
//        border.world = ((CraftWorld) player.getWorld()).getHandle();
//        border.setCenter(vector.getX(), vector.getZ());
//        border.setSize(size);
//        border.setWarningDistance(50);
//        border.setWarningTime(0);
//        ClientboundInitializeBorderPacket packet = new ClientboundInitializeBorderPacket(border);
//        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }

    @Override
    @Deprecated
    public void pasteStructure(File file, Location to) {
//        try {
//            DefinedStructure structure = new DefinedStructure();
//            structure.b(NBTCompressedStreamTools.a(new FileInputStream(file)));
//
//            WorldAccess world = ((CraftWorld) to.getWorld()).getHandle();
//            DefinedStructureInfo info = new DefinedStructureInfo().a(EnumBlockMirror.a).a(EnumBlockRotation.a)
//                    .a(false).c(false).a(ThreadLocalRandom.current());
//            StructureBoundingBox box = structure.b(info, new BlockPosition(to.getBlockX(), to.getBlockY(), to.getBlockZ()));
//            Location pos1 = new Location(to.getWorld(), box.g(), box.h(), box.i()); // box coords to bukkit
//            Location pos2 = new Location(to.getWorld(), box.j(), box.k(), box.l()); // box coords to bukkit
//
//            Location min = Util.min(pos1, pos2);
//            Location max = Util.max(pos1, pos2);
//
//            int deltaX = (max.getBlockX() - min.getBlockX()) / 2;
//            int deltaZ = (max.getBlockZ() - min.getBlockZ()) / 2;
//
//            BlockPosition pos = new BlockPosition(min.getBlockX() - deltaX, Math.min(box.h(), box.k()), min.getBlockZ() - deltaZ); // box.b and box.e = y coords
//
//            structure.a(world, pos, pos, info, new Random(), 2);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    // gets the appropriate rotation fo the schematic for the heading
    @Deprecated
    private void getRotation(@NotNull Vector heading) {
//        if (heading.getBlockZ() != 0) { // north/south
//            switch (heading.getBlockZ()) {
//                case 1: // south
//                    return EnumBlockRotation.c;
//                case -1: // north
//                    return EnumBlockRotation.a;
//            }
//        } else if (heading.getBlockX() != 0) { // east/west
//            switch (heading.getBlockX()) {
//                case 1: // east
//                    return EnumBlockRotation.b;
//                case -1: // west
//                    return EnumBlockRotation.d;
//            }
//        }
//        return EnumBlockRotation.d;
    }

    // todo (a lot of) optimizations
    @Override
    @Deprecated
    public @Nullable DefaultGenerator.StructureData placeAt(File file, Location to, Vector heading) {
//        DefinedStructure structure = new DefinedStructure();
//        try {
//            structure.b(NBTCompressedStreamTools.a(new FileInputStream(file)));
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return null;
//        }
//
//        WorldAccess world = ((CraftWorld) to.getWorld()).getHandle();
//        DefinedStructureInfo info = new DefinedStructureInfo().a(EnumBlockMirror.a).a(getRotation(heading))
//                .a(false).c(false).a(ThreadLocalRandom.current());
//        StructureBoundingBox box = structure.b(info, new BlockPosition(to.getBlockX(), to.getBlockY(), to.getBlockZ()));
//        Location pos1 = new Location(to.getWorld(), box.g(), box.h(), box.i()); // box coords to bukkit
//        Location pos2 = new Location(to.getWorld(), box.j(), box.k(), box.l()); // box coords to bukkit
//
//        // get min and max
//        Location min = Util.min(pos1, pos2);
//        Location max = Util.max(pos1, pos2);
//
//        //
//        int deltaX = (max.getBlockX() - min.getBlockX()) / 2; // gets the X dimension
//        int deltaZ = (max.getBlockZ() - min.getBlockZ()) / 2; // gets the Z dimension
//
//        Vector base = new Vector(min.getBlockX() - deltaX, Math.min(box.h(), box.k()), min.getBlockZ() - deltaZ);
//        BlockPosition pos = new BlockPosition(base.getBlockX(), base.getBlockY(), base.getBlockZ()); // box.b and box.e = y coords
//        List<DefinedStructure.BlockInfo> beginBlock = structure.a(pos, info, Blocks.bj);
//
//        Vector beginPos = null; // the green block relative to the beginning paste position
//        for (DefinedStructure.BlockInfo blockInfo : beginBlock) {
//            BlockPosition position = blockInfo.a;
//            beginPos = new Vector(position.getX(), position.getY(), position.getZ()).subtract(base);
//        }
//        if (beginPos == null) {
//            Verbose.error("There is no lime wool (start of parkour) in structure " + file.getName());
//            return null;
//        }
//
//        to = to.subtract(beginPos); // where the structure gets pasted from (top left corner)
//        BlockPosition finalPos = new BlockPosition(to.getX(), to.getY(), to.getZ());
//
//        structure.a(world, finalPos, finalPos, info, new Random(), 2);
//
//        List<Block> blocks = Util.getBlocks(max.subtract(beginPos), min.subtract(beginPos)); // subtraction to match the offset of it going to beginning position
//        Location endPos = null;
//        for (Block block : blocks) {
//            if (block.getType() == org.bukkit.Material.RED_WOOL) {
//                endPos = block.getLocation();
//            }
//        }
//        if (endPos == null) {
//            Verbose.error("There is no red wool (end of parkour) in structure " + file.getName());
//            for (Block block : blocks) {
//                block.setType(Material.AIR);
//            }
//            return null;
//        }
//
//        return new DefaultGenerator.StructureData(endPos.clone(), blocks);
        return null;
    }

    @Override
    @Deprecated
    public Vector getDimensions(File file, Location to) {
//        try {
//            DefinedStructure structure = new DefinedStructure();
//            structure.b(NBTCompressedStreamTools.a(new FileInputStream(file)));
//
//            DefinedStructureInfo info = new DefinedStructureInfo().a(EnumBlockMirror.a).a(EnumBlockRotation.a)
//                    .a(false).c(false).a(new Random());
//            StructureBoundingBox box = structure.b(info, new BlockPosition(to.getBlockX(), to.getBlockY(), to.getBlockZ()));
//            Location pos1 = new Location(to.getWorld(), box.g(), box.h(), box.i()); // box coords to bukkit
//            Location pos2 = new Location(to.getWorld(), box.j(), box.k(), box.l()); // box coords to bukkit
//
//            Location min = Util.min(pos1, pos2);
//            Location max = Util.max(pos1, pos2);
//
//            int deltaX = (max.getBlockX() - min.getBlockX());
//            int deltaZ = (max.getBlockZ() - min.getBlockZ());
//
//            return new Vector(deltaX + 1, max.subtract(min).getBlockY() + 1, deltaZ + 1);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
        return null;
    }
}