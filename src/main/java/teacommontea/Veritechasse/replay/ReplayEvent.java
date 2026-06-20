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

package teacommontea.veritechasse.replay;

public final class ReplayEvent {

    public enum Type {
        SWING,
        ATTACK,
        USE,
        PLACE,
        BREAK,
        HURT,
        FLAG
    }

    public final int tick;
    public final Type type;
    public final int targetId;
    public final int x;
    public final int y;
    public final int z;
    public final String detail;

    public ReplayEvent(int tick, Type type, int targetId, int x, int y, int z, String detail) {
        this.tick = tick;
        this.type = type;
        this.targetId = targetId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.detail = detail == null ? "" : detail;
    }
}
