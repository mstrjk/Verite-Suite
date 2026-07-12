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

package teacommontea.captcha;

import teacommontea.captcha.CaptchaKind;
import teacommontea.captcha.CaptchaOutcome;
import teacommontea.util.Messages;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CaptchaStandard implements Listener {

    private static final int SIZE = 54;
    private static final int MAX_FAILS = 5;
    private static final long TIMEOUT_TICKS = 30 * 20L;

    private static final List<Material> ITEMS = List.of(
            Material.DIAMOND_SWORD, Material.GOLDEN_APPLE, Material.ENCHANTED_BOOK, Material.IRON_PICKAXE,
            Material.BOW, Material.FLINT_AND_STEEL, Material.DIAMOND_HELMET, Material.BLAZE_ROD,
            Material.SLIME_BALL, Material.COMPASS, Material.TOTEM_OF_UNDYING, Material.GHAST_TEAR,
            Material.NETHER_STAR, Material.CLOCK, Material.SHEARS, Material.CARROT_ON_A_STICK,
            Material.SHIELD, Material.ELYTRA, Material.TRIDENT, Material.CROSSBOW,
            Material.MAGMA_CREAM, Material.FIRE_CHARGE, Material.BEACON, Material.RABBIT_FOOT,
            Material.ENDER_EYE, Material.HEART_OF_THE_SEA, Material.ENCHANTED_GOLDEN_APPLE, Material.DRAGON_EGG);

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    private final JavaPlugin plugin;
    private final Messages messages;
    private final CaptchaManager manager;
    private final Random random = new Random();
    private final Map<UUID, Session> active = new ConcurrentHashMap<>();

    public CaptchaStandard(JavaPlugin plugin, Messages messages, CaptchaManager manager) {
        this.plugin = plugin;
        this.messages = messages;
        this.manager = manager;
    }

    public boolean isActive(UUID u) {
        return active.containsKey(u);
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class Session {
        final long start = System.currentTimeMillis();
        final List<String> failed = new ArrayList<>();
        int answerSlot;
        Inventory inventory;
        Location anchor;
        int taskId = -1;
        String check;
        String evidence;
        double score;
    }

    public void open(Player player, String check, String evidence, double score) {
        UUID u = player.getUniqueId();

        List<Material> pool = new ArrayList<>(ITEMS);
        int answerIndex = random.nextInt(pool.size());
        Material answer = pool.remove(answerIndex);

        Session s = new Session();
        s.check = check;
        s.evidence = evidence;
        s.score = score;
        s.answerSlot = SLOTS[answerIndex];

        Holder holder = new Holder();
        String readable = titleCase(answer.name());

        Inventory inv = Bukkit.createInventory(holder, SIZE,
                messages.parse("<dark_gray>Click the " + readable));
        holder.inventory = inv;
        s.inventory = inv;

        ItemStack frame = named(Material.GRAY_STAINED_GLASS_PANE, "<gray>");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, frame);
        }

        List<Material> decoys = new ArrayList<>(pool);
        for (int i = 0; i < SLOTS.length; i++) {
            int slot = SLOTS[i];
            if (i == answerIndex) {
                inv.setItem(slot, named(answer, "<#B1C7F0>Click to confirm you are here."));
            } else {
                Material decoy = decoys.isEmpty()
                        ? Material.STONE
                        : decoys.remove(random.nextInt(decoys.size()));
                inv.setItem(slot, named(decoy, "<gray>"));
            }
        }

        s.anchor = player.getLocation();
        active.put(u, s);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        player.openInventory(inv);

        s.taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active.containsKey(u)) {
                timeout(player);
            }
        }, TIMEOUT_TICKS).getTaskId();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) {
            return;
        }
        Session s = active.get(p.getUniqueId());
        if (s == null || event.getView().getTopInventory() != s.inventory) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != s.inventory) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == s.answerSlot) {
            pass(p, s);
            return;
        }
        if (isPlayable(slot)) {
            ItemStack clicked = event.getCurrentItem();
            String name = clicked == null ? "unknown" : clicked.getType().name();
            fail(p, s, name);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) {
            return;
        }
        Session s = active.get(p.getUniqueId());
        if (s == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active.containsKey(p.getUniqueId()) && p.isOnline()) {
                p.openInventory(s.inventory);
            }
        }, 1L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Session s = active.get(event.getPlayer().getUniqueId());
        if (s == null || s.anchor == null || event.getTo() == null) {
            return;
        }
        Location to = event.getTo();
        if (to.getX() != s.anchor.getX() || to.getY() != s.anchor.getY() || to.getZ() != s.anchor.getZ()) {
            event.setTo(new Location(s.anchor.getWorld(), s.anchor.getX(), s.anchor.getY(), s.anchor.getZ(),
                    to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Session s = active.get(event.getPlayer().getUniqueId());
        if (s != null) {
            cleanup(event.getPlayer(), s);
        }
    }

    private void pass(Player player, Session s) {
        long elapsed = System.currentTimeMillis() - s.start;
        cleanup(player, s);
        player.closeInventory();
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        manager.resolved(player, CaptchaKind.STANDARD, CaptchaOutcome.PASS, elapsed,
                s.failed, s.check, s.evidence, s.score);
    }

    private void fail(Player player, Session s, String chosen) {
        s.failed.add(chosen);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        if (s.failed.size() >= MAX_FAILS) {
            long elapsed = System.currentTimeMillis() - s.start;
            cleanup(player, s);
            player.closeInventory();
            manager.resolved(player, CaptchaKind.STANDARD, CaptchaOutcome.FAIL, elapsed,
                    s.failed, s.check, s.evidence, s.score);
            return;
        }
        player.sendMessage(messages.prefixed(
                "<red>Wrong item. <gray>[<#B1C7F0>" + s.failed.size() + "<white>/<red>" + MAX_FAILS + "<gray>]"));
    }

    private void timeout(Player player) {
        Session s = active.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - s.start;
        cleanup(player, s);
        if (player.isOnline()) {
            player.closeInventory();
        }
        manager.resolved(player, CaptchaKind.STANDARD, CaptchaOutcome.TIMEOUT, elapsed,
                s.failed, s.check, s.evidence, s.score);
    }

    private void cleanup(Player player, Session s) {
        active.remove(player.getUniqueId());
        if (s.taskId != -1) {
            Bukkit.getScheduler().cancelTask(s.taskId);
        }
    }

    private static String titleCase(String materialName) {
        String[] words = materialName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static boolean isPlayable(int slot) {
        for (int s : SLOTS) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    private ItemStack named(Material material, String miniMessage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.parse(miniMessage)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
