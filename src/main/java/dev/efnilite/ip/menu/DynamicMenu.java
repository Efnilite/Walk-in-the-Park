package dev.efnilite.ip.menu;

import dev.efnilite.ip.player.ParkourPlayer2;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.item.MenuItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * A class for menus where items are dynamically displayed; that is, depending on provided conditions.
 * These conditions are specified by the param {@code shouldDisplay} in the method {@link #registerMainItem(int, int, BiFunction, Predicate)}.
 */
public abstract class DynamicMenu {

    protected final Map<Integer, List<ItemContainer>> registeredItems = new HashMap<>();

    /**
     * Registers an item that will be displayed in a specific slot if the specified condition is met.
     * This will be used to create a context-aware main menu.
     *
     * @param row           The row in which this item will be displayed. Starts from 0 and ends at 5.
     * @param id            The id of this item. This will be used to determine the positions of each item.
     *                      Lower is more to the left. Can't be lower than 0.
     * @param item          The item
     * @param shouldDisplay Whether this item should be displayed right now.
     */
    public void registerMainItem(int row, int id, BiFunction<@NotNull Player, @Nullable ParkourPlayer2, MenuItem> item, Predicate<Player> shouldDisplay) {
        if (id < 0 || row < 0 || row > 4) {
            return;
        }

        List<ItemContainer> existing = registeredItems.get(row);
        if (existing == null) {
            existing = new ArrayList<>();
        }
        existing.add(new ItemContainer(id, item, shouldDisplay));

        registeredItems.put(row, existing);
    }

    /**
     * Opens this dynamic menu to a player
     *
     * @param player The player
     * @param menu   The menu
     */
    public void display(Player player, Menu menu) {
        var user = ParkourPlayer2.as(player);
        for (int row : registeredItems.keySet()) {
            int actualSlot = row * 9; // 0, 9, 18, etc.

            List<ItemContainer> containers = registeredItems.get(row); // sort by id first
            containers.sort(Comparator.comparingInt(container -> container.id));

            for (ItemContainer container : containers) {
                if (container.predicate.test(player)) { // if item in id passes predicate, display it in the menu
                    menu.item(actualSlot, container.item.apply(player, user));
                    actualSlot++;
                }
            }
        }

        menu.open(player);
    }

    /**
     * Data class for registered items
     */
    protected record ItemContainer(int id, BiFunction<@NotNull Player, @Nullable ParkourPlayer2, MenuItem> item,
                                   Predicate<Player> predicate) {}
}
