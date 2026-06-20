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

package teacommontea.veritechasse.api;

import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.entity.Player;

public final class Verite {

    private static AntiCheat antiCheat;

    private Verite() {}

    public static void init(AntiCheat instance) {
        antiCheat = instance;
    }

    public static void exempt(Player player, int ticks) {
        if (antiCheat == null || player == null) return;
        VeritePlayer data = antiCheat.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) {
            data.getChecks().exempt(ticks);
        }
    }

    public static void exempt(Player player) {
        exempt(player, 40);
    }
}
