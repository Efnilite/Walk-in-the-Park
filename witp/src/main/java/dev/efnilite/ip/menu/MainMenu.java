package dev.efnilite.ip.menu;

import dev.efnilite.ip.ParkourMenu;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Class for the main menu, accessed on executing /parkour
 */
public class MainMenu {

    private static final Map<Integer, ItemContainer> registeredItems = new HashMap<>();

    static {
        // Singleplayer if player is not found
        registerMainItem(0, new Item(Material.BUCKET, "<#6E92B1><bold>Singleplayer").click(event -> {
            Player player = (Player) event.getEvent().getWhoClicked();
            ParkourUser.register(player);
        }), (player) -> !ParkourPlayer.isActive(player));

        // Settings if player is active
        registerMainItem(9, new Item(Material.SCAFFOLDING, "<#8CE03F><bold>Settings").click(event -> {
            Player player = (Player) event.getEvent().getWhoClicked();
            ParkourPlayer pp = ParkourPlayer.getPlayer(player);

            if (pp != null) {
                ParkourMenu.openSettingsMenu(pp);
            }
        }), ParkourPlayer::isActive);

        // Add a quit button if player is active
        registerMainItem(10, new Item(Material.BARRIER, "<#D71F1F><bold>Quit").click(event -> { // todo add lang support
            Player player = (Player) event.getEvent().getWhoClicked();
            ParkourUser.leave(player);
        }), ParkourPlayer::isActive);
    }

    /**
     * Registers an item that will be displayed in a specific slot if the specified condition is met.
     * This will be used to create a context-aware main menu.
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
    public static void registerMainItem(int id, MenuItem item, Predicate<Player> predicate) {
        if (id < 0) {
            return;
        }

        registeredItems.put(id, new ItemContainer(item, predicate));
    }

    /**
     * Opens the main menu.
     *
     * @param   player
     *          The player to open the menu to
     */
    public static void open(Player player) {
        Menu menu = new Menu(3, "Parkour")
                .distributeRowEvenly(1);

        int actualSlot = 9; // start from pos 9 (second row in inventory)
        for (int id : registeredItems.keySet()) {
            ItemContainer container = registeredItems.get(id);

            if (container.predicate.test(player)) { // if item in id passes predicate, display it in the menu
                menu.item(actualSlot, container.item);
                actualSlot++;
            }
        }

        menu.open(player);
    }

    /**
     * Data class for registered items
     */
    private static class ItemContainer {

        public MenuItem item;
        public Predicate<Player> predicate;

        public ItemContainer(MenuItem item, Predicate<Player> predicate) {
            this.item = item;
            this.predicate = predicate;
        }
    }

}
