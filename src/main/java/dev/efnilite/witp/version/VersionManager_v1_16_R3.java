package dev.efnilite.witp.version;

import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Efnilite
 */
public class VersionManager_v1_16_R3 implements VersionManager {

    @Override
    public void setWorldBorder(Player player, Vector vector, double size) {
        WorldBorder border = new WorldBorder();
        border.world = ((CraftWorld) player.getWorld()).getHandle();
        border.setCenter(vector.getX(), vector.getZ());
        border.setSize(size);
        border.setWarningDistance(50);
        border.setWarningTime(0);
        PacketPlayOutWorldBorder packet = new PacketPlayOutWorldBorder(border, PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    @Override
    public void pasteStructure(File file, Location to) {
        try {
            DefinedStructure structure = new DefinedStructure();
            structure.b(NBTCompressedStreamTools.a(new FileInputStream(file)));

            World world = ((CraftWorld) to.getWorld()).getHandle();
            DefinedStructureInfo info = new DefinedStructureInfo().a(EnumBlockMirror.NONE).a(EnumBlockRotation.NONE)
                    .a(false).a((ChunkCoordIntPair) null).c(false).a(ThreadLocalRandom.current());
            StructureBoundingBox box = structure.b(info, new BlockPosition(to.getBlockX(), to.getBlockY(), to.getBlockZ()));
            Location pos1 = new Location(to.getWorld(), box.a, box.b, box.c); // box coords to bukkit
            Location pos2 = new Location(to.getWorld(), box.d, box.e, box.f); // box coords to bukkit

            Location min = Util.min(pos1, pos2);
            Location max = Util.max(pos1, pos2);

            int deltaX = (max.getBlockX() - min.getBlockX()) / 2;
            int deltaZ = (max.getBlockZ() - min.getBlockZ()) / 2;

            BlockPosition pos = new BlockPosition(min.getBlockX() - deltaX, Math.min(box.b, box.e), min.getBlockZ() - deltaZ); // box.b and box.e = y coords

            structure.a((WorldAccess) world, pos, info, ThreadLocalRandom.current());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @Nullable ParkourGenerator.StructureData placeAt(File file, Location to) {
        try {
            to = to.getBlock().getLocation().clone();
            DefinedStructure structure = new DefinedStructure();
            structure.b(NBTCompressedStreamTools.a(new FileInputStream(file)));

            World world = ((CraftWorld) to.getWorld()).getHandle();
            DefinedStructureInfo info = new DefinedStructureInfo().a(EnumBlockMirror.NONE).a(EnumBlockRotation.NONE)
                    .a(false).a((ChunkCoordIntPair) null).c(false).a(ThreadLocalRandom.current());
            StructureBoundingBox box = structure.b(info, new BlockPosition(to.getBlockX(), to.getBlockY(), to.getBlockZ()));
            Location pos1 = new Location(to.getWorld(), box.a, box.b, box.c); // box coords to bukkit
            Location pos2 = new Location(to.getWorld(), box.d, box.e, box.f); // box coords to bukkit

            Location min = Util.min(pos1, pos2);
            Location max = Util.max(pos1, pos2);

            int deltaX = max.getBlockX() - min.getBlockX();
            int deltaZ = max.getBlockZ() - min.getBlockZ();

            Vector base = new Vector(min.getBlockX() - deltaX, Math.min(box.b, box.e), min.getBlockZ() - deltaZ);
            BlockPosition pos = new BlockPosition(base.getBlockX(), base.getBlockY(), base.getBlockZ());

            List<DefinedStructure.BlockInfo> beginBlock = structure.a(pos, info, Blocks.LIME_WOOL);
            Vector beginPos = null;
            for (DefinedStructure.BlockInfo blockInfo : beginBlock) {
                beginPos = new Vector(blockInfo.a.getX(), blockInfo.a.getY(), blockInfo.a.getZ()).clone().subtract(base);
            }
            if (beginPos == null) {
                Verbose.error("There is no lime wool (start of parkour) in structure " + file.getName());
                return null;
            }

            Vector location = to.clone().subtract(beginPos).toVector();
            structure.a((WorldAccess) world, new BlockPosition(location.getX(), location.getY(), location.getZ()), info, ThreadLocalRandom.current());

            List<DefinedStructure.BlockInfo> endBlock = structure.a(pos, info, Blocks.RED_WOOL);
            Location endPos = null;
            for (DefinedStructure.BlockInfo blockInfo : endBlock) {
                endPos = new Location(to.getWorld(), blockInfo.a.getX(), blockInfo.a.getY(), blockInfo.a.getZ());
            }
            if (endPos == null) {
                Verbose.error("There is no red wool (end of parkour) in structure " + file.getName());
                return null;
            }
            return new ParkourGenerator.StructureData(endPos, Util.getBlocks(min, max));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Vector getDimensions(File file, Location to) {
        try {
            DefinedStructure structure = new DefinedStructure();
            structure.b(NBTCompressedStreamTools.a(new FileInputStream(file)));

            DefinedStructureInfo info = new DefinedStructureInfo().a(EnumBlockMirror.NONE).a(EnumBlockRotation.NONE)
                    .a(false).a((ChunkCoordIntPair) null).c(false).a(new Random());
            StructureBoundingBox box = structure.b(info, new BlockPosition(to.getBlockX(), to.getBlockY(), to.getBlockZ()));
            Location pos1 = new Location(to.getWorld(), box.a, box.b, box.c); // box coords to bukkit
            Location pos2 = new Location(to.getWorld(), box.d, box.e, box.f); // box coords to bukkit

            Location min = Util.min(pos1, pos2);
            Location max = Util.max(pos1, pos2);

            int deltaX = (max.getBlockX() - min.getBlockX());
            int deltaZ = (max.getBlockZ() - min.getBlockZ());

            return new Vector(deltaX + 1, max.subtract(min).getBlockY() + 1, deltaZ + 1);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}