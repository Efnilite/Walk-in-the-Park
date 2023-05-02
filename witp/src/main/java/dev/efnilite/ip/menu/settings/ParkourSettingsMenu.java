package dev.efnilite.ip.menu.settings;

import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.menu.DynamicMenu;
import dev.efnilite.ip.menu.Menus;
import dev.efnilite.ip.menu.ParkourOption;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.style.Style;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.util.Util;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.MenuClickEvent;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.inventory.item.SliderItem;
import dev.efnilite.vilib.inventory.item.TimedItem;
import dev.efnilite.vilib.lib.fastboard.fastboard.FastBoard;
import dev.efnilite.vilib.util.Unicodes;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles all menu-related activities
 *
 * @author Efnilite
 * @since v3.0.0
 */
public class ParkourSettingsMenu extends DynamicMenu {

    public ParkourSettingsMenu(ParkourOption... disabled) {

        // ---------- top row ----------

        // styles
        registerMainItem(1, 0, (p, user) -> Locales.getItem(p, ParkourOption.STYLES.path + ".item", user instanceof ParkourPlayer player ? player.style : null).click(event -> {
            if (!(user instanceof ParkourPlayer player)) {
                return;
            }

            openStyleMenu(player);
        }), player -> checkOptions(player, ParkourOption.STYLES, disabled));

        // leads
        registerMainItem(1, 1, (p, user) -> {
            Item displayItem = Locales.getItem(p, ParkourOption.LEADS.path);

            if (!(user instanceof ParkourPlayer player)) {
                return displayItem;
            }

            List<Integer> leads = Option.POSSIBLE_LEADS;

            SliderItem item = new SliderItem().initial(leads.indexOf(player.blockLead)); // initial value of the player

            int slot = 0;
            for (int value : leads) {
                item.add(slot, displayItem.clone()
                            .amount(value)
                            .modifyLore(line -> line.replace("%s", Integer.toString(value))),
                        event2 -> handleSettingChange(player, () -> player.blockLead = value));
                slot++;
            }
            return item;
        }, player -> checkOptions(player, ParkourOption.LEADS, disabled));

        // schematics
        registerMainItem(1, 9, (p, user) -> Locales.getItem(p, ParkourOption.SCHEMATIC.path + ".item").click(event -> {
            if (!(user instanceof ParkourPlayer player)) {
                return;
            }
            openSchematicMenu(player, disabled);
        }), player -> checkOptions(player, ParkourOption.SCHEMATIC, disabled));

        // time
        registerMainItem(1, 10, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.TIME.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            // Tick times start at 6:00 and total is 24.000.
            // Source: https://minecraft.fandom.com/wiki/Daylight_cycle?file=Day_Night_Clock_24h.png
            List<Integer> times = Arrays.asList(0, 6000, 12000, 18000); // 00:00 -> 6:00 -> 12:00 -> 18:00

            return new SliderItem()
                .initial(times.indexOf(player.selectedTime))
                .add(0, item.clone()
                            .modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "12:00 AM" : "00:00")),
                        event -> handleSettingChange(player, () -> player.selectedTime = 0))
                    .add(1, item.clone()
                            .modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "6:00 AM" : "6:00")),
                        event -> handleSettingChange(player, () -> player.selectedTime = 6000))
                    .add(2, item.clone()
                            .modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "12:00 PM" : "12:00")),
                        event -> handleSettingChange(player, () -> player.selectedTime = 12000))
                    .add(3, item.clone()
                            .modifyLore(line -> line.replace("%s", Option.OPTIONS_TIME_FORMAT == 12 ? "6:00 PM" : "18:00")),
                        event -> handleSettingChange(player, () -> player.selectedTime = 18000));
        }, player -> checkOptions(player, ParkourOption.TIME, disabled));

        // ---------- bottom row ----------

        // show scoreboard
        registerMainItem(2, 0, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.SCOREBOARD.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem()
                .initial(player.showScoreboard ? 0 : 1)
                .add(0, item.clone()
                        .material(Material.LIME_STAINED_GLASS_PANE)
                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))),
                    event -> handleSettingChange(player, () -> {
                            player.showScoreboard = true;
                            player.board = new FastBoard(p);
                        }))
                .add(1, item.clone()
                        .material(Material.RED_STAINED_GLASS_PANE)
                        .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))),
                    event -> handleSettingChange(player, () -> {
                        player.showScoreboard = false;
                        if (player.board != null && !player.board.isDeleted()) {
                            player.board.delete();
                        }
                    }));
        }, player -> checkOptions(player, ParkourOption.SCOREBOARD, disabled) && Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD)));

        // show fall message
        registerMainItem(2, 1, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.FALL_MESSAGE.path + ".item");

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem()
                .initial(player.showFallMessage ? 0 : 1)
                .add(0, item.clone()
                        .material(Material.LIME_STAINED_GLASS_PANE)
                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))),
                    event -> handleSettingChange(player, () -> player.showFallMessage = true))
                .add(1, item.clone()
                    .material(Material.RED_STAINED_GLASS_PANE)
                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))),
                event -> handleSettingChange(player, () -> player.showFallMessage = false));
        }, player -> checkOptions(player, ParkourOption.FALL_MESSAGE, disabled));

        // show sound
        registerMainItem(2, 2, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.PARTICLES.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem()
                .initial(player.particles ? 0 : 1)
                .add(0, item.clone().material(Material.LIME_STAINED_GLASS_PANE)
                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))),
                    event -> handleSettingChange(player, () -> player.particles = true))
                .add(1, item.clone()
                        .material(Material.RED_STAINED_GLASS_PANE)
                        .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))),
                    event -> handleSettingChange(player, () -> player.particles = false));
        }, player -> checkOptions(player, ParkourOption.PARTICLES, disabled));

        // show sound
        registerMainItem(2, 3, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.SOUND.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem()
                .initial(player.sound ? 0 : 1)
                .add(0, item.clone()
                        .material(Material.LIME_STAINED_GLASS_PANE)
                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))),
                    event -> handleSettingChange(player, () -> player.sound = true))
                .add(1, item.clone()
                    .material(Material.RED_STAINED_GLASS_PANE)
                    .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                    .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))),
                    event -> handleSettingChange(player, () -> player.sound = false));
        }, player -> checkOptions(player, ParkourOption.SOUND, disabled));

        // show special blocks
        registerMainItem(2, 4, (p, user) -> {
            Item item = Locales.getItem(p, ParkourOption.SPECIAL_BLOCKS.path);

            if (!(user instanceof ParkourPlayer player)) {
                return item;
            }

            return new SliderItem()
                .initial(player.useSpecialBlocks ? 0 : 1)
                .add(0, item.clone()
                        .material(Material.LIME_STAINED_GLASS_PANE)
                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))),
                    event -> handleScoreSettingChange(player, event, () -> player.useSpecialBlocks = true))
                .add(1, item.clone()
                        .material(Material.RED_STAINED_GLASS_PANE)
                        .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))),
                    event -> handleScoreSettingChange(player, event, () -> player.useSpecialBlocks = false));
        }, player -> checkOptions(player, ParkourOption.SPECIAL_BLOCKS, disabled));

        // Always allow closing of the menu
        registerMainItem(3, 10,
                (player, user) -> Locales.getItem(player, "other.close")
                    .click(event -> Menus.SETTINGS.open(event.getPlayer())),
                player -> true);
    }

    /**
     * Shows the main menu to a valid ParkourPlayer instance
     *
     * @param user The ParkourPlayer
     */
    public void open(ParkourPlayer user) {
        Player player = user.player;

        display(player, new Menu(4, Locales.getString(player, "settings.name"))
            .distributeRowEvenly(0, 1, 2, 3)
            .item(27, Locales.getItem(player, "other.close").click(event -> Menus.SETTINGS.open(event.getPlayer())))
            .fillBackground(Util.isBedrockPlayer(player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE));
    }

    /**
     * Opens the style menu
     *
     * @param player      The ParkourPlayer instance
     */
    public void openStyleMenu(ParkourPlayer player) {
        // init menu
        PagedMenu menu = new PagedMenu(3, Locales.getString(player.locale, ParkourOption.STYLES.path + ".name"));

        List<MenuItem> items = new ArrayList<>();
        for (Style style : Registry.getStyles()) {
            String perm = ParkourOption.STYLES.permission + "." + style.name().toLowerCase();
            if (Option.PERMISSIONS_STYLES && !player.player.hasPermission(perm.replace(" ", "."))) {
                continue;
            }

            items.add(Locales.getItem(player.player, ParkourOption.STYLES.path + ".style_item", style.name(), style.category())
                .material(Colls.random(style.materials()).getMaterial())
                .glowing(player.style.equals(style.name()))
                .click(event -> {
                    player.style = style.name();
                    player.updateGeneratorSettings();
                    open(player);
                }));
        }

        menu.displayRows(0, 1)
                .addToDisplay(items)
                .nextPage(26, new Item(Material.LIME_DYE, "<#0DCB07><bold>" + Unicodes.DOUBLE_ARROW_RIGHT).click(event -> menu.page(1)))
                .prevPage(18, new Item(Material.RED_DYE, "<#DE1F1F><bold>" + Unicodes.DOUBLE_ARROW_LEFT).click(event -> menu.page(-1)))
                .item(22, Locales.getItem(player.locale, "other.close").click(event -> open(player)))
                .fillBackground(Util.isBedrockPlayer(player.player) ? Material.AIR : Material.GRAY_STAINED_GLASS_PANE).open(player.player);
    }

    /**
     * Opens the schematic customization menu
     *
     * @param user The ParkourPlayer instance
     */
    public void openSchematicMenu(ParkourPlayer user, ParkourOption[] disabled) {
        // init menu
        Menu schematics = new Menu(3, Locales.getString(user.locale, ParkourOption.SCHEMATIC.path + ".name"));

        List<Double> difficulties = Arrays.asList(0.25, 0.5, 0.75, 1.0);
        List<String> values = Locales.getStringList(user.locale, ParkourOption.SCHEMATIC_DIFFICULTY.path + ".values");

        Item item = Locales.getItem(user.locale, ParkourOption.SCHEMATIC_DIFFICULTY.path);

        if (checkOptions(user.player, ParkourOption.SCHEMATIC_DIFFICULTY, disabled)) {
            schematics.item(10, new SliderItem()
                .initial(difficulties.indexOf(user.schematicDifficulty))
                .add(0, item.clone()
                        .material(Material.LIME_STAINED_GLASS_PANE)
                        .modifyLore(line -> line.replace("%s", "<#0DCB07>" + values.get(0))),
                    event -> handleScoreSettingChange(user, event, () -> user.schematicDifficulty = 0.25))
                .add(1, item.clone()
                        .material(Material.YELLOW_STAINED_GLASS_PANE)
                        .modifyLore(line -> line.replace("%s", "<yellow>" + values.get(1))),
                    event -> handleScoreSettingChange(user, event, () -> user.schematicDifficulty = 0.5))
                .add(2, item.clone()
                        .material(Material.ORANGE_STAINED_GLASS_PANE)
                        .modifyLore(line -> line.replace("%s", "<#FF6C17>" + values.get(2))),
                    event -> handleScoreSettingChange(user, event, () -> user.schematicDifficulty = 0.75))
                .add(3, item.clone()
                        .material(Material.SKELETON_SKULL)
                        .modifyLore(line -> line.replace("%s", "<dark_red>" + values.get(3))),
                    event -> handleScoreSettingChange(user, event, () -> user.schematicDifficulty = 1.0)));
        }

        item = Locales.getItem(user.locale, ParkourOption.USE_SCHEMATICS.path);

        if (checkOptions(user.player, ParkourOption.USE_SCHEMATICS, disabled)) {
            schematics.item(9, new SliderItem().initial(user.useSchematic ? 0 : 1)
                .add(0, item.clone()
                        .material(Material.LIME_STAINED_GLASS_PANE)
                        .modifyName(name -> "<#0DCB07><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, true))),
                    event -> handleScoreSettingChange(user, event, () -> user.useSchematic = true))
                .add(1, item.clone()
                        .material(Material.RED_STAINED_GLASS_PANE)
                        .modifyName(name -> "<red><bold>" + ChatColor.stripColor(name))
                        .modifyLore(line -> line.replace("%s", getBooleanSymbol(user, false))),
                    event -> handleScoreSettingChange(user, event, () -> user.useSchematic = false)));
        }

        schematics.distributeRowEvenly(0, 1, 2)
                .item(26, Locales.getItem(user.locale, "other.close").click(event -> open(user)))
                .fillBackground(Util.isBedrockPlayer(user.player) ? Material.AIR : Material.CYAN_STAINED_GLASS_PANE).open(user.player);
    }

    private boolean handleSettingChange(ParkourPlayer player, Runnable onAllowed) {
        onAllowed.run();
        player.updateGeneratorSettings();
        return true;
    }

    private boolean handleScoreSettingChange(ParkourPlayer player, MenuClickEvent event, Runnable onAllowed) {
        if (player.session.generator.score == 0) {
            return handleSettingChange(player, onAllowed);
        }

        event.getMenu().item(event.getSlot(), new TimedItem(Locales.getItem(player.locale, "settings.parkour_settings.items.no_change").click((event1) -> {}), event).stay(5 * 20));
        event.getMenu().updateItem(event.getSlot());
        return false;

    }

    // replaces true/false with a checkmark and cross
    private String getBooleanSymbol(ParkourUser user, boolean value) {
        return value ? Locales.getString(user.player, "settings.parkour_settings.enabled") : Locales.getString(user.player, "settings.parkour_settings.disabled");
    }

    // check if option is allowed to be displayed
    private boolean checkOptions(Player player, ParkourOption option, ParkourOption[] disabled) {
        return !Arrays.asList(disabled).contains(option) && option.mayPerform(player);
    }
}