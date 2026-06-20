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

package teacommontea.veritechasse.player;

import teacommontea.veritechasse.net.GameMode;
import teacommontea.veritechasse.net.NetUser;
import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.check.CheckHolder;
import teacommontea.veritechasse.engine.entity.EntityTracker;
import teacommontea.veritechasse.engine.latency.CompensatedWorld;
import teacommontea.veritechasse.engine.latency.TransactionTracker;
import teacommontea.veritechasse.engine.latency.WorldSnapshot;
import teacommontea.veritechasse.replay.ReplayRecorder;

import java.util.UUID;

public final class VeritePlayer {

    private final AntiCheat antiCheat;
    private final UUID uuid;
    private final NetUser user;
    private final TransactionTracker transactions;
    private final CompensatedWorld compensatedWorld;
    private final CheckHolder checks;
    private final ReplayRecorder replay;

    public final WorldSnapshot snapshot = new WorldSnapshot();
    public final WorldSnapshot.VehicleSnapshot vehicle = new WorldSnapshot.VehicleSnapshot();
    public final EntityTracker entityTracker = new EntityTracker();
    public final teacommontea.veritechasse.engine.rotation.RotationData rotations =
            new teacommontea.veritechasse.engine.rotation.RotationData();

    public double x, y, z;
    public double lastX, lastY, lastZ;
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    public boolean guiOpen;
    public long lastGuiOpenChange;
    public long lastExternalEvent;
    public long lastVelocity;

    public double appliedKbX, appliedKbY, appliedKbZ;
    public int appliedKbTick;
    public boolean kbPending;

    public int lastAttackTick = Integer.MIN_VALUE;

    public String clientBrand = "";
    public boolean bedrockClient;
    public boolean knownClient;

    public boolean alertsEnabled = true;
    public boolean guiMoveExempt;
    public boolean onGround;
    public boolean sprinting;
    public GameMode gamemode = GameMode.SURVIVAL;
    public int heldSlot;
    public double attackSpeed = 4.0;
    public boolean disableChecks;

    private double[] possibleEyeHeights = {1.62};

    public VeritePlayer(AntiCheat antiCheat, UUID uuid, NetUser user) {
        this.antiCheat = antiCheat;
        this.uuid = uuid;
        this.user = user;
        this.transactions = new TransactionTracker(user);
        this.compensatedWorld = new CompensatedWorld(transactions);
        this.checks = new CheckHolder(this);
        this.replay = new ReplayRecorder(this);
    }

    public CheckHolder getChecks() {
        return checks;
    }

    public ReplayRecorder getReplay() {
        return replay;
    }

    public int currentTick() {
        return antiCheat.getTickManager().currentTick();
    }

    public NetUser getUser() {
        return user;
    }

    public TransactionTracker getTransactions() {
        return transactions;
    }

    public CompensatedWorld getCompensatedWorld() {
        return compensatedWorld;
    }

    public double[] getPossibleEyeHeights() {
        return possibleEyeHeights;
    }

    public void setPossibleEyeHeights(double[] heights) {
        this.possibleEyeHeights = heights;
    }

    public AntiCheat getAntiCheat() {
        return antiCheat;
    }

    public UUID getUuid() {
        return uuid;
    }
}
