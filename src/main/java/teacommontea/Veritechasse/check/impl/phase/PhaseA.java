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

package teacommontea.veritechasse.check.impl.phase;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.util.BoundingBox;

@CheckInfo(name = "PhaseA", description = "Body intersects a solid block while moving (noclip).", decay = 0.02)
public final class PhaseA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double WIDTH = 0.28;
    private static final double HEIGHT = 1.8;
    private static final double MOVE_THRESHOLD = 0.05;
    private static final int SUSTAIN = 2;
    private static final int IMPULSE_TICKS = 10;

    private double lastX, lastZ;
    private boolean hasLast;
    private int insideTicks;
    private int teleportTimer;
    private double innocence = 1.0;

    public PhaseA(VeritePlayer player) {
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
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (teleportTimer > 0) teleportTimer--;

        VeritePacketEvent flying = event;
        if (!flying.hasPositionChanged()) return;

        double x = flying.getLocation().getX();
        double y = flying.getLocation().getY();
        double z = flying.getLocation().getZ();
        if (!hasLast) {
            lastX = x;
            lastZ = z;
            hasLast = true;
            return;
        }
        double dx = x - lastX;
        double dz = z - lastZ;
        lastX = x;
        lastZ = z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        boolean exempt = s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle || teleportTimer > 0;

        if (exempt || horizontal < MOVE_THRESHOLD) {
            insideTicks = Math.max(0, insideTicks - 1);
            innocence = Math.min(1.0, innocence + 0.2);
            return;
        }

        if (intersectsSolid(s, x, y, z)) {
            insideTicks++;
            if (insideTicks >= SUSTAIN) {
                setInfo("horizontal=" + formatOffset(horizontal) + " insideTicks=" + insideTicks + " sustain=" + SUSTAIN);
                innocence = Math.max(0.0, innocence - 0.6);
            }
        } else {
            insideTicks = Math.max(0, insideTicks - 1);
            innocence = Math.min(1.0, innocence + 0.25);
        }
    }

    private boolean intersectsSolid(WorldSnapshot s, double x, double y, double z) {
        BoundingBox body = new BoundingBox(x - WIDTH, y + 0.2, z - WIDTH, x + WIDTH, y + HEIGHT, z + WIDTH);
        int bx0 = floor(x - WIDTH), bx1 = floor(x + WIDTH);
        int by0 = floor(y + 0.2), by1 = floor(y + HEIGHT);
        int bz0 = floor(z - WIDTH), bz1 = floor(z + WIDTH);

        for (int bx = bx0; bx <= bx1; bx++) {
            for (int by = by0; by <= by1; by++) {
                for (int bz = bz0; bz <= bz1; bz++) {
                    BoundingBox[] boxes = s.collisionAt(bx, by, bz);
                    if (boxes == null || boxes.length == 0) continue;
                    for (BoundingBox box : boxes) {
                        BoundingBox shifted = box.clone().shift(bx, by, bz);
                        if (shifted.overlaps(body)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
