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

package teacommontea.veritechasse.check.impl;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.net.VeritePacketEvent.DiggingAction;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;
import teacommontea.veritechasse.utils.math.MathUtil;

@CheckInfo(name = "AutoClickerB", description = "Attacked before the swing cooldown recharged.", decay = 0.02)
public final class AutoClickerB extends Check implements PacketCheck, teacommontea.veritechasse.check.ConfidenceCheck {

    @Override
    public double innocence() {
        return innocence;
    }

    private static final float MIN_PROGRESS = 0.2f;
    private static final double DEFAULT_ATTACK_SPEED = 4.0;

    private int ticksSinceSwing;
    private double innocence = 1.0;

    public AutoClickerB(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.isFlying()) {
            ticksSinceSwing++;
            return;
        }

        if (event.getPacketType() == VeritePacketType.ANIMATION) {
            ticksSinceSwing = 0;
            return;
        }

        if (event.getPacketType() == VeritePacketType.PLAYER_DIGGING) {
            if (event.getDiggingAction() == DiggingAction.RELEASE_USE_ITEM) {
                ticksSinceSwing = 0;
            }
            return;
        }

        boolean isAttack = false;
        if (event.getPacketType() == VeritePacketType.ATTACK) {
            isAttack = true;
        }
        if (!isAttack) return;

        float progress = getProgress();
        if (progress < MIN_PROGRESS) {
            double severity = (MIN_PROGRESS - progress) / MIN_PROGRESS;
            setInfo("progress=" + formatOffset(progress) + " min=" + formatOffset(MIN_PROGRESS));
            innocence = Math.max(0.0, innocence - severity * 0.25);
        } else {
            innocence = Math.min(1.0, innocence + 0.15);
        }
    }

    private float getProgress() {
        double attackSpeed = player.attackSpeed > 0 ? player.attackSpeed : DEFAULT_ATTACK_SPEED;
        double delay = 1.0 / attackSpeed * 20.0;
        return MathUtil.clamp((float) ((ticksSinceSwing + 0.5) / delay), 0f, 1f);
    }
}
