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

public final class ReplayFrame {

    public static final int FLAG_ON_GROUND = 1;
    public static final int FLAG_SPRINTING = 1 << 1;
    public static final int FLAG_SNEAKING = 1 << 2;
    public static final int FLAG_SWIMMING = 1 << 3;
    public static final int FLAG_GLIDING = 1 << 4;
    public static final int FLAG_IN_WATER = 1 << 5;

    public final int tick;
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final int flags;

    public ReplayFrame(int tick, double x, double y, double z, float yaw, float pitch, int flags) {
        this.tick = tick;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.flags = flags;
    }
}
