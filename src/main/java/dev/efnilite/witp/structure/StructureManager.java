package dev.efnilite.witp.structure;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.io.File;

public interface StructureManager {

    void paste(File file, Location to);

    Vector dimensions(File file, Location to);

}