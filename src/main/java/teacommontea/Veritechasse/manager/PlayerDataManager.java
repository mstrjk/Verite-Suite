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

package teacommontea.veritechasse.manager;

import teacommontea.veritechasse.net.NetUser;
import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {

    private final AntiCheat antiCheat;
    private final Map<UUID, VeritePlayer> players = new ConcurrentHashMap<>();

    public PlayerDataManager(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    public VeritePlayer add(UUID uuid, NetUser user) {
        VeritePlayer player = new VeritePlayer(antiCheat, uuid, user);
        players.put(uuid, player);
        return player;
    }

    public VeritePlayer get(UUID uuid) {
        return players.get(uuid);
    }

    public VeritePlayer remove(UUID uuid) {
        return players.remove(uuid);
    }

    public Iterable<VeritePlayer> all() {
        return players.values();
    }
}
