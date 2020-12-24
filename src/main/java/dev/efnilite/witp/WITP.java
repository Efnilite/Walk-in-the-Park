package dev.efnilite.witp;

import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.structure.StructureManager;
import dev.efnilite.witp.structure.StructureManager_v1_16_R3;
import dev.efnilite.witp.util.Configuration;
import dev.efnilite.witp.util.Metrics;
import dev.efnilite.witp.util.Util;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class WITP extends JavaPlugin implements Listener {

    private static WITP instance;
    private static Configuration configuration;
    private static StructureManager manager;

    @Override
    public void onEnable() {
        String version = Util.getVersion();
        if ("v1_16_R3".equals(version)) {
            manager = new StructureManager_v1_16_R3();
        } else {
            Verbose.error("You are trying to start this plugin using an invalid server version");
            Verbose.error("This plugin only works in version 1.16.4");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        configuration = new Configuration(this);

        if (configuration.getFile("config").getBoolean("metrics")) {
            new Metrics(this, 9272);
        }

        ParkourGenerator.Configurable.init();
        this.getServer().getPluginManager().registerEvents(this, this);
        addCommand("witp", new MainCommand());
    }

    private void addCommand(String name, BukkitCommand wrapper) {
        PluginCommand command = getCommand(name);

        if (command == null) {
            throw new IllegalStateException("Command is null");
        }

        command.setExecutor(wrapper);
        command.setTabCompleter(wrapper);
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        if (configuration.getFile("config").getBoolean("bungeecord.enabled")) {
            try {
                ParkourPlayer.register(event.getPlayer());
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Something went wrong while trying to fetch a player's (" + event.getPlayer().getName() + ") data");
            }
        }
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static StructureManager getManager() {
        return manager;
    }

    public static WITP getInstance() {
        return instance;
    }
}
