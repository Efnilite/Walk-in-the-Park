package dev.efnilite.ip;

import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.world.World;
import dev.efnilite.vilib.event.EventWatcher;
import dev.efnilite.vilib.particle.ParticleData;
import dev.efnilite.vilib.particle.Particles;
import dev.efnilite.vilib.util.Locations;
import dev.efnilite.vilib.util.Strings;
import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Internal event handler
 */
@ApiStatus.Internal
public class Events implements EventWatcher {

    @EventHandler
    public void chat(AsyncPlayerChatEvent event) {
        if (!Option.OPTIONS_ENABLED.get(ParkourOption.CHAT)) {
            return;
        }

        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);

        if (user == null) {
            return;
        }

        Session session = user.session;

        if (session.muted.contains(user)) {
            return;
        }

        event.setCancelled(true);
        switch (user.chatType) {
            case LOBBY_ONLY -> session.getUsers().forEach(other -> other.sendTranslated("settings.chat.formats.lobby", player.getName(), event.getMessage()));
            case PLAYERS_ONLY -> session.getPlayers().forEach(other -> other.sendTranslated("settings.chat.formats.players", player.getName(), event.getMessage()));
            default -> event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            Modes.DEFAULT.create(player);
            return;
        }

        if (!player.getWorld().equals(World.getWorld())) {
            return;
        }

        org.bukkit.World fallback = Bukkit.getWorld(Config.CONFIG.getString("world.fall-back"));

        if (fallback != null) {
            PaperLib.teleportAsync(player, fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            return;
        }

        PaperLib.teleportAsync(player, Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(World.getWorld()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("No fallback world was found!"))
                .getSpawnLocation());
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        ParkourUser.unregister(user, true, false, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void command(PlayerCommandPreprocessEvent event) {
        if (!Config.CONFIG.getBoolean("focus-mode.enabled")) {
            return;
        }

        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        String command = event.getMessage().toLowerCase();
        if (Config.CONFIG.getStringList("focus-mode.whitelist").stream().anyMatch(c -> command.contains(c.toLowerCase()))) {
            return;
        }

        user.sendTranslated("other.no_do");
        event.setCancelled(true);
    }


    @EventHandler
    public void interactWand(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("ip.admin") || item.getItemMeta() == null || !item.getItemMeta().getDisplayName().contains("Schematic Wand") || event.getClickedBlock() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Location location = event.getClickedBlock().getLocation();
        Location[] existingSelection = Command.selections.get(player);

        event.setCancelled(true);

        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                send(player, IP.PREFIX + "Position 1 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    Command.selections.put(player, new Location[]{location, null});
                    return;
                }

                Command.selections.put(player, new Location[]{location, existingSelection[1]});

                Particles.box(BoundingBox.of(location, existingSelection[1]), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
            }
            case RIGHT_CLICK_BLOCK -> {
                send(player, IP.PREFIX + "Position 2 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    Command.selections.put(player, new Location[]{null, location});
                    return;
                }

                Command.selections.put(player, new Location[]{existingSelection[0], location});

                Particles.box(BoundingBox.of(existingSelection[0], location), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);

        if (pp == null) {
            return;
        }

        boolean type = event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.DISPENSER || event.getClickedBlock().getType() == Material.DROPPER || event.getClickedBlock().getType() == Material.HOPPER);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && type && event.getHand() == EquipmentSlot.HAND) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;

        if (!action) {
            return;
        }

        Material held = getHeldItem(player).getType();

        Material play = Locales.getItem(player, "play.item").getMaterial();
        Material community = Locales.getItem(player, "community.item").getMaterial();
        Material settings = Locales.getItem(player, "settings.item").getMaterial();
        Material lobby = Locales.getItem(player, "lobby.item").getMaterial();
        Material quit = Locales.getItem(player, "other.quit").getMaterial();

        event.setCancelled(true);

        if (held == play) {
            Menus.PLAY.open(player);
        } else if (held == community) {
            Menus.COMMUNITY.open(player);
        } else if (held == settings) {
            Menus.SETTINGS.open(player);
        } else if (held == lobby) {
            Menus.LOBBY.open(player);
        } else if (held == quit) {
            ParkourUser.leave(player);
        } else {
            if (!Config.CONFIG.getBoolean("options.disable-inventory-blocks")) {
                event.setCancelled(false);
            }
        }
    }

    private ItemStack getHeldItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.AIR ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }

    @EventHandler
    public void switchWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        org.bukkit.World parkour = World.getWorld();

        boolean isAdmin = Config.CONFIG.getBoolean("permissions.enabled") ? ParkourOption.ADMIN.mayPerform(player) : player.isOp();

        if (player.getWorld() == parkour && user == null && !isAdmin && player.getTicksLived() > 20) {
            Bukkit.getWorlds().stream()
                    .filter(world -> !world.equals(parkour))
                    .findAny()
                    .ifPresent(world -> PaperLib.teleportAsync(player, world.getSpawnLocation()));
            return;
        }

        if (event.getFrom() == parkour && user != null && Duration.between(user.joined, Instant.now()).toMillis() > 100) {
            ParkourUser.unregister(user, true, false, false);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getInventory().getType() == InventoryType.CRAFTING) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void spectate(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }

        handleRestriction(event.getPlayer(), event);
    }

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void mount(EntityMountEvent event) {
//        if (!(event.getEntity() instanceof Player player)) {
//            return;
//        }
//
//        handleRestriction(player, event);
//    }

    private void handleRestriction(Player player, Cancellable event) {
        if (!ParkourUser.isUser(player)) {
            return;
        }

        event.setCancelled(true);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Strings.colour(message));
    }
}