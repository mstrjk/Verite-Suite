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

import java.util.List;

public final class ReplayRecord {

    public String id;
    public String suspect;
    public String suspectName;
    public long savedAt;
    public int flagTick;
    public String check;
    public String evidence;
    public String worldName;
    public String skinValue;
    public String skinSignature;
    public List<Frame> frames;
    public List<Event> events;

    public static final class Frame {
        public int t;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public int flags;
    }

    public static final class Event {
        public int t;
        public String type;
        public int target;
        public int x;
        public int y;
        public int z;
        public String detail;
    }
}
