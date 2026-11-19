package com.mcmiddleearth.guidebook.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player is about to see a Welcome. Another plugin can listen to act on the Welcome, or cancel it so the
 * player doesn't see it. To act only on Welcomes that are actually shown, listen at {@code EventPriority.MONITOR} with
 * {@code ignoreCancelled = true}. A cancelled Welcome doesn't count as one, so the event fires again the next time the
 * player enters the Area.
 *
 * <p>This is not an "entered an Area" event: entering an Area doesn't always lead to a Welcome, and staff previews
 * never fire it.
 *
 * <p>Changing how a Welcome looks is a Display's job, not this event's (ADR 0008).
 */
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
