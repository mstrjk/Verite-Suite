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

package teacommontea.veritechasse.engine.entity;

public final class TrackedEntity {

    private static final int HISTORY = 20;

    public final int id;
    public boolean player;
    public double width = 0.6;
    public double height = 1.8;

    private final double[] histX = new double[HISTORY];
    private final double[] histY = new double[HISTORY];
    private final double[] histZ = new double[HISTORY];
    private int head;
    private int count;

    public double x, y, z;

    public TrackedEntity(int id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        push(x, y, z);
    }

    public void setPosition(double nx, double ny, double nz) {
        this.x = nx;
        this.y = ny;
        this.z = nz;
        push(nx, ny, nz);
    }

    public void move(double dx, double dy, double dz) {
        setPosition(x + dx, y + dy, z + dz);
    }

    private void push(double px, double py, double pz) {
        head = (head + 1) % HISTORY;
        histX[head] = px;
        histY[head] = py;
        histZ[head] = pz;
        if (count < HISTORY) count++;
    }

    public double histXAgo(int ticks) {
        return histX[indexAgo(ticks)];
    }

    public double histYAgo(int ticks) {
        return histY[indexAgo(ticks)];
    }

    public double histZAgo(int ticks) {
        return histZ[indexAgo(ticks)];
    }

    private int indexAgo(int ticks) {
        int t = Math.max(0, Math.min(ticks, count - 1));
        int idx = head - t;
        while (idx < 0) idx += HISTORY;
        return idx;
    }
}
