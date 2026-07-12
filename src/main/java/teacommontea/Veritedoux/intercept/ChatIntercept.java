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

package teacommontea.veritedoux.intercept;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import teacommontea.veritedoux.Sieve;

public final class ChatIntercept implements Listener {

    private static final String HANDLER_NAME = "verite_chat_filter";

    private final Plugin plugin;
    private final NmsAccess nms;

    private ChatIntercept(Plugin plugin, NmsAccess nms) {
        this.plugin = plugin;
        this.nms = nms;
    }

    public static ChatIntercept install(Plugin plugin) {
        NmsAccess nms;
        try {
            nms = NmsAccess.resolve();
        } catch (NmsAccess.Unsupported u) {
            plugin.getLogger().warning("[Verite] outbound chat interceptor unavailable ("
                    + u.getMessage() + "); falling back to chat-event filtering only.");
            return null;
        }
        ChatIntercept ci = new ChatIntercept(plugin, nms);
        Bukkit.getPluginManager().registerEvents(ci, plugin);
        for (Player p : Bukkit.getOnlinePlayers()) {
            ci.inject(p);
        }
        plugin.getLogger().info("[Verite] outbound chat interceptor active (dataVersion "
                + nms.dataVersion() + ", packet " + nms.chatPacketClass().getSimpleName() + ").");
        return ci;
    }

    public void shutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            eject(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        inject(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        eject(e.getPlayer());
    }

    private void inject(Player p) {
        try {
            Channel ch = nms.channelOf(p);
            if (ch == null || ch.pipeline().get(HANDLER_NAME) != null) {
                return;
            }

            var pipeline = ch.pipeline();
            UUID id = p.getUniqueId();
            OutboundFilter handler = new OutboundFilter(id);
            if (pipeline.get("packet_handler") != null) {
                pipeline.addBefore("packet_handler", HANDLER_NAME, handler);
            } else {
                pipeline.addLast(HANDLER_NAME, handler);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Verite] could not inject chat filter for "
                    + p.getName() + ": " + t);
        }
    }

    private void eject(Player p) {
        try {
            Channel ch = nms.channelOf(p);
            if (ch != null && ch.pipeline().get(HANDLER_NAME) != null) {
                ch.pipeline().remove(HANDLER_NAME);
            }
        } catch (Throwable ignored) {

        }
    }

    private final class OutboundFilter extends ChannelOutboundHandlerAdapter {
        private final UUID player;

        OutboundFilter(UUID player) { this.player = player; }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (nms.isChatPacket(msg)) {
                String text = nms.plainTextOf(msg);
                if (text != null && !text.isEmpty() && Sieve.check(player, text) != Sieve.Result.CLEAN) {

                    promise.setSuccess();
                    return;
                }
            }
            super.write(ctx, msg, promise);
        }
    }
}
