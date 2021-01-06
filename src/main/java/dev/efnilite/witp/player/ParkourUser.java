package dev.efnilite.witp.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

abstract class ParkourUser {

    protected final Player player;
    protected GameMode previousGamemode;
    protected Location previousLocation;
    protected HashMap<Integer, ItemStack> previousInventory;

    public ParkourUser(@NotNull Player player) {
        this.player = player;
    }
}