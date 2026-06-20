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

import teacommontea.veritesauver.store.Scope;
import teacommontea.veritesauver.util.SauverFormat;
import teacommontea.veritesauver.util.SauverMessages;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SauverChat {

    private static final String BYPASS = "veritesauver.chat.bypass";

    private final Sauver sauver;

    public SauverChat(Sauver sauver) {
        this.sauver = sauver;
    }

    private SauverMessages msg() {
        return sauver.messages();
    }

    private Scope chat() {
        return sauver.store().scope("chatmod");
    }

    public void broadcast(CommandSender sender, String[] args) {
        if (args.length == 0) {
            msg().send(sender, "<#FFFFFF>Usage: <#FFFFFF>/bc <message> <#FFFFFF>- broadcast a message to the server");
            return;
        }
        String message = String.join(" ", args);
        Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text(" "));
        Bukkit.broadcast(SauverMessages.screen(sauver.messages().messages().prefix()
                + "<#FFFFFF>: <#FFFFFF>" + message));
        Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text(" "));
    }

    public void chatClear(CommandSender sender) {
        net.kyori.adventure.text.Component blank = net.kyori.adventure.text.Component.text(" ");
        for (int i = 0; i < 300; i++) {
            Bukkit.getServer().sendMessage(blank);
        }
        String who = sender instanceof Player p ? p.getName() : SauverEngine.CONSOLE_NAME;
        Bukkit.broadcast(SauverMessages.screen("<#555555>[<#FF5555>!<#555555>] <#FFFFFF>Chat was cleared by "
                + who + " <#555555>[<#FF5555>!<#555555>]"));
    }

    public void chatMute(CommandSender sender) {
        boolean muted = isChatMuted();
        if (!muted) {
            chat().set("muted", true);
            Bukkit.broadcast(SauverMessages.screen("<#555555>[<#FF5555>!<#555555>]<#FF5555> Chat has been muted! <#555555>[<#FF5555>!<#555555>]"));
        } else {
            chat().set("muted", false);
            Bukkit.broadcast(SauverMessages.screen("<#555555>[<#FF5555>!<#555555>]<#B1C7F0> Chat has been unmuted! <#555555>[<#FF5555>!<#555555>]"));
        }
    }

    private boolean isChatMuted() {
        return chat().getBoolean("muted", false);
    }

    public boolean mutedGate(Player p) {
        if (p.hasPermission(BYPASS)) {
            return false;
        }
        if (isChatMuted()) {
            msg().err(p, "<#FF5555>You cannot speak while the chat is muted.");
            return true;
        }
        return false;
    }

    public void slowmode(CommandSender sender, String[] args) {
        String arg1 = args.length > 0 ? args[0] : null;
        String targetName = args.length > 1 ? args[1] : null;
        if (arg1 == null) {
            if (chat().has("global")) {
                chat().delete("global");
                Bukkit.broadcast(SauverMessages.screen(sauver.messages().messages().prefix() + " <#B1C7F0>Slowmode has been disabled."));
            } else {
                msg().send(sender, "<#FFFFFF>Usage: <#FFFFFF>/slowmode <duration> [player] <#FFFFFF>- set chat slowmode");
            }
            return;
        }
        if (arg1.equalsIgnoreCase("off")) {
            if (targetName != null) {
                OfflinePlayer t = Bukkit.getOfflinePlayer(targetName);
                chat().delete("player." + t.getUniqueId());
                msg().send(sender, "<#B1C7F0>You removed slowmode from <#FFFFFF>" + targetName + "<#B1C7F0>.");
                if (t.isOnline() && t.getPlayer() != null) {
                    msg().send(t.getPlayer(), "<#FFFFFF>Your personal slowmode has been removed.");
                }
            } else {
                chat().delete("global");
                Bukkit.broadcast(SauverMessages.screen(sauver.messages().messages().prefix() + " <#B1C7F0>Slowmode has been disabled."));
            }
            return;
        }
        Long millis = parseDuration(arg1);
        if (millis == null) {
            msg().err(sender, "<#FF5555>That duration is invalid. <#FFFFFF>Use <#FFFFFF>s<#FFFFFF>, <#FFFFFF>m<#FFFFFF>, "
                    + "<#FFFFFF>h<#FFFFFF>, or <#FFFFFF>d<#FFFFFF>. Example: <#FFFFFF>/slowmode 10s<#FFFFFF>.");
            return;
        }
        if (millis <= 0) {
            msg().err(sender, "<#FF5555>The duration must be greater than <#FFFFFF>0<#FF5555>.");
            return;
        }
        if (targetName != null) {
            OfflinePlayer t = Bukkit.getOfflinePlayer(targetName);
            chat().set("player." + t.getUniqueId(), millis);
            msg().send(sender, "<#FFFFFF>You set a slowmode of <#FFFFFF>" + arg1 + " <#FFFFFF>on <#FFFFFF>" + targetName + "<#FFFFFF>.");
            if (t.isOnline() && t.getPlayer() != null) {
                msg().send(t.getPlayer(), "<#FFFFFF>You have been given a personal slowmode of <#FFFFFF>" + arg1 + "<#FFFFFF>.");
            }
        } else {
            chat().set("global", millis);
            Bukkit.broadcast(SauverMessages.screen(sauver.messages().messages().prefix()
                    + " <#FFFFFF>Slowmode has been set to <#FFFFFF>" + arg1 + "<#FFFFFF>."));
        }
    }

    public boolean slowmodeGate(Player p) {
        if (p.hasPermission(BYPASS)) {
            return false;
        }
        UUID u = p.getUniqueId();
        long sm = chat().getLong("player." + u, 0);
        if (sm <= 0) {
            sm = chat().getLong("global", 0);
        }
        if (sm > 0) {
            long last = chat().getLong("last." + u, 0);
            if (last > 0) {
                long diff = System.currentTimeMillis() - last;
                if (diff < sm) {
                    long remain = sm - diff;
                    msg().err(p, "<#FF5555>Your slowmode is active. <#FFFFFF>Time remaining: <#FFFFFF>"
                            + SauverFormat.fancyTime(remain));
                    return true;
                }
            }
        }
        chat().set("last." + u, System.currentTimeMillis());
        return false;
    }

    public List<String> slowmodeTab(int pos) {
        if (pos == 1) {
            return List.of("off", "10s", "30s", "60s", "5m");
        }
        if (pos == 2) {
            return onlineNames();
        }
        return List.of();
    }

    private static Long parseDuration(String input) {
        if (input.length() < 2) {
            return null;
        }
        char unit = input.charAt(input.length() - 1);
        String numPart = input.substring(0, input.length() - 1);
        double num;
        try {
            num = Double.parseDouble(numPart);
        } catch (NumberFormatException e) {
            return null;
        }
        long perUnit = switch (unit) {
            case 's' -> 1000L;
            case 'm' -> 60_000L;
            case 'h' -> 3_600_000L;
            case 'd' -> 86_400_000L;
            default -> -1L;
        };
        if (perUnit < 0) {
            return null;
        }
        return (long) (num * perUnit);
    }

    static List<String> onlineNames() {
        List<String> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            out.add(p.getName());
        }
        return out;
    }
}
