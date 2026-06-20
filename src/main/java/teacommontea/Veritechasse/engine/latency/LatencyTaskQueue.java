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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

public final class LatencyTaskQueue {

    private final LinkedList<Map.Entry<Integer, Runnable>> transactionMap = new LinkedList<>();
    private final ArrayList<Runnable> tasksToRun = new ArrayList<>();
    private final TransactionTracker tracker;

    public LatencyTaskQueue(TransactionTracker tracker) {
        this.tracker = tracker;
    }

    public void addRealTimeTask(int transaction, Runnable runnable) {
        if (tracker.lastTransactionReceived() >= transaction) {
            runnable.run();
            return;
        }
        synchronized (this) {
            transactionMap.add(Map.entry(transaction, runnable));
        }
    }

    public void onTransactionReceived(int transaction) {
        synchronized (this) {
            tasksToRun.clear();

            ListIterator<Map.Entry<Integer, Runnable>> iterator = transactionMap.listIterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Runnable> pair = iterator.next();

                if (transaction + 1 < pair.getKey()) break;
                if (transaction == pair.getKey() - 1) continue;

                tasksToRun.add(pair.getValue());
                iterator.remove();
            }

            for (Runnable runnable : tasksToRun) {
                try {
                    runnable.run();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
