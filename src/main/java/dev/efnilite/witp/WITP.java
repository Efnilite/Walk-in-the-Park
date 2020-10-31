package dev.efnilite.witp;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.util.Configuration;
import dev.efnilite.witp.util.Metrics;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class WITP extends JavaPlugin implements Listener {

    private static WITP instance;
    private static Configuration configuration;

    @Override
    public void onEnable() {
        instance = this;
        configuration = new Configuration(this);
        getServer().getPluginManager().registerEvents(this, this);

        if (configuration.getFile("config").getBoolean("metrics")) {
            new Metrics(this, 9272);
        }

        ParkourGenerator.GeneratorChance.init();

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
    public void jump(PlayerJumpEvent e) {
        ParkourPlayer player = ParkourPlayer.getPlayer(e.getPlayer());
        if (player != null) {
            player.getGenerator().generateNext();
        }
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static WITP getInstance() {
        return instance;
    }
}
