package dev.efnilite.ip.schematic;

import dev.efnilite.ip.IP;
import dev.efnilite.vilib.util.Colls;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SchematicConverter {

    public static void convert() {
        var toZip = IP.getInFolder("schematics")
                .listFiles((dir, name) -> name.contains("parkour-") || name.contains("spawn-island"));

        if (toZip == null) return;

        IP.log("Zipping files to backup");
        zipFiles(toZip, new File(IP.getInFolder("schematics"), "convert-backup.zip"));

        var comparator = Comparator
                .comparing(Vector::getZ)
                .thenComparing(Vector::getY)
                .thenComparing(Vector::getX);

        for (var file : toZip) {
            IP.log("Converting %s".formatted(file.getName()));

            try {
                var blockMap = read(file);

                var map = new TreeMap<Vector, BlockData>(comparator);
                map.putAll(blockMap);

            } catch (IOException | ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void zipFiles(File[] files, File zipFile) {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : files) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }

                    zos.closeEntry();
                }
            }
        } catch (IOException ex) {
            IP.logging().stack("Error while zipping files", ex);
        }
    }

    private static Map<Vector, BlockData> read(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            var version = (int) stream.readObject();
            var palette = (Map<String, Integer>) stream.readObject();
            var offsets = (Map<String, Integer>) stream.readObject();

            Map<Integer, BlockData> paletteRef = Colls.thread(palette).inverse()
                    .mapv((k, ov) -> Bukkit.createBlockData(ov)).get();

            // create final map by parse Map<String, Object> -> Vector and applying possible State
            return Colls.thread(offsets).mapkv(SchematicConverter::fromString, paletteRef::get).get();
        }
    }

    private static Vector fromString(String string) {
        String[] parts = string.split(",");
        return new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }
}
