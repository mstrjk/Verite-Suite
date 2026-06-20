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

package teacommontea.veritechasse.check;

import teacommontea.veritechasse.player.VeritePlayer;

public abstract class Check {

    protected final VeritePlayer player;

    private final String name;
    private final String description;
    private final double decay;

    private double violations;
    private String info = "";

    protected Check(VeritePlayer player) {
        this.player = player;
        CheckInfo info = getClass().getAnnotation(CheckInfo.class);
        if (info == null) {
            throw new IllegalStateException(getClass().getName() + " is missing @CheckInfo");
        }
        this.name = info.name();
        this.description = info.description();
        this.decay = info.decay();
    }

    protected final void flag(String reason) {

        if (!player.getAntiCheat().checkSettings().familyEnabled(name)) {
            return;
        }
        violations++;
        player.getAntiCheat().getViolationManager().handle(this, reason);
    }

    protected final void flag() {
        flag("");
    }

    protected final void reward() {
        violations = Math.max(0.0, violations - decay);
    }

    public final String formatOffset(double offset) {
        return offset > 0.001 ? String.format("%.5f", offset) : String.format("%.2E", offset);
    }

    public final VeritePlayer getPlayer() {
        return player;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final double getViolations() {
        return violations;
    }

    protected final void setInfo(String info) {
        this.info = info == null ? "" : info;
    }

    public final String getInfo() {
        return info;
    }
}
