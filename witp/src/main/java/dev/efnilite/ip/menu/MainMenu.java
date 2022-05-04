package dev.efnilite.ip.menu;

import dev.efnilite.ip.ParkourCommand;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

/**
 * Class for the main menu, accessed on executing /parkour
 */
public class MainMenu {

    private static final Map<Integer, List<ItemContainer>> registeredItems = new HashMap<>();

    static {
        // Singleplayer if player is not found
        registerMainItem(1, 0, new Item(Material.ENDER_PEARL, "<#6E92B1><bold>Singleplayer")
                .lore("<gray>Play on your own.").click(
                event -> SingleplayerMenu.open(event.getPlayer())),
                player -> !ParkourPlayer.isActive(player) && ParkourOption.JOIN.check(player));

        registerMainItem(1, 2, new Item(Material.GLASS, "<#39D5AB><bold>Spectator")
                .lore("<gray>Spectate another player or lobby.").click(
                event -> SpectatorMenu.open(event.getPlayer())),
                player -> !ParkourPlayer.isActive(player) && ParkourOption.JOIN.check(player));

        // Settings if player is active
        registerMainItem(1, 9, new Item(Material.SCAFFOLDING, "<#8CE03F><bold>Settings").click(event -> {
                ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());

                if (pp != null) {
                    pp.getGenerator().menu();
                }
        }), player -> ParkourPlayer.isActive(player) && ParkourOption.SETTINGS.check(player));

        // Quit button if player is active
        registerMainItem(1, 10, new Item(Material.BARRIER, "<#D71F1F><bold>Quit").click(event -> // todo add lang support
                ParkourUser.leave(event.getPlayer())),
                ParkourPlayer::isActive);

        // Leaderboard only if player has perms
        registerMainItem(3, 0, new Item(Material.GOLD_NUGGET, "<#6693E7><bold>Leaderboard").click( // todo add items.yml support
                event -> LeaderboardMenu.open(event.getPlayer())),
                ParkourOption.LEADERBOARD::check);

        // Language only if player has perms
        registerMainItem(3, 1, new Item(Material.WRITABLE_BOOK, "<#4A41BC><bold>Language")
                .lore("<gray>Change your language.").click(
                event -> LangMenu.open(ParkourPlayer.getPlayer(event.getPlayer()))),
                player -> ParkourPlayer.isActive(player) && ParkourOption.LANGUAGE.check(player));

        registerMainItem(3, 2, new Item(Material.PAPER, "<#E53CA2><bold>View commands")
                .lore("<gray>View all current commands.").click(
                event -> {
                    ParkourCommand.sendHelpMessages(event.getPlayer());
                    event.getPlayer().closeInventory();
                }),
                player -> true);

        // Always allow closing of the menu
        registerMainItem(3, 10, new Item(Material.ARROW, "<#F5A3A3><bold>Close").click(
                event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    /**
     * Registers an item that will be displayed in a specific slot if the specified condition is met.
     * This will be used to create a context-aware main menu.
     *
     * @param   row
     *          The row in which this item will be displayed. Starts from 0 and ends at 5.
     *
     * @param   id
     *          The id of this item. This will be used to determine the positions of each item.
     *          Lower is more to the left. Can't be lower than 0.
     *
     * @param   item
     *          The item
     *
     * @param   predicate
     *          The predicate
     */
    public static void registerMainItem(int row, int id, MenuItem item, Predicate<Player> predicate) {
        if (id < 0 || row < 0 || row > 4) {
            return;
        }

        List<ItemContainer> existing = registeredItems.get(row);
        if (existing == null) {
            existing = new ArrayList<>();
        }
        existing.add(new ItemContainer(id, item, predicate));

        registeredItems.put(row, existing);
    }

    /**
     * Opens the main menu.
     *
     * @param   player
     *          The player to open the menu to
     */
    public static void open(Player player) {
        Menu menu = new Menu(4, "<white>Parkour")
                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .distributeRowEvenly(0, 1, 2, 3);

        for (int row : registeredItems.keySet()) {
            int actualSlot = row * 9; // 0, 9, 18, etc.

            List<ItemContainer> containers = registeredItems.get(row); // sort by id first
            containers.sort(Comparator.comparingInt(container -> container.id));

            for (ItemContainer container : containers) {
                if (container.predicate.test(player)) { // if item in id passes predicate, display it in the menu
                    menu.item(actualSlot, container.item);
                    actualSlot++;
                }
            }
        }

        menu.open(player);
    }

    /**
     * Data class for registered items
     */
    private record ItemContainer(int id, MenuItem item, Predicate<Player> predicate) {

    }
}