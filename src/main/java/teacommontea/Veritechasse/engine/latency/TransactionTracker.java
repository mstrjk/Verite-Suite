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

package teacommontea.veritechasse.engine.latency;

import net.minecraft.network.protocol.common.ClientboundPingPacket;
import teacommontea.veritechasse.net.NetUser;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class TransactionTracker {

    private final NetUser user;
    private final LatencyTaskQueue taskQueue = new LatencyTaskQueue(this);

    private final AtomicInteger transactionIdCounter = new AtomicInteger(0);
    private final AtomicInteger lastTransactionSent = new AtomicInteger(0);
    private final AtomicInteger lastTransactionReceived = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Short> sentIds = new ConcurrentLinkedQueue<>();

    public TransactionTracker(NetUser user) {
        this.user = user;
    }

    public LatencyTaskQueue getTaskQueue() {
        return taskQueue;
    }

    public int lastTransactionSent() {
        return lastTransactionSent.get();
    }

    public int lastTransactionReceived() {
        return lastTransactionReceived.get();
    }

    public void sendTransaction() {
        if (user == null || !user.isPlayState()) return;

        short transactionId = (short) (-1 * (transactionIdCounter.getAndIncrement() & 0x7FFF));
        try {
            sentIds.add(transactionId);
            lastTransactionSent.incrementAndGet();

            user.writePacket(new ClientboundPingPacket(transactionId));
        } catch (Exception ignored) {
        }
    }

    public boolean handleResponse(int id) {
        short shortId = (short) id;
        if (!sentIds.contains(shortId)) {
            return false;
        }
        Short polled;
        while ((polled = sentIds.poll()) != null) {
            lastTransactionReceived.incrementAndGet();
            if (polled == shortId) break;
        }
        taskQueue.onTransactionReceived(lastTransactionReceived.get());
        return true;
    }

    public boolean isPong(int id) {
        return sentIds.contains((short) id);
    }

    public int outstanding() {
        return sentIds.size();
    }
}
