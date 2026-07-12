/*
 * This file is part of Verite.
 * Copyright (C) 2026 teacommontea
 *
 * Verite is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * Verite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details. You should have received a copy of the license along with
 * Verite. If not, see <https://www.gnu.org/licenses/>.
 */

package teacommontea.captcha;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class PunishmentRequestedEvent extends Event implements Cancellable {

    public enum Cause {
        CAPTCHA_FAILED,
        CAPTCHA_TIMEOUT
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID playerId;
    private final Cause cause;
    private final String check;
    private final String evidence;
    private final double confidence;
    private String kickMessage;
    private boolean cancelled;

    public PunishmentRequestedEvent(Player player, Cause cause, String check, String evidence,
                                    double confidence, String kickMessage) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.cause = cause;
        this.check = check;
        this.evidence = evidence;
        this.confidence = confidence;
        this.kickMessage = kickMessage;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Cause getCause() {
        return cause;
    }

    public String getCheck() {
        return check;
    }

    public String getEvidence() {
        return evidence;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
