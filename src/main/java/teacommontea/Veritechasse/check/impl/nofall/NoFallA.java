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

package teacommontea.veritechasse.check.impl.nofall;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.GroundDetector;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;

@CheckInfo(name = "NoFallA", description = "Landed from a long fall with no damage reaction.", decay = 0.02)
public final class NoFallA extends Check implements PacketCheck, ConfidenceCheck {

    private static final double FALL_DAMAGE_THRESHOLD = 3.5;
    private static final int IMPULSE_TICKS = 12;
    private static final int SUSTAIN = 2;

    private double lastY;
    private boolean hasLast;
    private double fallDistance;
    private boolean wasOnGround = true;
    private int impulseTimer;
    private int damageTimer;
    private int suspiciousLandings;
    private double innocence = 1.0;

    public NoFallA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    public void markImpulse() {
        impulseTimer = IMPULSE_TICKS;
        fallDistance = 0;
        hasLast = false;
    }

    public void markDamage() {
        damageTimer = IMPULSE_TICKS;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (!event.isFlying()) return;

        WorldSnapshot s = player.snapshot;
        if (!s.valid) return;
        if (impulseTimer > 0) impulseTimer--;
        if (damageTimer > 0) damageTimer--;

        VeritePacketEvent flying = event;

        double x = flying.getLocation().getX();
        double y = flying.getLocation().getY();
        double z = flying.getLocation().getZ();
        boolean onGround = GroundDetector.isOnGround(s, x, z, y);

        if (!hasLast) {
            lastY = y;
            hasLast = true;
            wasOnGround = onGround;
            return;
        }
        double deltaY = y - lastY;
        lastY = y;

        boolean exempt = !s.fallDamagePossible
                || s.gameMode == GameMode.CREATIVE || s.gameMode == GameMode.SPECTATOR
                || s.flying || s.gliding || s.riptiding || s.insideVehicle
                || s.levitation || s.slowFalling || impulseTimer > 0
                || negatesFall(s);

        if (exempt) {
            fallDistance = 0;
            wasOnGround = onGround;
            return;
        }

        if (!onGround && deltaY < 0) {
            fallDistance += -deltaY;
        }

        boolean landed = onGround && !wasOnGround;
        wasOnGround = onGround;

        if (landed) {
            if (fallDistance > FALL_DAMAGE_THRESHOLD && damageTimer <= 0) {
                suspiciousLandings++;
                if (suspiciousLandings >= SUSTAIN) {
                    setInfo("fallDistance=" + formatOffset(fallDistance) + " threshold=" + formatOffset(FALL_DAMAGE_THRESHOLD));
                    innocence = Math.max(0.0, innocence - 0.5);
                }
            } else {
                suspiciousLandings = 0;
                innocence = Math.min(1.0, innocence + 0.3);
            }
            fallDistance = 0;
        }
    }

    private boolean negatesFall(WorldSnapshot s) {
        Material below = s.belowMaterial;
        Material here = s.feetMaterial;
        return below == Material.WATER || here == Material.WATER
                || below == Material.HAY_BLOCK || below == Material.SLIME_BLOCK
                || below == Material.HONEY_BLOCK || below == Material.COBWEB
                || here == Material.COBWEB || below == Material.SWEET_BERRY_BUSH
                || below == Material.POWDER_SNOW || here == Material.POWDER_SNOW
                || below == Material.BUBBLE_COLUMN || here == Material.LADDER
                || here == Material.VINE || here == Material.SCAFFOLDING;
    }
}
