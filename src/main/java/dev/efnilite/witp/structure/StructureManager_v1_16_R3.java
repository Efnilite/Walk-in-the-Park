package dev.efnilite.witp.structure;

import dev.efnilite.witp.util.Util;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class StructureManager_v1_16_R3 implements StructureManager {

    @Override
    public void paste(File file, Location to) {
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
    public Vector dimensions(File file, Location to) {
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