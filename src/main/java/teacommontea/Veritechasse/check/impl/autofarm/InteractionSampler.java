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

package teacommontea.veritechasse.check.impl.autofarm;

import java.util.ArrayDeque;
import java.util.Deque;

public final class InteractionSampler {

    public static final class Interaction {
        public final long time;
        public final int x, y, z;
        public final float yaw, pitch;

        Interaction(long time, int x, int y, int z, float yaw, float pitch) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final int MAX = 40;
    private final Deque<Interaction> interactions = new ArrayDeque<>(MAX);

    public void record(long time, int x, int y, int z, float yaw, float pitch) {
        if (interactions.size() == MAX) interactions.pollFirst();
        interactions.addLast(new Interaction(time, x, y, z, yaw, pitch));
    }

    public int size() {
        return interactions.size();
    }

    public Interaction[] toArray() {
        return interactions.toArray(new Interaction[0]);
    }

    public Interaction last() {
        return interactions.peekLast();
    }
}
