package dev.efnilite.ip.hook;

import dev.efnilite.ip.IP;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private static Economy economy;

    /**
     * Deposits amount to the bal of player.
     *
     * @param player The player.
     * @param amount The amount.
     */
    public static void deposit(Player player, double amount) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        if (economy == null) {
            RegisteredServiceProvider<Economy> service = Bukkit.getServicesManager().getRegistration(Economy.class);

            if (service == null) {
                IP.logging().stack("Error while trying to fetch the Vault economy!", new IllegalStateException());
                return;
            }

            economy = service.getProvider();
        }

        economy.depositPlayer(player, amount);
    }
}