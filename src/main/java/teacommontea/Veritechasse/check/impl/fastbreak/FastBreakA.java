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

package teacommontea.veritechasse.check.impl.fastbreak;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.net.VeritePacketEvent.DiggingAction;
import teacommontea.veritechasse.net.Vec3i;
import teacommontea.veritechasse.check.BlockBreakCheck;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.ConfidenceCheck;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.engine.BreakTimeResolver;
import teacommontea.veritechasse.player.BlockBreak;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CheckInfo(name = "FastBreakA", description = "Broke a block faster than its computed break time.", decay = 0.05)
public final class FastBreakA extends Check implements PacketCheck, BlockBreakCheck, ConfidenceCheck {

    private static final double MS_PER_TICK = 50.0;
    private static final double LENIENCY_TICKS = 1.5;
    private static final int SUSTAIN = 2;

    private Vec3i startPos;
    private long startTime;
    private int fastBreaks;
    private double innocence = 1.0;

    public FastBreakA(VeritePlayer player) {
        super(player);
    }

    @Override
    public double innocence() {
        return innocence;
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.PLAYER_DIGGING) return;
        if (event.getDiggingAction() == DiggingAction.START_DIGGING) {
            startPos = event.getDigBlockPosition();
            startTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onBlockBreak(final BlockBreak blockBreak) {
        Vec3i pos = blockBreak.position;
        if (startPos == null || startPos.getX() != pos.getX()
                || startPos.getY() != pos.getY() || startPos.getZ() != pos.getZ()) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        startPos = null;

        Player bukkit = Bukkit.getPlayer(player.getUuid());
        if (bukkit == null) return;

        GameMode gm = bukkit.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;
        Block block = bukkit.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());

        int minTicks = BreakTimeResolver.minBreakTicks(bukkit, block);
        if (minTicks <= 1) return;

        double minMs = (minTicks - LENIENCY_TICKS) * MS_PER_TICK;
        if (minMs <= 0) return;

        if (elapsed < minMs) {
            fastBreaks++;
            if (fastBreaks >= SUSTAIN) {
                setInfo("elapsedMs=" + String.format("%.3f", (double) elapsed) + " minMs=" + String.format("%.3f", minMs));
                innocence = Math.max(0.0, innocence - 0.5);
            }
        } else {
            fastBreaks = Math.max(0, fastBreaks - 1);
            innocence = Math.min(1.0, innocence + 0.3);
        }
    }
}
