package dev.efnilite.witp.wrapper;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A class for removing unnecessary methods that are the same across all Event classes,
 * making the actual Event classes cleaner.
 */
public abstract class EventWrapper extends Event {

    private static final HandlerList handlerList = new HandlerList();
    protected boolean cancelled;

    public boolean call() {
        Bukkit.getPluginManager().callEvent(this);
        return cancelled;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}