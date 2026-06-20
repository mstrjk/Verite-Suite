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

import teacommontea.veritechasse.net.BlockFace;
import org.bukkit.Material;
import teacommontea.veritechasse.net.GameMode;
import teacommontea.veritechasse.net.Vec3i;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.BlockBreakCheck;
import teacommontea.veritechasse.check.BlockPlaceCheck;
import teacommontea.veritechasse.player.BlockBreak;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@CheckInfo(name = "FastPlaceD", description = "Build coherence: orientation/correction signature of a printer.", decay = 0.02)
public final class FastPlaceD extends Check implements BlockPlaceCheck, BlockBreakCheck, teacommontea.veritechasse.check.ConfidenceCheck {

    private static final int SESSION_IDLE_TICKS = 40;
    private static final int PRISTINE_RUN_LIMIT = 24;
    private static final float MIN_TURN_FOR_FACING_CHANGE = 45.0f;

    private static final Set<Material> DIRECTIONAL = buildDirectionalSet();

    private final Map<Long, Placed> placedAt = new HashMap<>();
    private boolean awaitingReplaceAfterBreak;
    private long lastBreakKey = Long.MIN_VALUE;

    private int lastActivityTick = Integer.MIN_VALUE;
    private int directionalRun;
    private double innocence = 1.0;

    @Override
    public double innocence() {
        return innocence;
    }

    private BlockFace lastMandatedFacing;
    private float lastDirectionalYaw;

    public FastPlaceD(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
        int tick = player.currentTick();
        if (lastActivityTick != Integer.MIN_VALUE && tick - lastActivityTick > SESSION_IDLE_TICKS) {
            resetSession();
        }
        lastActivityTick = tick;

        Material type = place.bukkitMaterial;
        Vec3i pos = place.position;
        long key = key(pos);

        if (DIRECTIONAL.contains(type)) {
            checkOrientation(place);
            directionalRun++;
            if (directionalRun >= PRISTINE_RUN_LIMIT) {
                setInfo("directionalRun=" + String.valueOf(directionalRun) + " limit=" + String.valueOf(PRISTINE_RUN_LIMIT));
                innocence = Math.max(0.0, innocence - 0.5);
                directionalRun = 0;
            }
        }

        BlockFace mandatedFacing = PlacedStateResolver.resolveFacing(player, place);
        Placed prev = placedAt.get(key);
        if (awaitingReplaceAfterBreak && key == lastBreakKey && prev != null
                && (prev.type != type || prev.facing != mandatedFacing)) {
            directionalRun = 0;
            innocence = Math.min(1.0, innocence + 0.5);
        }
        awaitingReplaceAfterBreak = false;

        placedAt.put(key, new Placed(type, mandatedFacing));
    }

    @Override
    public void onBlockBreak(final BlockBreak blockBreak) {
        lastBreakKey = key(blockBreak.position);
        awaitingReplaceAfterBreak = true;
    }

    private void checkOrientation(BlockPlace place) {
        BlockFace mandated = PlacedStateResolver.resolveFacing(player, place);
        if (mandated == null) return;

        if (lastMandatedFacing != null && mandated != lastMandatedFacing) {
            float yawDelta = Math.abs(wrapDegrees(player.yaw - lastDirectionalYaw));
            if (yawDelta < MIN_TURN_FOR_FACING_CHANGE) {
                setInfo("yawDelta=" + formatOffset(yawDelta) + " minTurn=" + formatOffset(MIN_TURN_FOR_FACING_CHANGE));
                innocence = Math.max(0.0, innocence - 0.4);
            }
        }

        lastMandatedFacing = mandated;
        lastDirectionalYaw = player.yaw;
    }

    private static float wrapDegrees(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    private void resetSession() {
        placedAt.clear();
        directionalRun = 0;
        awaitingReplaceAfterBreak = false;
        lastMandatedFacing = null;
        lastDirectionalYaw = 0;
    }

    private static long key(Vec3i p) {
        return (((long) p.getX() & 0x3FFFFFF) << 38) | (((long) p.getZ() & 0x3FFFFFF) << 12)
                | ((long) p.getY() & 0xFFF);
    }

    private static Set<Material> buildDirectionalSet() {
        Set<Material> s = new HashSet<>();
        for (Material t : Material.values()) {
            String n = t.name();
            if (n.contains("STAIRS") || n.contains("TRAPDOOR") || n.endsWith("_DOOR")
                    || n.contains("LEVER") || n.contains("BUTTON") || n.contains("OBSERVER")
                    || n.contains("REPEATER") || n.contains("COMPARATOR")) {
                s.add(t);
            }
        }
        return s;
    }

    private record Placed(Material type, BlockFace facing) {
    }
}
