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

package teacommontea.veritechasse.listener;

import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import teacommontea.veritechasse.net.BlockFace;
import teacommontea.veritechasse.net.NetUser;
import teacommontea.veritechasse.net.Vec3f;
import teacommontea.veritechasse.net.Vec3i;
import teacommontea.veritechasse.player.BlockBreak;
import teacommontea.veritechasse.player.BlockPlace;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public final class BukkitListener implements Listener {

    private final AntiCheat antiCheat;

    public BukkitListener(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        NetUser user = antiCheat.getInjector().inject(p);
        antiCheat.getPlayerDataManager().add(p.getUniqueId(), user);
        announceJoin(p);
    }

    private void announceJoin(Player p) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(antiCheat.getPlugin(), () -> {
            if (!p.isOnline()) return;
            String brand = p.getClientBrandName();
            if (brand == null || brand.isEmpty()) {
                brand = "vanilla";
            }
            VeritePlayer data = antiCheat.getPlayerDataManager().get(p.getUniqueId());
            if (data != null && data.clientBrand.isEmpty()) {
                data.clientBrand = brand.toLowerCase();
            }
            net.kyori.adventure.text.Component message = antiCheat.messages().prefixed(
                    "<white>" + p.getName() + " <white>joined using <#B1C7F0>" + brand + "<white>.");
            for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (online.isOp()) {
                    online.sendMessage(message);
                }
            }
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        VeritePlayer data = antiCheat.getPlayerDataManager().get(event.getPlayer().getUniqueId());
        if (data != null) {
            antiCheat.getInjector().eject(data.getUser());
        }
        antiCheat.getPlayerDataManager().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        VeritePlayer player = antiCheat.getPlayerDataManager().get(event.getPlayer().getUniqueId());
        if (player != null) {
            player.getChecks().markTeleport();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        VeritePlayer player = antiCheat.getPlayerDataManager().get(event.getPlayer().getUniqueId());
        if (player != null) {
            player.getChecks().markTeleport();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player != null) {
            player.lastVelocity = System.currentTimeMillis();
            player.getChecks().markVelocity();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player p = event.getPlayer();
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player == null) return;
        org.bukkit.util.Vector v = event.getVelocity();
        player.appliedKbX = v.getX();
        player.appliedKbY = v.getY();
        player.appliedKbZ = v.getZ();
        player.appliedKbTick = player.currentTick();
        player.kbPending = true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player != null) {
            player.getChecks().onBowShoot(antiCheat.getTickManager().currentTick());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInvOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player == null) return;
        if (event.getView().getType() != InventoryType.CRAFTING) {
            player.guiOpen = true;
            player.lastGuiOpenChange = System.currentTimeMillis();
        }
        org.bukkit.inventory.Inventory inv = event.getInventory();
        if (inv.getLocation() != null) {
            player.getChecks().onContainerOpen(p.getLocation(), inv.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player p = event.getPlayer();
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player == null) return;
        if (event.getState() == PlayerFishEvent.State.BITE) {
            player.getChecks().onFishBite();
        } else if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH
                || event.getState() == PlayerFishEvent.State.REEL_IN) {
            player.getChecks().onFishReel();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player == null) return;
        if (event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK
                || event.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR
                || event.getAction() == org.bukkit.event.inventory.InventoryAction.NOTHING) {
            return;
        }
        player.getChecks().onInventoryClick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInvClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player == null) return;
        player.guiOpen = false;
        player.lastGuiOpenChange = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        VeritePlayer player = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        if (player == null) return;
        player.lastExternalEvent = System.currentTimeMillis();

        Block b = event.getBlockPlaced();
        Block against = event.getBlockAgainst();
        Vec3i pos = new Vec3i(b.getX(), b.getY(), b.getZ());
        org.bukkit.block.BlockFace bf = b.getFace(against);
        BlockFace face = toFace(bf);
        org.bukkit.Material replaced = event.getBlockReplacedState().getType();
        BlockPlace place = new BlockPlace(player, pos, face, b.getType(), replaced, new Vec3f(0.5f, 0.5f, 0.5f));
        player.getChecks().onBlockPlace(place);
    }

    private static BlockFace toFace(org.bukkit.block.BlockFace f) {
        if (f == null) return BlockFace.UP;
        switch (f) {
            case NORTH: return BlockFace.NORTH;
            case SOUTH: return BlockFace.SOUTH;
            case EAST: return BlockFace.EAST;
            case WEST: return BlockFace.WEST;
            case UP: return BlockFace.UP;
            case DOWN: return BlockFace.DOWN;
            default: return BlockFace.UP;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        long now = System.currentTimeMillis();
        for (VeritePlayer player : antiCheat.getPlayerDataManager().all()) {
            player.getCompensatedWorld().updateBlock(b.getX(), b.getY(), b.getZ(), null);
            player.lastExternalEvent = now;
        }

        VeritePlayer breaker = antiCheat.getPlayerDataManager().get(event.getPlayer().getUniqueId());
        if (breaker != null) {
            breaker.getChecks().onBlockBreak(
                    new BlockBreak(new Vec3i(b.getX(), b.getY(), b.getZ())));
        }
    }
}
