package dev.efnilite.witp.util.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Auto-updates the config to remove current values (with possible exceptions)
 * (W.I.P.)
 * @author Efnilite
 */
public class BetaConfigUpdater {

    public static void update(Plugin plugin, String current, File toUpdate, String... ignore) throws IOException {
        update(plugin, current, toUpdate, Arrays.asList(ignore));
    }

    public static void update(Plugin plugin, String current, File toUpdate, List<String> ignore) throws IOException {
        InputStream resource = plugin.getResource(current);
        if (resource == null) {
            throw new IOException("InputStream is null");
        }

        YamlConfiguration oldFile = YamlConfiguration.loadConfiguration(toUpdate);
        Map<String, Object> oldValues = oldFile.getValues(true);
        for (String ig : ignore) {
            for (String old : new LinkedHashSet<>(oldValues.keySet())) {
                if (ig.startsWith(old)) {
                    oldValues.remove(old);
                }
            }
        }

//        File comparable = new File(plugin.getDataFolder() + "/new-" + toUpdate.getName());
//        write(resource, comparable);
//        YamlConfiguration temp = YamlConfiguration.loadConfiguration(comparable);
        for (String key : oldValues.keySet()) {
            oldFile.set(key, oldValues.get(key));
        }
        oldFile.options().copyDefaults(true);
        oldFile.save(toUpdate);

//        write(new FileInputStream(comparable), toUpdate);
//        comparable.delete();
    }

    private static void write(InputStream in, File to) throws IOException {
        OutputStream out = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
        out.close();
        in.close();
    }
}