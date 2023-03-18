package dev.efnilite.ip.legacy;

import dev.efnilite.ip.IP;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LegacyFolderMigration {

    public static void migrate() {
        Plugin plugin = IP.getPlugin();

        File file = new File("plugins/WITP");
        if (file.exists()) {
            try {
                Files.move(file.toPath(), plugin.getDataFolder().toPath());

                plugin.getLogger().info("##");
                plugin.getLogger().info("## New folder migration successful!");
                plugin.getLogger().info("## All Infinite Parkour files are now located in the plugins/IP folder.");
                plugin.getLogger().info("##");
            } catch (IOException ex) {
                plugin.getLogger().severe("##");
                plugin.getLogger().severe("## New folder migration failed!");
                plugin.getLogger().severe("## Please close any files in the plugins/WITP folder and restart the server.");
                plugin.getLogger().severe("## If this issue persists, please rename the plugins/WITP folder to plugins/IP.");
                plugin.getLogger().severe("##");

                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }
}