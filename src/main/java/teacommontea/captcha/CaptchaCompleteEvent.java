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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.UUID;

public final class CaptchaCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID playerId;
    private final CaptchaKind kind;
    private final CaptchaOutcome outcome;
    private final long durationMs;
    private final List<String> failedChoices;
    private final boolean punishmentRequested;

    public CaptchaCompleteEvent(Player player, CaptchaKind kind, CaptchaOutcome outcome,
                                long durationMs, List<String> failedChoices, boolean punishmentRequested) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.kind = kind;
        this.outcome = outcome;
        this.durationMs = durationMs;
        this.failedChoices = failedChoices == null ? List.of() : List.copyOf(failedChoices);
        this.punishmentRequested = punishmentRequested;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public CaptchaKind getKind() {
        return kind;
    }

    public CaptchaOutcome getOutcome() {
        return outcome;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public List<String> getFailedChoices() {
        return failedChoices;
    }

    public int getFailedCount() {
        return failedChoices.size();
    }

    public boolean isPunishmentRequested() {
        return punishmentRequested;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
