package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourCommand;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.SingleSession;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import dev.efnilite.vilib.inventory.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Class for the main menu, accessed on executing /parkour
 */
public class MainMenu {

    private static final Map<Integer, List<ItemContainer>> registeredItems = new HashMap<>();

    static {
        // Singleplayer if player is not found
        registerMainItem(1, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.singleplayer").click(
                event -> SingleplayerMenu.open(event.getPlayer())),
                player -> {
                    ParkourUser user = ParkourUser.getUser(player);
                    // if user is null display item or if the player isn't already playing single player
                    return user == null || user instanceof ParkourSpectator || !(user instanceof ParkourPlayer) && ParkourOption.JOIN.check(player)
                            && !(user.getSession() instanceof SingleSession);
                });

        registerMainItem(1, 2,
                user -> IP.getConfiguration().getFromItemData(user, "main.spectator").click(
                event -> SpectatorMenu.open(event.getPlayer())),
                // display spectator if the player isn't already one
                player -> !(ParkourUser.getUser(player) instanceof ParkourSpectator) && ParkourOption.JOIN.check(player));

        // Settings if player is active
        registerMainItem(1, 9,
                user -> IP.getConfiguration().getFromItemData(user, "main.settings").click(event -> {
                ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());

                if (pp != null) {
                    pp.getGenerator().menu();
                }
        }), player -> ParkourPlayer.isActive(player) && ParkourOption.SETTINGS.check(player));

        // Quit button if player is active
        registerMainItem(1, 10,
                user -> IP.getConfiguration().getFromItemData(user, "main.quit").click(event ->
                ParkourUser.leave(event.getPlayer())),
                ParkourPlayer::isActive);

        // Leaderboard only if player has perms
        registerMainItem(3, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.leaderboard").click(
                event -> LeaderboardMenu.open(event.getPlayer())),
                ParkourOption.LEADERBOARD::check);

        // Language only if player has perms
        registerMainItem(3, 1,
                user -> IP.getConfiguration().getFromItemData(user, "main.language").click(
                event -> LangMenu.open(ParkourPlayer.getPlayer(event.getPlayer()))),
                player -> ParkourPlayer.isActive(player) && ParkourOption.LANGUAGE.check(player));

        registerMainItem(3, 2,
                user -> IP.getConfiguration().getFromItemData(user, "main.commands").click(
                event -> {
                    ParkourCommand.sendHelpMessages(event.getPlayer());
                    event.getPlayer().closeInventory();
                }),
                player -> true);

        // Always allow closing of the menu
        registerMainItem(3, 10,
                user -> IP.getConfiguration().getFromItemData(user, "general.close")
                        .click(event -> event.getPlayer().closeInventory()),
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
    public static void registerMainItem(int row, int id, Function<@Nullable ParkourUser, MenuItem> item, Predicate<Player> predicate) {
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

        ParkourUser user = ParkourUser.getUser(player);
        for (int row : registeredItems.keySet()) {
            int actualSlot = row * 9; // 0, 9, 18, etc.

            List<ItemContainer> containers = registeredItems.get(row); // sort by id first
            containers.sort(Comparator.comparingInt(container -> container.id));

            for (ItemContainer container : containers) {
                if (container.predicate.test(player)) { // if item in id passes predicate, display it in the menu
                    menu.item(actualSlot, container.item.apply(user));
                    actualSlot++;
                }
            }
        }

        menu.open(player);
    }

    /**
     * Data class for registered items
     */
    private record ItemContainer(int id, Function<@Nullable ParkourUser, MenuItem> item, Predicate<Player> predicate) {

    }
}