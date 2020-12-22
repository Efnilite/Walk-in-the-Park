package dev.efnilite.witp.util.inventory;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

public interface InventoryConsumer extends BiConsumer<InventoryClickEvent, ItemStack> {

}
