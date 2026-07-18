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

import org.bukkit.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.UUID;

public final class VanishFeatures {

    private VanishFeatures() {}

    private static final MiniMessage MM = MiniMessage.miniMessage();

    static void registerAll(Plugin plugin, Vanish vanish) {
        VanishSettings s = vanish.settings();

        Bukkit.getPluginManager().registerEvents(new Prevent(vanish, s), plugin);
        if (s.fakeMessage) Bukkit.getPluginManager().registerEvents(new FakeMessage(vanish), plugin);

        new OnVanishState(plugin, vanish);
        if (s.inventoryInspect) Bukkit.getPluginManager().registerEvents(new InventoryInspect(vanish), plugin);
        if (s.serverPing) Bukkit.getPluginManager().registerEvents(new ServerPing(vanish), plugin);
        if (s.gamemode) Bukkit.getPluginManager().registerEvents(new GameModeToggle(plugin, vanish), plugin);
        if (s.rideEntity) Bukkit.getPluginManager().registerEvents(new RideEntity(vanish), plugin);
        if (s.silentContainer) Bukkit.getPluginManager().registerEvents(new SilentContainer(plugin, vanish), plugin);
        if (s.actionbar) startActionbar(plugin, vanish);
        VanishPlaceholders.registerIfAvailable();
    }

    private static boolean v(Vanish vanish, UUID id) {
        return vanish.isVanished(id);
    }

    public static final class Prevent implements Listener {
        private final Vanish vanish;
        private final VanishSettings s;
        Prevent(Vanish vanish, VanishSettings s) { this.vanish = vanish; this.s = s; }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPickup(EntityPickupItemEvent e) {
            if (s.preventPickup && e.getEntity() instanceof Player p && v(vanish, p.getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onInteract(PlayerInteractEvent e) {
            if (s.preventInteract && v(vanish, e.getPlayer().getUniqueId())
                    && e.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
                e.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onBreak(BlockBreakEvent e) {
            if (s.preventBlockBreak && v(vanish, e.getPlayer().getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPlace(BlockPlaceEvent e) {
            if (s.preventBlockPlace && v(vanish, e.getPlayer().getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onTarget(EntityTargetLivingEntityEvent e) {
            if (s.preventTarget && e.getTarget() instanceof Player p && v(vanish, p.getUniqueId())) {
                e.setCancelled(true);
                e.setTarget(null);
            }
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onDamage(EntityDamageEvent e) {
            if (s.preventDamage && e.getEntity() instanceof Player p && v(vanish, p.getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onFood(FoodLevelChangeEvent e) {
            if (s.preventFood && e.getEntity() instanceof Player p && v(vanish, p.getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onDrop(org.bukkit.event.player.PlayerDropItemEvent e) {
            if (s.preventDrop && v(vanish, e.getPlayer().getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent e) {
            if (s.preventBuckets && v(vanish, e.getPlayer().getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent e) {
            if (s.preventBuckets && v(vanish, e.getPlayer().getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onDamageBy(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
            if (s.preventDamage && e.getDamager() instanceof Player p && v(vanish, p.getUniqueId())) e.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onAdvancement(org.bukkit.event.player.PlayerAdvancementDoneEvent e) {
            if (s.preventAdvancement && v(vanish, e.getPlayer().getUniqueId())) {
                e.message(null);
            }
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onChat(io.papermc.paper.event.player.AsyncChatEvent e) {
            if (!s.preventChat) return;
            Player sender = e.getPlayer();
            if (!v(vanish, sender.getUniqueId())) return;
            e.viewers().removeIf(viewer ->
                    viewer instanceof Player pv && !pv.equals(sender) && !Vanish.canSee(pv, sender));
        }
    }

    public static final class FakeMessage implements Listener {
        private final Vanish vanish;
        FakeMessage(Vanish vanish) { this.vanish = vanish; }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onJoin(PlayerJoinEvent e) {
            if (v(vanish, e.getPlayer().getUniqueId())) {
                e.joinMessage(null);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onQuit(PlayerQuitEvent e) {
            if (v(vanish, e.getPlayer().getUniqueId())) {
                e.quitMessage(null);
            }
        }
    }

    public static final class OnVanishState implements VanishEvents.Listener {
        private final Vanish vanish;
        private final VanishSettings s;
        OnVanishState(Plugin plugin, Vanish vanish) {
            this.vanish = vanish;
            this.s = vanish.settings();
            VanishEvents.register(this);
        }

        @Override
        public void vanished(UUID id) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) return;
            p.setCollidable(false);
            p.setSleepingIgnored(true);
            if (s.fly && p.hasPermission("verite.vanish.fly")) {
                p.setAllowFlight(true);
                p.setFlying(true);
            }
            if (s.invulnerability && p.hasPermission("verite.vanish.invulnerable")) {
                p.setInvulnerable(true);
            }
            applyEffects(p);
        }

        @Override
        public void unvanished(UUID id) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) return;
            p.setCollidable(true);
            p.setSleepingIgnored(false);
            p.setInvulnerable(false);
            if (!p.hasPermission("verite.vanish.fly.keep")
                    && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            removeEffects(p);
        }

        private void applyEffects(Player p) {
            if (!s.effects || !p.hasPermission("verite.vanish.effects")) return;
            for (org.bukkit.potion.PotionEffectType type : EFFECT_TYPES) {
                if (type != null) {

                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            type, Integer.MAX_VALUE, 0, true, false, false));
                }
            }
        }

        private void removeEffects(Player p) {
            for (org.bukkit.potion.PotionEffectType type : EFFECT_TYPES) {
                if (type != null) p.removePotionEffect(type);
            }
        }
    }

    private static final org.bukkit.potion.PotionEffectType[] EFFECT_TYPES = {
            org.bukkit.potion.PotionEffectType.getByName("NIGHT_VISION"),
            org.bukkit.potion.PotionEffectType.getByName("WATER_BREATHING"),
            org.bukkit.potion.PotionEffectType.getByName("FIRE_RESISTANCE"),
    };

    public static final class GameModeToggle implements Listener {
        private final Plugin plugin;
        private final Vanish vanish;
        private final java.util.Map<UUID, GameMode> saved = new java.util.HashMap<>();
        private final java.util.Set<UUID> sneakWindow = java.util.concurrent.ConcurrentHashMap.newKeySet();
        GameModeToggle(Plugin plugin, Vanish vanish) { this.plugin = plugin; this.vanish = vanish; }

        @EventHandler(ignoreCancelled = true)
        public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent e) {
            Player p = e.getPlayer();
            if (!e.isSneaking() || !v(vanish, p.getUniqueId())) return;
            UUID id = p.getUniqueId();
            if (sneakWindow.contains(id)) {

                if (p.getGameMode() == GameMode.SPECTATOR) {
                    p.setGameMode(saved.getOrDefault(id, GameMode.SURVIVAL));
                } else {
                    saved.put(id, p.getGameMode());
                    p.setGameMode(GameMode.SPECTATOR);
                }
                sneakWindow.remove(id);
            } else {
                if (p.getGameMode() != GameMode.SPECTATOR) saved.put(id, p.getGameMode());
                sneakWindow.add(id);
                Bukkit.getScheduler().runTaskLater(plugin, () -> sneakWindow.remove(id), 8L);
            }
        }
    }

    public static final class RideEntity implements Listener {
        private final Vanish vanish;
        RideEntity(Vanish vanish) { this.vanish = vanish; }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onMount(org.bukkit.event.player.PlayerInteractEntityEvent e) {
            for (org.bukkit.entity.Entity passenger : e.getRightClicked().getPassengers()) {
                if (passenger instanceof Player pp && v(vanish, pp.getUniqueId())) {
                    pp.leaveVehicle();
                }
            }
        }
    }

    public static final class SilentContainer implements Listener {
        private final Plugin plugin;
        private final Vanish vanish;
        private final java.util.Map<UUID, GameMode> restore = new java.util.HashMap<>();
        SilentContainer(Plugin plugin, Vanish vanish) { this.plugin = plugin; this.vanish = vanish; }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onInteract(PlayerInteractEvent e) {
            Player p = e.getPlayer();
            if (!v(vanish, p.getUniqueId())) return;
            if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
            org.bukkit.block.Block b = e.getClickedBlock();
            if (b == null) return;
            if (p.getGameMode() == GameMode.SPECTATOR) return;
            if (b.getType() == org.bukkit.Material.ENDER_CHEST) {
                e.setCancelled(true);
                p.openInventory(p.getEnderChest());
                return;
            }
            if (!(b.getState() instanceof org.bukkit.block.Container container)) return;
            e.setCancelled(true);
            restore.put(p.getUniqueId(), p.getGameMode());
            p.setGameMode(GameMode.SPECTATOR);
            p.openInventory(container.getInventory());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                GameMode gm = restore.remove(p.getUniqueId());
                if (gm != null) p.setGameMode(gm);
            }, 1L);
        }
    }

    public static final class InventoryInspect implements Listener {
        private final Vanish vanish;
        InventoryInspect(Vanish vanish) { this.vanish = vanish; }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onInteractPlayer(org.bukkit.event.player.PlayerInteractEntityEvent e) {
            Player staff = e.getPlayer();
            if (!v(vanish, staff.getUniqueId())) return;
            if (!staff.isSneaking()) return;
            if (!(e.getRightClicked() instanceof Player target)) return;
            e.setCancelled(true);
            staff.openInventory(target.getInventory());
        }
    }

    public static final class ServerPing implements Listener {
        private final Vanish vanish;
        ServerPing(Vanish vanish) { this.vanish = vanish; }

        @EventHandler(priority = EventPriority.HIGH)
        public void onPing(com.destroystokyo.paper.event.server.PaperServerListPingEvent e) {
            int vanishedOnline = 0;
            for (UUID id : vanish.getVanished()) {
                if (Bukkit.getPlayer(id) != null) vanishedOnline++;
            }
            if (vanishedOnline > 0) {
                e.setNumPlayers(Math.max(0, e.getNumPlayers() - vanishedOnline));
            }

            e.getPlayerSample().removeIf(profile ->
                    profile.getId() != null && vanish.isVanished(profile.getId()));
        }
    }

    private static void startActionbar(Plugin plugin, Vanish vanish) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (vanish.getVanished().isEmpty()) return;
            for (UUID id : vanish.getVanished()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.sendActionBar(MM.deserialize("<#B1C7F0>You are <#FFFFFF>vanished"));
                }
            }
        }, 20L, 40L);
    }
}
