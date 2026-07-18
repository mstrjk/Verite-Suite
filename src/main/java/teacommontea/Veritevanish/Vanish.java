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

package teacommontea.veritevanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import teacommontea.veritesauver.store.SauverStore;
import teacommontea.veritesauver.store.Scope;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Vanish {

    private static Vanish instance;

    private final Plugin plugin;
    private final SauverStore store;
    private final Scope scope;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final VanishSettings settings;

    private static final String SCOPE = "vanish";

    private Vanish(Plugin plugin, SauverStore store, VanishSettings settings) {
        this.plugin = plugin;
        this.store = store;
        this.scope = store.scope(SCOPE);
        this.settings = settings;
    }

    VanishSettings settings() {
        return settings;
    }

    public static Vanish instance() {
        return instance;
    }

    public Plugin plugin() {
        return plugin;
    }

    public static Vanish enable(Plugin pl) throws Exception {
        SauverStore store = SauverStore.open(pl, "vanish.bin");
        Vanish v = new Vanish(pl, store, VanishSettings.load(pl));
        instance = v;
        v.restore();
        pl.getServer().getPluginManager().registerEvents(new VanishListener(v), pl);
        VanishFeatures.registerAll(pl, v);
        pl.getLogger().info("[Verite] Veritevanish up (" + v.vanished.size() + " vanished restored).");
        return v;
    }

    public void disable() {

        for (UUID id : new HashSet<>(vanished)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                showToAll(p);
            }
        }
        store.shutdown();
        instance = null;
    }

    private void restore() {
        for (Scope.Entry e : scope.entries()) {
            try {
                UUID id = UUID.fromString(e.key());
                vanished.add(id);
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    hideFromAll(p);
                }
            } catch (IllegalArgumentException ignored) {

            }
        }
    }

    public boolean isVanished(UUID player) {
        return player != null && vanished.contains(player);
    }

    public Set<UUID> getVanished() {
        return Collections.unmodifiableSet(new HashSet<>(vanished));
    }

    public boolean vanish(Player p) {
        if (p == null) return false;
        UUID id = p.getUniqueId();
        if (vanished.contains(id)) return true;

        VeriteVanishEvent ev = new VeriteVanishEvent(p, true);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;

        vanished.add(id);
        scope.set(id.toString(), true);
        hideFromAll(p);
        VanishEvents.fireVanished(id);
        return true;
    }

    public boolean unvanish(Player p) {
        if (p == null) return false;
        UUID id = p.getUniqueId();
        if (!vanished.contains(id)) return true;

        VeriteVanishEvent ev = new VeriteVanishEvent(p, false);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;

        vanished.remove(id);
        scope.delete(id.toString());
        showToAll(p);
        VanishEvents.fireUnvanished(id);
        return true;
    }

    public boolean toggle(Player p) {
        return isVanished(p.getUniqueId()) ? !unvanish(p) : vanish(p);
    }

    private void hideFromAll(Player p) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(p)) continue;
            if (!canSee(viewer, p)) {
                viewer.hidePlayer(plugin, p);
            } else if (settings.ghost) {

                VanishGhost.ghostFor(viewer, p);
            }
        }
    }

    private void showToAll(Player p) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(p)) continue;
            viewer.showPlayer(plugin, p);
        }
    }

    static boolean canSee(Player viewer, Player hidden) {
        if (viewer == null || !viewer.hasPermission("verite.vanish.see")) return false;
        return tierOf(viewer) >= tierOf(hidden);
    }

    static boolean canSee(Player viewer) {
        return viewer != null && viewer.hasPermission("verite.vanish.see");
    }

    private static int tierOf(Player p) {
        return VanishLevel.tierOf(p);
    }

    void applyOnJoin(Player joiner) {
        for (UUID id : vanished) {
            Player hidden = Bukkit.getPlayer(id);
            if (hidden != null && !hidden.equals(joiner) && !canSee(joiner, hidden)) {
                joiner.hidePlayer(plugin, hidden);
            }
        }
        if (vanished.contains(joiner.getUniqueId())) {
            hideFromAll(joiner);
        }
    }
}
