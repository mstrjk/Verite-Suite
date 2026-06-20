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

package teacommontea.veritechasse.check.impl.baritone;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;

@CheckInfo(name = "BaritoneA", description = "Forward-only input steering a curving path with perfect tracking (pathfinder).", decay = 0.02)
public final class BaritoneA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double MOVE_THRESHOLD = 0.12;
    private static final double FORWARD_DOT = 0.995;
    private static final float PITCH_EPSILON = 0.05f;
    private static final float MIN_YAW_TRAVEL = 90.0f;
    private static final int REQUIRED_TICKS = 100;
    private static final int IMPULSE_TICKS = 10;

    private boolean forwardOnly;
    private int inputGrace;

    private float anchorPitch;
    private float lastYaw;
    private boolean hasAnchor;
    private int trackingTicks;
    private float yawTravel;
    private int teleportTimer;
    private double innocence = 1.0;

    public BaritoneA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markTeleport() {
        teleportTimer = IMPULSE_TICKS;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() == VeritePacketType.PLAYER_INPUT) {
            boolean strafing = event.isLeft() || event.isRight();
            forwardOnly = event.isForward() && !strafing && !event.isBackward();
            if (strafing || event.isBackward()) inputGrace = 6;
            return;
        }

        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (teleportTimer > 0) teleportTimer--;
        if (inputGrace > 0) inputGrace--;

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) return;

        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        double moved = Math.sqrt(dx * dx + dz * dz);

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle || teleportTimer > 0
                || s.horizontalCollision;

        if (exempt || moved < MOVE_THRESHOLD || !forwardOnly || inputGrace > 0) {
            resetTracking();
            innocence = Math.min(1.0, innocence + 0.15);
            return;
        }

        float pitch = player.pitch;
        float yaw = player.yaw;
        if (!hasAnchor) {
            anchorPitch = pitch;
            lastYaw = yaw;
            yawTravel = 0.0f;
            hasAnchor = true;
            trackingTicks = 1;
            return;
        }

        double yawRad = Math.toRadians(yaw);
        double dot = (dx * -Math.sin(yawRad) + dz * Math.cos(yawRad)) / moved;

        if (Math.abs(pitch - anchorPitch) <= PITCH_EPSILON && dot >= FORWARD_DOT) {
            yawTravel += Math.abs(angleDiff(yaw, lastYaw));
            lastYaw = yaw;
            trackingTicks++;
            if (trackingTicks > REQUIRED_TICKS && yawTravel >= MIN_YAW_TRAVEL) {
                setInfo("pitchDelta=" + formatOffset(Math.abs(pitch - anchorPitch)) + " yawTravel="
                        + formatOffset(yawTravel) + " dot=" + formatOffset(dot) + " ticks=" + trackingTicks);
                innocence = Math.max(0.0, innocence - 0.4);
            } else {
                innocence = Math.min(1.0, innocence + 0.02);
            }
        } else {
            resetTracking();
            innocence = Math.min(1.0, innocence + 0.15);
        }
    }

    private void resetTracking() {
        hasAnchor = false;
        yawTravel = 0.0f;
        trackingTicks = Math.max(0, trackingTicks - 2);
    }

    private static float angleDiff(float a, float b) {
        float diff = (a - b) % 360.0f;
        if (diff >= 180.0f) diff -= 360.0f;
        if (diff < -180.0f) diff += 360.0f;
        return diff;
    }
}
