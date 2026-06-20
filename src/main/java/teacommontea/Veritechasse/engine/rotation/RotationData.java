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

package teacommontea.veritechasse.engine.rotation;

public final class RotationData {

    private static final int HISTORY = 20;

    private final float[] yawDeltas = new float[HISTORY];
    private final float[] pitchDeltas = new float[HISTORY];
    private int head;
    private int count;

    public float yaw, pitch;
    public float lastYaw, lastPitch;
    public float yawDelta, pitchDelta;
    public boolean hasData;

    public final GcdCalibrator yawGcd = new GcdCalibrator();
    public final GcdCalibrator pitchGcd = new GcdCalibrator();

    public void update(float newYaw, float newPitch) {
        if (hasData) {
            lastYaw = yaw;
            lastPitch = pitch;
        } else {
            lastYaw = newYaw;
            lastPitch = newPitch;
        }
        yaw = newYaw;
        pitch = newPitch;

        yawDelta = Math.abs(wrap(yaw - lastYaw));
        pitchDelta = Math.abs(pitch - lastPitch);

        head = (head + 1) % HISTORY;
        yawDeltas[head] = yawDelta;
        pitchDeltas[head] = pitchDelta;
        if (count < HISTORY) count++;
        hasData = true;
    }

    public void calibrate() {
        yawGcd.feed(yawDelta);
        pitchGcd.feed(pitchDelta);
    }

    public int size() {
        return count;
    }

    public float yawDeltaAgo(int ago) {
        return yawDeltas[indexAgo(ago)];
    }

    public float pitchDeltaAgo(int ago) {
        return pitchDeltas[indexAgo(ago)];
    }

    private int indexAgo(int ago) {
        int a = Math.max(0, Math.min(ago, count - 1));
        int idx = head - a;
        while (idx < 0) idx += HISTORY;
        return idx;
    }

    private static float wrap(float deg) {
        float d = deg % 360.0f;
        if (d >= 180.0f) d -= 360.0f;
        if (d < -180.0f) d += 360.0f;
        return d;
    }
}
