package com.mcmiddleearth.guidebook.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuidebookSendEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private boolean cancelled;
    private final Player player;
    private final String guidebookName;

    public GuidebookSendEvent(Player player, String guidebookName) {
        this.player = player;
        this.guidebookName = guidebookName;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getGuidebookName() {
        return guidebookName;
    }

    public Player getPlayer() {
        return player;
    }
}
