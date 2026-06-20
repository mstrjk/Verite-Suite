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

import org.bukkit.plugin.Plugin;

import teacommontea.veritesauver.store.SauverStore;
import teacommontea.veritesauver.util.SauverMessages;

import java.util.UUID;

public final class Sauver {

    private static Sauver instance;

    private final Plugin plugin;
    private final SauverStore store;
    private final SauverDAO dao;
    private final SauverMessages messages;
    private final SauverChat chat;

    private int sweepTask = -1;
    private int playtimeTask = -1;

    private Sauver(Plugin plugin, SauverStore store) {
        this.plugin = plugin;
        this.store = store;
        this.dao = new SauverDAO(store);
        this.messages = new SauverMessages();
        this.chat = new SauverChat(this);
    }

    public static Sauver enable(Plugin pl) throws Exception {
        SauverStore store = SauverStore.open(pl);
        Sauver s = new Sauver(pl, store);
        instance = s;

        pl.getServer().getPluginManager().registerEvents(new SauverListeners(s), pl);
        SauverConfig.load(pl.getDataFolder());
        SauverTemplates.load(pl.getDataFolder());
        SauverGeoIp.load(pl.getDataFolder());

        s.sweepTask = pl.getServer().getScheduler()
                .runTaskTimer(pl, s::sweepExpired, 600L, 600L).getTaskId();

        s.playtimeTask = pl.getServer().getScheduler()
                .runTaskTimer(pl, s::flushPlaytime, 1200L, 1200L).getTaskId();
        return s;
    }

    public void disable() {
        if (sweepTask != -1) {
            plugin.getServer().getScheduler().cancelTask(sweepTask);
            sweepTask = -1;
        }
        if (playtimeTask != -1) {
            plugin.getServer().getScheduler().cancelTask(playtimeTask);
            playtimeTask = -1;
        }

        long now = System.currentTimeMillis();
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            dao.recordLogout(p.getUniqueId(), now);
        }
        store.shutdown();
        if (instance == this) {
            instance = null;
        }
    }

    public void reload() {
        SauverConfig.load(plugin.getDataFolder());
        SauverTemplates.load(plugin.getDataFolder());
        SauverGeoIp.load(plugin.getDataFolder());
    }

    void sweepExpired() {
        long now = System.currentTimeMillis();
        for (SauverDAO.ActivePointer ptr : dao.activePointers()) {
            Entry e = dao.load(ptr.entryId());
            if (e != null && e.active() && e.expired(now)) {
                Entry lapsed = dao.expire(ptr.entryId());
                if (lapsed != null) {
                    SauverEvents.fireRemoved(lapsed);
                }
            }
        }
    }

    void flushPlaytime() {
        long now = System.currentTimeMillis();
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            dao.flushSession(p.getUniqueId(), now);
        }
    }

    public static Sauver instance()     { return instance; }
    public Plugin plugin()              { return plugin; }
    public SauverDAO dao()              { return dao; }
    public SauverStore store()          { return store; }
    public SauverMessages messages()    { return messages; }
    public SauverChat chat()            { return chat; }

    public Entry activeMute(UUID u) {
        Entry own = inForceOrNull(dao.activeMute(u));
        if (own != null) {
            return own;
        }
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(u);
        if (p != null && p.getAddress() != null && p.getAddress().getAddress() != null) {
            String ip = p.getAddress().getAddress().getHostAddress();
            Entry ipMute = dao.activeIpPunishment(Entry.Type.MUTE, ip, System.currentTimeMillis());
            if (ipMute != null) {
                return ipMute;
            }
        }
        return own;
    }

    public Entry activeBan(UUID u) {
        return inForceOrNull(dao.activeBan(u));
    }

    private static Entry inForceOrNull(Entry e) {
        if (e == null) {
            return null;
        }
        return e.inForce(System.currentTimeMillis()) ? e : null;
    }
}
