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

package teacommontea.veritesauver;

import teacommontea.veritesauver.util.SauverMessages;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SauverListeners implements Listener {

    private final Sauver sauver;

    public SauverListeners(Sauver sauver) {
        this.sauver = sauver;
    }

    private SauverDAO dao() {
        return sauver.dao();
    }

    private final java.util.Map<UUID, String> pendingRealIp = new java.util.concurrent.ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String ip = realClientIp(event);
        if (ip != null) {
            pendingRealIp.put(uuid, ip);
        }
        long now = System.currentTimeMillis();

        Entry block = resolveLoginBan(uuid, ip, now);
        if (block != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    SauverMessages.screen(SauverEngine.banScreen(block)));
            return;
        }

        if (SauverLockdown.active() && !hasOfflineBypass(uuid)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    SauverMessages.screen("<#FF5555><bold>Server locked down.</bold><newline><newline><#FFFFFF>"
                            + SauverLockdown.reason()));
        }
    }

    private boolean hasOfflineBypass(UUID uuid) {
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = provider.getMethod("get").invoke(null);
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) {
                return false;
            }
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object result = permData.getClass().getMethod("checkPermission", String.class)
                    .invoke(permData, "veritesauver.lockdown.bypass");
            Object asBool = result.getClass().getMethod("asBoolean").invoke(result);
            return asBool instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    private Entry resolveLoginBan(UUID uuid, String ip, long now) {
        Entry own = dao().activeBan(uuid);
        if (own != null && own.inForce(now)) {
            return own;
        }
        Entry ipBan = dao().activeIpPunishment(Entry.Type.BAN, ip, now);
        if (ipBan != null) {
            return ipBan;
        }
        if (SauverConfig.banAlts()) {
            Entry alt = dao().bannedAltOnIp(uuid, ip, now);
            if (alt != null) {
                return alt;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        String ip = pendingRealIp.remove(p.getUniqueId());
        if (ip == null) {
            ip = p.getAddress() == null || p.getAddress().getAddress() == null
                    ? null : p.getAddress().getAddress().getHostAddress();
        }

        dao().recordLogin(p.getUniqueId(), p.getName(), ip, System.currentTimeMillis());
        recordClientDetails(p);
        notifyDupeIp(p, ip);
    }

    private static String realClientIp(AsyncPlayerPreLoginEvent event) {
        String injected = parseForwardedHost(event.getHostname());
        if (injected != null) {
            return injected;
        }
        return event.getAddress() == null ? null : event.getAddress().getHostAddress();
    }

    private static String parseForwardedHost(String hostname) {
        if (hostname == null || !hostname.contains("///")) {
            return null;
        }
        String[] parts = hostname.split("///");
        if (parts.length < 2) {
            return null;
        }
        String candidate = parts[1].trim();
        int colon = candidate.lastIndexOf(':');
        if (colon > 0 && candidate.indexOf(':') == colon) {

            candidate = candidate.substring(0, colon);
        }
        return candidate.isEmpty() ? null : candidate;
    }

    private void recordClientDetails(Player p) {
        int protocol = protocolOf(p);
        String referrer = virtualHostOf(p);
        dao().recordClient(p.getUniqueId(), null, protocol, referrer, System.currentTimeMillis());
        sauver.plugin().getServer().getScheduler().runTaskLater(sauver.plugin(), () -> {
            if (!p.isOnline()) {
                return;
            }
            String brand = p.getClientBrandName();
            if (brand == null || brand.isBlank()) {
                brand = "vanilla";
            }
            dao().recordClient(p.getUniqueId(), brand, 0, null, System.currentTimeMillis());
        }, 40L);
    }

    private static int protocolOf(Player p) {
        try {
            Object v = Player.class.getMethod("getProtocolVersion").invoke(p);
            return v instanceof Integer i ? i : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String virtualHostOf(Player p) {
        try {
            Object host = Player.class.getMethod("getVirtualHost").invoke(p);
            if (host instanceof java.net.InetSocketAddress addr) {
                return addr.getHostString() + ":" + addr.getPort();
            }
            return host == null ? null : String.valueOf(host);
        } catch (Throwable t) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID u = event.getPlayer().getUniqueId();
        pendingRealIp.remove(u);
        dao().recordLogout(u, System.currentTimeMillis());
    }

    private void notifyDupeIp(Player joining, String ip) {
        if (ip == null) {
            return;
        }
        long now = System.currentTimeMillis();
        List<UUID> shared = dao().usersOfIp(ip);
        List<String> alts = new ArrayList<>();
        boolean flagged = false;
        for (UUID other : shared) {
            if (other.equals(joining.getUniqueId())) {
                continue;
            }
            String name = dao().nameOf(other);
            if (name == null) {
                name = other.toString().substring(0, 8);
            }
            Entry ban = dao().activeBan(other);
            Entry mute = dao().activeMute(other);
            boolean banned = ban != null && ban.inForce(now);
            boolean muted = mute != null && mute.inForce(now);
            if (banned || muted) {
                flagged = true;
                alts.add("<#FF5555>" + name + (banned ? " (banned)" : " (muted)"));
            } else {
                alts.add("<#FFFFFF>" + name);
            }
        }
        if (alts.isEmpty()) {
            return;
        }
        String head = (flagged ? "<#FF5555>⚠ " : "<#FFFFFF>") + "<#FFFFFF>" + joining.getName()
                + " <#FFFFFF>shares an IP with: " + String.join("<#FFFFFF>, ", alts);
        for (Player staff : joining.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("veritesauver.notify.dupeip_join")) {
                sauver.messages().send(staff, head);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player p = event.getPlayer();
        SauverChat chat = sauver.chat();
        if (chat.mutedGate(p) || chat.slowmodeGate(p)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMutedCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        Entry mute = sauver.activeMute(p.getUniqueId());
        if (mute == null) {
            return;
        }
        if (isBlockedWhileMuted(event.getMessage())) {
            event.setCancelled(true);
            sauver.messages().send(p, SauverEngine.muteLine(mute));
        }
    }

    private static boolean isBlockedWhileMuted(String message) {
        String line = message.startsWith("/") ? message.substring(1) : message;
        int sp = line.indexOf(' ');
        String word = (sp < 0 ? line : line.substring(0, sp)).toLowerCase(Locale.ROOT);
        if (word.isEmpty()) {
            return false;
        }
        teacommontea.eve.Eve eve = compiledBlacklist();
        return eve != null && !eve.scan(word, word).isEmpty();
    }

    private static List<String> blacklistSource;
    private static teacommontea.eve.Eve blacklistCompiled;

    private static synchronized teacommontea.eve.Eve compiledBlacklist() {
        List<String> source = SauverConfig.muteCommandBlacklist();
        if (source.equals(blacklistSource)) {
            return blacklistCompiled;
        }
        blacklistSource = new ArrayList<>(source);
        if (!teacommontea.eve.Eve.nativeAvailable()) {
            blacklistCompiled = null;
            return null;
        }
        StringBuilder src = new StringBuilder();
        for (String stmt : source) {
            src.append(stmt).append('\n');
        }
        try {
            blacklistCompiled = teacommontea.eve.Eve.parse(src.toString());
        } catch (Throwable t) {
            Sauver s = Sauver.instance();
            if (s != null && s.plugin() != null) {
                s.plugin().getLogger().warning("[Veritesauver] mute-blacklist EVE parse failed: " + t.getMessage());
            }
            blacklistCompiled = null;
        }
        return blacklistCompiled;
    }
}
