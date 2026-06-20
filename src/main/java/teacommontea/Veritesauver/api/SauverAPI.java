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

package teacommontea.veritesauver.api;

import teacommontea.veritesauver.Entry;
import teacommontea.veritesauver.Sauver;
import teacommontea.veritesauver.SauverEngine;
import teacommontea.veritesauver.SauverEvents;

import java.util.List;
import java.util.UUID;

public final class SauverAPI {

    private SauverAPI() {}

    private static Sauver sauver() {
        return Sauver.instance();
    }

    public static boolean enabled() {
        return sauver() != null;
    }

    public static boolean isBanned(UUID player) {
        return enabled() && sauver().activeBan(player) != null;
    }

    public static boolean isMuted(UUID player) {
        return enabled() && sauver().activeMute(player) != null;
    }

    public static Entry activeBan(UUID player) {
        return enabled() ? sauver().activeBan(player) : null;
    }

    public static Entry activeMute(UUID player) {
        return enabled() ? sauver().activeMute(player) : null;
    }

    public static String country(String ip) {
        return enabled() ? teacommontea.veritesauver.SauverGeoIp.country(ip) : null;
    }

    public static boolean muteGate(org.bukkit.entity.Player p) {
        if (!enabled()) {
            return false;
        }
        Entry mute = sauver().activeMute(p.getUniqueId());
        if (mute == null) {
            return false;
        }
        sauver().messages().send(p, teacommontea.veritesauver.SauverEngine.muteLine(mute));
        return true;
    }

    public static boolean chatMuteGate(org.bukkit.entity.Player p) {
        return enabled() && sauver().chat().mutedGate(p);
    }

    public static boolean slowmodeGate(org.bukkit.entity.Player p) {
        return enabled() && sauver().chat().slowmodeGate(p);
    }

    public static List<Entry> history(UUID player, int limit) {
        return enabled() ? sauver().dao().history(player, limit) : List.of();
    }

    public static List<Entry> activeWarnings(UUID player) {
        return enabled() ? sauver().dao().activeWarnings(player, System.currentTimeMillis()) : List.of();
    }

    public static List<UUID> altsOf(UUID player) {
        if (!enabled()) {
            return List.of();
        }
        List<String> ips = sauver().dao().ipsOf(player);
        if (ips.isEmpty()) {
            return List.of();
        }
        return sauver().dao().usersOfIp(ips.get(0));
    }

    public static Entry ban(UUID target, String targetName, String reason,
                            UUID executor, String executorName, long durationMillis, boolean silent) {
        if (!enabled()) return null;
        SauverEngine.Result r = SauverEngine.issue(Entry.Type.BAN, target, null, targetName,
                reason, executor, executorName, durationMillis, silent, false);
        return r.ok() ? r.entry() : null;
    }

    public static Entry mute(UUID target, String targetName, String reason,
                             UUID executor, String executorName, long durationMillis, boolean silent) {
        if (!enabled()) return null;
        SauverEngine.Result r = SauverEngine.issue(Entry.Type.MUTE, target, null, targetName,
                reason, executor, executorName, durationMillis, silent, false);
        return r.ok() ? r.entry() : null;
    }

    public static Entry warn(UUID target, String targetName, String reason,
                             UUID executor, String executorName, boolean silent) {
        if (!enabled()) return null;
        SauverEngine.Result r = SauverEngine.warn(target, targetName, reason, executor, executorName, silent);
        return r.ok() ? r.entry() : null;
    }

    public static Entry kick(UUID target, String targetName, String reason,
                             UUID executor, String executorName, boolean silent) {
        if (!enabled()) return null;
        SauverEngine.Result r = SauverEngine.kick(target, targetName, reason, executor, executorName, silent);
        return r.ok() ? r.entry() : null;
    }

    public static boolean unban(UUID target, String targetName, UUID remover, String removerName, String reason) {
        return enabled() && SauverEngine.pardon(Entry.Type.BAN, target, targetName, remover, removerName, reason).ok();
    }

    public static boolean unmute(UUID target, String targetName, UUID remover, String removerName, String reason) {
        return enabled() && SauverEngine.pardon(Entry.Type.MUTE, target, targetName, remover, removerName, reason).ok();
    }

    public static void registerListener(SauverEvents.Listener listener) {
        SauverEvents.register(listener);
    }

    public static void unregisterListener(SauverEvents.Listener listener) {
        SauverEvents.unregister(listener);
    }
}
