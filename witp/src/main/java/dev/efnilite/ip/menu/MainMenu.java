package dev.efnilite.ip.menu;

import dev.efnilite.ip.ParkourCommand;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourSpectator;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.SingleSession;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.animation.RandomAnimation;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.util.Unicodes;
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
                .lore(formatSynonyms("单人玩家 %s シングルプレイヤー")).click(
                event -> SingleplayerMenu.open(event.getPlayer())),
                player -> {
                    ParkourUser user = ParkourUser.getUser(player);
                    // if user is null display item or if the player isn't already playing single player
                    return user == null || user instanceof ParkourSpectator || !(user instanceof ParkourPlayer) && ParkourOption.JOIN.check(player)
                            && !(user.getSession() instanceof SingleSession);
                });

        registerMainItem(1, 2, new Item(Material.GLASS, "<#39D5AB><bold>Spectator")
                .lore(formatSynonyms("Zuschauer %s 观众 %s 觀眾 %s Spectateur %s 見物人 %s Toekijker")).click(
                event -> SpectatorMenu.open(event.getPlayer())),
                // display spectator if the player isn't already one
                player -> !(ParkourUser.getUser(player) instanceof ParkourSpectator) && ParkourOption.JOIN.check(player));

        // Settings if player is active
        registerMainItem(1, 9, new Item(Material.SCAFFOLDING, "<#8CE03F><bold>Settings")
                .lore(formatSynonyms("Einstellungen %s 设置 %s 命令 %s Paramêtres %s セッティング %s Instellingen")).click(event -> {
                ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());

                if (pp != null) {
                    pp.getGenerator().menu();
                }
        }), player -> ParkourPlayer.isActive(player) && ParkourOption.SETTINGS.check(player));

        // Quit button if player is active
        registerMainItem(1, 10, new Item(Material.BARRIER, "<#D71F1F><bold>Quit")
                .lore(formatSynonyms("Aufhören %s 退出 %s 辭職 %s Quitter %s 去る %s Stoppen")).click(event -> // todo add lang support
                ParkourUser.leave(event.getPlayer())),
                ParkourPlayer::isActive);

        // Leaderboard only if player has perms
        registerMainItem(3, 0, new Item(Material.GOLD_NUGGET, "<#6693E7><bold>Leaderboard")
                .lore(formatSynonyms("Bestenliste %s 排行榜 %s Classement %s リーダーボード %s Scorebord")).click( // todo add items.yml support
                event -> LeaderboardMenu.open(event.getPlayer())),
                ParkourOption.LEADERBOARD::check);

        // Language only if player has perms
        registerMainItem(3, 1, new Item(Material.WRITABLE_BOOK, "<#4A41BC><bold>Language")
                .lore(formatSynonyms("Sprache %s 语言 %s 語言 %s Langue %s 言語 %s Taal")).click(
                event -> LangMenu.open(ParkourPlayer.getPlayer(event.getPlayer()))),
                player -> ParkourPlayer.isActive(player) && ParkourOption.LANGUAGE.check(player));

        registerMainItem(3, 2, new Item(Material.PAPER, "<#E53CA2><bold>View commands")
                .lore(formatSynonyms("Commands ansehen %s 查看命令 %s Afficher commandes %s Commands bekijken")).click(
                event -> {
                    ParkourCommand.sendHelpMessages(event.getPlayer());
                    event.getPlayer().closeInventory();
                }),
                player -> true);

        // Always allow closing of the menu
        registerMainItem(3, 10, new Item(Material.ARROW, "<#F5A3A3><bold>Close")
                .lore(formatSynonyms("Schließen %s 关闭 %s Fermer %s 閉じる %s Sluiten")).click(
                event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    /**
     * Formats synonyms in the main menu. {@code %s} is used as the separator symbol. Max line length is 17.
     * This also adds the colour dark_gray to every line.
     *
     * @param   string
     *          The string containing the text.
     *
     * @return a String array that has been formatted according to the description.
     */
    public static List<String> formatSynonyms(String string) {
        String separator = String.valueOf(Unicodes.BULLET);
        string = string.replace("%s", separator);

        List<String> total = new ArrayList<>();
        String[] sections = string.split(separator); // split by character
        StringBuilder current = new StringBuilder();
        for (String section : sections) {
            current.append(section); // append section and separator
            if (current.length() > 17) { // if length is > 20, loop around
                total.add(current.insert(0, "<dark_gray>").toString());
                current = new StringBuilder().append(separator);
            } else {
                current.append(separator);
            }
        }
        current.deleteCharAt(current.length() - 1); // delete trailing character
        if (current.length() > 1) {
            total.add(current.insert(0, "<dark_gray>").toString()); // add final result
        }

        return total;
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