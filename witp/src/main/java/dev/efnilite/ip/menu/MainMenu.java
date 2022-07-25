package dev.efnilite.ip.menu;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.ParkourOption;
import dev.efnilite.ip.chat.ChatType;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Class for the main menu, accessed on executing /parkour
 */
public class MainMenu extends DynamicMenu {

    public static final MainMenu INSTANCE = new MainMenu();

    public MainMenu() {
        // Singleplayer if player is not found
        registerMainItem(1, 0,
                user -> IP.getConfiguration().getFromItemData(user, "main.singleplayer").click(
                event -> SingleplayerMenu.open(event.getPlayer())),
                ParkourOption.JOIN::check);

        registerMainItem(1, 2,
                user -> IP.getConfiguration().getFromItemData(user, "main.spectator").click(
                event -> SpectatorMenu.open(event.getPlayer())),
                // display spectator if the player isn't already one
                ParkourOption.JOIN::check);

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
                user -> {
                    if (user == null) {
                        return new Item(Material.STONE, "");
                    }

                    ChatType next = switch (user.getChatType()) {
                        case PUBLIC -> ChatType.LOBBY_ONLY;
                        case LOBBY_ONLY -> ChatType.PLAYERS_ONLY;
                        default -> ChatType.PUBLIC;
                    };

                    return new Item(Material.FEATHER, "<#20C6BC><bold>Change chat to " + next)
                            .lore("<dark_gray>Select who you can chat with", "Currently: " + user.getChatType().getName()).click(
                            event -> {
                                user.setChatType(next);
                                open(event.getPlayer());
                            });
                },
                player -> ParkourUser.getUser(player) != null);

        // Always allow closing of the menu
        registerMainItem(3, 10,
                user -> IP.getConfiguration().getFromItemData(user, "general.close")
                        .click(event -> event.getPlayer().closeInventory()),
                player -> true);
    }

    /**
     * Opens the main menu.
     *
     * @param   player
     *          The player to open the menu to
     */
    public void open(Player player) {
        Menu menu = new Menu(4, "<white>Parkour")
                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .distributeRowEvenly(0, 1, 2, 3);

        display(player, menu);
    }
}