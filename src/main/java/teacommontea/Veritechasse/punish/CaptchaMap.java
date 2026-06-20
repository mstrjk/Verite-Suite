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

package teacommontea.veritechasse.punish;

import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.api.CaptchaKind;
import teacommontea.veritechasse.api.CaptchaOutcome;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CaptchaMap implements Listener {

    private static final String CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;
    private static final int MAX_TRIES = 3;
    private static final int LIMIT_SECONDS = 60;

    private final AntiCheat antiCheat;
    private final CaptchaManager manager;
    private final Random random = new Random();
    private final Map<UUID, Session> active = new ConcurrentHashMap<>();

    public CaptchaMap(AntiCheat antiCheat, CaptchaManager manager) {
        this.antiCheat = antiCheat;
        this.manager = manager;
    }

    public boolean isActive(UUID u) {
        return active.containsKey(u);
    }

    private static final class Session {
        String code;
        MapView view;
        final long start = System.currentTimeMillis();
        final List<String> wrong = new ArrayList<>();
        int tries;
        int secondsLeft = LIMIT_SECONDS;
        ItemStack[] inventory;
        ItemStack[] armor;
        Location anchor;
        int heldSlot;
        int taskId = -1;
        String check;
        String evidence;
        double score;

        BufferedImage image() {
            BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 128, 128);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Monospaced", Font.BOLD, 32));
            int width = g.getFontMetrics().stringWidth(code);
            g.drawString(code, (128 - width) / 2, 64);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString("Tries: " + (MAX_TRIES - tries), 8, 100);
            g.drawString("Time: " + secondsLeft + "s", 8, 116);
            g.setColor(new Color(140, 140, 140));
            for (int i = 0; i < 25; i++) {
                g.fillRect((int) (Math.abs(code.hashCode() + i * 31) % 128),
                        (int) (Math.abs(code.hashCode() * 7L + i * 17) % 128), 1, 1);
            }
            g.dispose();
            return img;
        }
    }

    private static final class CodeRenderer extends MapRenderer {
        private final Session session;
        private boolean drawn;

        CodeRenderer(Session session) {
            super(false);
            this.session = session;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            canvas.drawImage(0, 0, session.image());
            drawn = true;
        }
    }

    public void open(Player player, String check, String evidence, double score) {
        UUID u = player.getUniqueId();
        if (active.containsKey(u)) {
            return;
        }

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }

        Session s = new Session();
        s.code = code.toString();
        s.check = check;
        s.evidence = evidence;
        s.score = score;

        MapView view = Bukkit.createMap(player.getWorld());
        new ArrayList<>(view.getRenderers()).forEach(view::removeRenderer);
        view.addRenderer(new CodeRenderer(s));
        s.view = view;

        s.inventory = player.getInventory().getContents();
        s.armor = player.getInventory().getArmorContents();
        s.heldSlot = player.getInventory().getHeldItemSlot();
        s.anchor = player.getLocation();
        active.put(u, s);

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItem(0, mapItem(view));
        player.getInventory().setHeldItemSlot(0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, LIMIT_SECONDS * 20, 255, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, LIMIT_SECONDS * 20, 1, false, false));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        player.sendMessage(antiCheat.messages().prefixed(
                "<#B1C7F0>Type the code shown on the map. <white>Type <white>exit <white>to give up."));
        player.sendMap(view);

        startTick(player, s);
    }

    private void startTick(Player player, Session s) {
        UUID u = player.getUniqueId();
        s.taskId = Bukkit.getScheduler().runTaskTimer(antiCheat.getPlugin(), () -> {
            if (!active.containsKey(u) || !player.isOnline()) {
                Bukkit.getScheduler().cancelTask(s.taskId);
                return;
            }
            long elapsed = System.currentTimeMillis() - s.start;
            s.secondsLeft = (int) (LIMIT_SECONDS - elapsed / 1000);
            if (s.secondsLeft <= 0) {
                timeout(player);
                return;
            }
            player.sendMap(s.view);
        }, 20L, 20L).getTaskId();
    }

    private ItemStack mapItem(MapView view) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta != null) {
            meta.setMapView(view);
            meta.displayName(antiCheat.messages().parse("<#B1C7F0>Captcha")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        Player p = event.getPlayer();
        Session s = active.get(p.getUniqueId());
        if (s == null) {
            return;
        }
        event.setCancelled(true);
        String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(antiCheat.getPlugin(), () -> {
            if (!active.containsKey(p.getUniqueId())) {
                return;
            }
            if (message.equalsIgnoreCase("exit")) {
                fail(p, s, "exit");
            } else if (message.equals(s.code)) {
                pass(p, s);
            } else {
                fail(p, s, message);
            }
        });
    }

    private void pass(Player player, Session s) {
        long elapsed = System.currentTimeMillis() - s.start;
        cleanup(player, s);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        manager.resolved(player, CaptchaKind.DETAILED, CaptchaOutcome.PASS, elapsed,
                s.wrong, s.check, s.evidence, s.score);
    }

    private void fail(Player player, Session s, String typed) {
        s.tries++;
        s.wrong.add(typed);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        if (s.tries >= MAX_TRIES) {
            long elapsed = System.currentTimeMillis() - s.start;
            cleanup(player, s);
            manager.resolved(player, CaptchaKind.DETAILED, CaptchaOutcome.FAIL, elapsed,
                    s.wrong, s.check, s.evidence, s.score);
            return;
        }
        player.sendMap(s.view);
        player.sendMessage(antiCheat.messages().prefixed(
                "<red>Incorrect. <white>Tries left: <white>" + (MAX_TRIES - s.tries)));
    }

    private void timeout(Player player) {
        Session s = active.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - s.start;
        cleanup(player, s);
        manager.resolved(player, CaptchaKind.DETAILED, CaptchaOutcome.TIMEOUT, elapsed,
                s.wrong, s.check, s.evidence, s.score);
    }

    private void cleanup(Player player, Session s) {
        active.remove(player.getUniqueId());
        if (s.taskId != -1) {
            Bukkit.getScheduler().cancelTask(s.taskId);
        }
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            if (s.inventory != null) {
                player.getInventory().clear();
                player.getInventory().setContents(s.inventory);
            }
            if (s.armor != null) {
                player.getInventory().setArmorContents(s.armor);
            }
            player.getInventory().setHeldItemSlot(s.heldSlot);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (active.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        if (active.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (active.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player p && active.containsKey(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p && active.containsKey(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p && active.containsKey(p.getUniqueId())) {
            event.setCancelled(true);
        }
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
    public void onInteract(PlayerInteractEvent event) {
        if (!active.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        Action a = event.getAction();
        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK
                || a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Session s = active.get(event.getPlayer().getUniqueId());
        if (s != null) {
            cleanup(event.getPlayer(), s);
        }
    }
}
