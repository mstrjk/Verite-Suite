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

package teacommontea.veritesauver.command;

import teacommontea.veritesauver.Entry;
import teacommontea.veritesauver.Sauver;
import teacommontea.veritesauver.SauverDuration;
import teacommontea.veritesauver.SauverEngine;
import teacommontea.veritesauver.SauverExempt;
import teacommontea.veritesauver.SauverDAO;
import teacommontea.veritesauver.SauverGeoIp;
import teacommontea.veritesauver.SauverLimits;
import teacommontea.veritesauver.SauverLockdown;
import teacommontea.veritesauver.SauverMojang;
import teacommontea.veritesauver.SauverTemplates;
import teacommontea.veritesauver.util.SauverFormat;
import teacommontea.veritesauver.util.SauverProtocol;
import teacommontea.veritesauver.util.SauverMessages;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SauverCommands implements CommandExecutor, TabCompleter {

    private static final String DEFAULT_BAN_REASON  = "The Ban Hammer has spoken!";
    private static final String DEFAULT_MUTE_REASON = "Spamming";
    private static final String DEFAULT_REMOVE_REASON = "No reason specified.";
    private static final int PAGE_SIZE = 8;

    private final Sauver sauver;

    public SauverCommands(Sauver sauver) {
        this.sauver = sauver;
    }

    private SauverMessages msg() {
        return sauver.messages();
    }

    private teacommontea.veritesauver.SauverDAO dao() {
        return sauver.dao();
    }

    private void send(CommandSender to, String body) {
        msg().send(to, body);
    }

    private void err(CommandSender to, String body) {
        msg().err(to, body);
    }

    private void raw(CommandSender to, String body) {
        msg().raw(to, body);
    }

    private void usage(CommandSender to, String syntax, String desc) {
        send(to, "<#FFFFFF>Usage: <#FFFFFF>/" + syntax + " <#FFFFFF>- " + desc);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "ban"          -> issueBanOrMute(sender, args, Entry.Type.BAN, false);
            case "tempban"      -> issueBanOrMute(sender, args, Entry.Type.BAN, true);
            case "mute"         -> issueBanOrMute(sender, args, Entry.Type.MUTE, false);
            case "tempmute"     -> issueBanOrMute(sender, args, Entry.Type.MUTE, true);
            case "ipban"        -> issueIp(sender, args, Entry.Type.BAN);
            case "ipmute"       -> issueIp(sender, args, Entry.Type.MUTE);
            case "kick"         -> kick(sender, args);
            case "warn"         -> warn(sender, args);
            case "unwarn"       -> unwarn(sender, args);
            case "warnings"     -> warnings(sender, args);
            case "checkwarn"    -> checkwarn(sender, args);
            case "warnlist"     -> warnlist(sender, args);
            case "unban"        -> pardon(sender, args, Entry.Type.BAN);
            case "unmute"       -> pardon(sender, args, Entry.Type.MUTE);
            case "checkban"     -> check(sender, args, Entry.Type.BAN);
            case "checkmute"    -> check(sender, args, Entry.Type.MUTE);
            case "dupeip"       -> dupeip(sender, args);
            case "iphistory"    -> iphistory(sender, args);
            case "namehistory"  -> namehistory(sender, args);
            case "lastuuid"     -> lastuuid(sender, args);
            case "banlist"      -> listActive(sender, args, Entry.Type.BAN);
            case "mutelist"     -> listActive(sender, args, Entry.Type.MUTE);
            case "history"      -> history(sender, args);
            case "staffhistory" -> staffhistory(sender, args);
            case "staffrollback"-> staffrollback(sender, args);
            case "prunehistory" -> prunehistory(sender, args);
            case "lockdown"     -> lockdown(sender, args);
            case "geoip"        -> geoip(sender, args);
            case "whois"        -> whois(sender, args);
            case "seen"         -> seen(sender, args);
            case "bc", "broadcast" -> sauver.chat().broadcast(sender, args);
            case "chatclear", "cc" -> sauver.chat().chatClear(sender);
            case "chatmute"     -> sauver.chat().chatMute(sender);
            case "slowmode"     -> sauver.chat().slowmode(sender, args);
            case "punish"       -> punishAdmin(sender, args);
            default -> { return false; }
        }
        return true;
    }

    private record Parsed(String targetName, boolean silent, List<String> rest) {}

    private static Parsed parse(String[] args) {
        boolean silent = false;
        List<String> kept = new ArrayList<>();
        for (String a : args) {
            if (a.equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                kept.add(a);
            }
        }
        if (kept.isEmpty()) {
            return new Parsed(null, silent, List.of());
        }
        String target = kept.remove(0);
        return new Parsed(target, silent, kept);
    }

    private static String executorName(CommandSender sender) {
        return sender instanceof Player p ? p.getName() : SauverEngine.CONSOLE_NAME;
    }

    private static UUID executorUuid(CommandSender sender) {
        return sender instanceof Player p ? p.getUniqueId() : null;
    }

    private UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        UUID known = dao().uuidByName(name);
        if (known != null) {
            return known;
        }
        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(name);
        return off == null ? null : off.getUniqueId();
    }

    private String bestName(UUID u, String fallback) {
        String n = dao().nameOf(u);
        if (n != null) {
            return n;
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer(u);
        return off.getName() != null ? off.getName() : fallback;
    }

    private void issueBanOrMute(CommandSender sender, String[] args, Entry.Type type, boolean requireDuration) {
        Parsed parsed = parse(args);
        String word = type == Entry.Type.BAN ? "ban" : "mute";
        if (parsed.targetName() == null) {
            String c = (requireDuration ? "temp" : "") + word;
            usage(sender, c + " <player> " + (requireDuration ? "<duration> " : "") + "[-s] [reason]",
                    (requireDuration ? "temporarily " : "") + word + " a player");
            return;
        }
        UUID target = resolve(parsed.targetName());
        if (target == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + parsed.targetName() + "<#FF5555>.");
            return;
        }

        List<String> rest = new ArrayList<>(parsed.rest());

        if (!requireDuration && !rest.isEmpty()) {
            SauverTemplates.Template tpl = SauverTemplates.get(type, rest.get(0));
            if (tpl != null) {
                String exemptT = SauverExempt.blockReason(sender, target, type);
                if (exemptT != null) {
                    err(sender, "<#FF5555>" + exemptT);
                    return;
                }
                if (tpl.permission() != null && !sender.hasPermission(tpl.permission())) {
                    err(sender, "<#FF5555>You may not use the <#FFFFFF>" + tpl.name() + " <#FF5555>template.");
                    return;
                }
                SauverEngine.Result tr = SauverEngine.issueTemplate(type, target,
                        bestName(target, parsed.targetName()), tpl, executorUuid(sender), executorName(sender), parsed.silent());
                if (!tr.ok()) {
                    err(sender, "<#FF5555>" + tr.error());
                    return;
                }
                String tdur = tr.entry().permanent() ? "permanently" : "for <#FFFFFF>" + SauverFormat.fancyTime(tr.entry().duration());
                send(sender, "<#B1C7F0>You " + word + "ned <#FFFFFF>" + bestName(target, parsed.targetName())
                        + " <#B1C7F0>" + tdur + " <#FFFFFF>via template <#FFFFFF>" + tpl.name() + " <#FFFFFF>(#" + tr.entry().randomId() + ")");
                return;
            }
        }

        long duration = Entry.PERMANENT;
        if (!rest.isEmpty()) {
            long maybe = SauverDuration.parse(rest.get(0));
            if (maybe != -1) {
                duration = maybe;
                rest.remove(0);
            } else if (requireDuration) {
                err(sender, "<#FF5555>That duration is invalid: <#FFFFFF>" + rest.get(0)
                        + "<#FF5555>. Use forms like <#FFFFFF>7d<#FF5555>, <#FFFFFF>12h<#FF5555>, <#FFFFFF>2w<#FF5555>, <#FFFFFF>1mo<#FF5555>.");
                return;
            }
        } else if (requireDuration) {
            usage(sender, "temp" + word + " <player> <duration> [-s] [reason]", "temporarily " + word + " a player");
            return;
        }

        String reason = rest.isEmpty()
                ? (type == Entry.Type.BAN ? DEFAULT_BAN_REASON : DEFAULT_MUTE_REASON)
                : String.join(" ", rest);

        if (alreadyPunished(sender, type, target, parsed.targetName())) {
            return;
        }
        String exempt = SauverExempt.blockReason(sender, target, type);
        if (exempt != null) {
            err(sender, "<#FF5555>" + exempt);
            return;
        }
        long now = System.currentTimeMillis();
        long cd = SauverLimits.cooldownRemaining(sender, type, now);
        if (cd > 0) {
            err(sender, "<#FF5555>Slow down. Wait <#FFFFFF>" + SauverFormat.fancyTime(cd)
                    + " <#FF5555>before your next " + type.id() + ".");
            return;
        }
        SauverLimits.Check cap = SauverLimits.capDuration(sender, type, duration);
        if (!cap.ok()) {
            err(sender, "<#FF5555>" + cap.error());
            return;
        }
        duration = cap.durationMillis();

        SauverEngine.Result r = SauverEngine.issue(
                type, target, null, bestName(target, parsed.targetName()), reason,
                executorUuid(sender), executorName(sender), duration, parsed.silent(), false);
        if (!r.ok()) {
            err(sender, "<#FF5555>" + r.error());
            return;
        }
        SauverLimits.markUsed(sender, type, now);
        String dur = r.entry().permanent() ? "permanently" : "for <#FFFFFF>" + SauverFormat.fancyTime(r.entry().duration());
        send(sender, "<#B1C7F0>You " + word + "ned <#FFFFFF>" + bestName(target, parsed.targetName())
                + " <#B1C7F0>" + dur + "<#B1C7F0>. <#FFFFFF>Reason: <#FFFFFF>" + reason + " <#FFFFFF>(#" + r.entry().randomId() + ")");
    }

    private boolean alreadyPunished(CommandSender sender, Entry.Type type, UUID target, String name) {
        Entry existing = type == Entry.Type.BAN ? sauver.activeBan(target) : sauver.activeMute(target);
        if (existing != null) {
            String word = type == Entry.Type.BAN ? "banned" : "muted";
            err(sender, "<#FFFFFF>" + name + " <#FF5555>is already " + word
                    + " <#FFFFFF>(#" + existing.randomId() + ")<#FF5555>. Lift it first with <#FFFFFF>/un"
                    + (type == Entry.Type.BAN ? "ban " : "mute ") + name + "<#FF5555>.");
            return true;
        }
        return false;
    }

    private void issueIp(CommandSender sender, String[] args, Entry.Type type) {
        String word = type == Entry.Type.BAN ? "ipban" : "ipmute";
        Parsed parsed = parse(args);
        if (parsed.targetName() == null) {
            usage(sender, word + " <player|IP> [-s] [duration] [reason]", word + " a player or IP");
            return;
        }
        UUID targetUuid = null;
        String ip;
        String labelName;
        if (isIpLiteral(parsed.targetName())) {
            ip = parsed.targetName();
            labelName = ip;
        } else {
            targetUuid = resolve(parsed.targetName());
            if (targetUuid == null) {
                err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + parsed.targetName() + "<#FF5555>.");
                return;
            }
            List<String> ips = dao().ipsOf(targetUuid);
            if (ips.isEmpty()) {
                err(sender, "<#FF5555>No IP on record for <#FFFFFF>" + parsed.targetName()
                        + "<#FF5555>. They must have logged in at least once.");
                return;
            }
            ip = ips.get(0);
            labelName = bestName(targetUuid, parsed.targetName());
        }

        List<String> rest = new ArrayList<>(parsed.rest());
        long duration = Entry.PERMANENT;
        if (!rest.isEmpty()) {
            long maybe = SauverDuration.parse(rest.get(0));
            if (maybe != -1) {
                duration = maybe;
                rest.remove(0);
            }
        }
        String reason = rest.isEmpty()
                ? (type == Entry.Type.BAN ? DEFAULT_BAN_REASON : DEFAULT_MUTE_REASON)
                : String.join(" ", rest);

        SauverEngine.Result r = SauverEngine.issueIp(type, targetUuid, ip, labelName, reason,
                executorUuid(sender), executorName(sender), duration, parsed.silent());
        if (!r.ok()) {
            err(sender, "<#FF5555>" + r.error());
            return;
        }
        String dur = r.entry().permanent() ? "permanently" : "for <#FFFFFF>" + SauverFormat.fancyTime(r.entry().duration());
        send(sender, "<#B1C7F0>You " + word + "ned <#FFFFFF>" + labelName
                + " <#B1C7F0>" + dur + "<#B1C7F0>. <#FFFFFF>Reason: <#FFFFFF>" + reason + " <#FFFFFF>(#" + r.entry().randomId() + ")");
    }

    private static boolean isIpLiteral(String s) {
        return s.matches("\\d{1,3}(\\.\\d{1,3}){3}") || s.contains(":");
    }

    private void kick(CommandSender sender, String[] args) {
        Parsed parsed = parse(args);
        if (parsed.targetName() == null) {
            usage(sender, "kick <player> [-s] [reason]", "kick a player");
            return;
        }
        Player target = Bukkit.getPlayerExact(parsed.targetName());
        if (target == null) {
            err(sender, "<#FF5555>That player is not online.");
            return;
        }
        String exempt = SauverExempt.blockReason(sender, target.getUniqueId(), Entry.Type.KICK);
        if (exempt != null) {
            err(sender, "<#FF5555>" + exempt);
            return;
        }
        String reason = parsed.rest().isEmpty() ? "Kicked by an operator." : String.join(" ", parsed.rest());
        SauverEngine.Result r = SauverEngine.kick(target.getUniqueId(), target.getName(), reason,
                executorUuid(sender), executorName(sender), parsed.silent());
        if (!r.ok()) {
            err(sender, "<#FF5555>" + r.error());
            return;
        }
        send(sender, "<#B1C7F0>You kicked <#FFFFFF>" + target.getName()
                + "<#B1C7F0>. <#FFFFFF>Reason: <#FFFFFF>" + reason);
    }

    private void warn(CommandSender sender, String[] args) {
        Parsed parsed = parse(args);
        if (parsed.targetName() == null) {
            usage(sender, "warn <player> [-s] [reason]", "warn a player");
            return;
        }
        UUID target = resolve(parsed.targetName());
        if (target == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + parsed.targetName() + "<#FF5555>.");
            return;
        }
        String exempt = SauverExempt.blockReason(sender, target, Entry.Type.WARNING);
        if (exempt != null) {
            err(sender, "<#FF5555>" + exempt);
            return;
        }
        String reason = parsed.rest().isEmpty() ? "No reason specified." : String.join(" ", parsed.rest());
        SauverEngine.Result r = SauverEngine.warn(target, bestName(target, parsed.targetName()), reason,
                executorUuid(sender), executorName(sender), parsed.silent());
        if (!r.ok()) {
            err(sender, "<#FF5555>" + r.error());
            return;
        }
        int active = dao().activeWarnings(target, System.currentTimeMillis()).size();
        send(sender, "<#B1C7F0>You warned <#FFFFFF>" + bestName(target, parsed.targetName())
                + "<#B1C7F0>. <#FFFFFF>Active warnings: <#FFFFFF>" + active + " <#FFFFFF>(#" + r.entry().randomId() + ")");
    }

    private void unwarn(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "unwarn <player> [reason]", "remove a player's most recent warning");
            return;
        }
        UUID target = resolve(args[0]);
        if (target == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
            return;
        }
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : DEFAULT_REMOVE_REASON;
        SauverEngine.Result r = SauverEngine.unwarn(target, bestName(target, args[0]),
                executorUuid(sender), executorName(sender), reason);
        if (!r.ok()) {
            err(sender, "<#FF5555>" + r.error());
            return;
        }
        int active = dao().activeWarnings(target, System.currentTimeMillis()).size();
        send(sender, "<#B1C7F0>You removed a warning from <#FFFFFF>" + bestName(target, args[0])
                + "<#B1C7F0>. <#FFFFFF>Active warnings: <#FFFFFF>" + active);
    }

    private void warnings(CommandSender sender, String[] args) {
        UUID target;
        String name;
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                err(sender, "<#FF5555>Console must specify a player.");
                return;
            }
            target = p.getUniqueId();
            name = p.getName();
        } else {
            if (!sender.hasPermission("veritesauver.warnings")) {
                err(sender, "<#FF5555>You may only view your own warnings.");
                return;
            }
            target = resolve(args[0]);
            if (target == null) {
                err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
                return;
            }
            name = bestName(target, args[0]);
        }
        List<Entry> active = dao().activeWarnings(target, System.currentTimeMillis());
        if (active.isEmpty()) {
            send(sender, "<#FFFFFF>" + name + " <#B1C7F0>has no active warnings.");
            return;
        }
        send(sender, "<#FFFFFF>" + name + " <#FFFFFF>has <#FFFFFF>" + active.size()
                + " <#FFFFFF>active " + SauverFormat.pluralize(active.size(), "warning") + ".");
        long now = System.currentTimeMillis();
        for (Entry e : active) {
            raw(sender, "<#FFFFFF>  #" + e.randomId() + " <#FFFFFF>" + e.reason()
                    + " <#FFFFFF>by <#FFFFFF>" + e.executorName() + " <#FFFFFF>(expires in <#FFFFFF>" + SauverFormat.fancyTime(e.remaining(now)) + "<#FFFFFF>)");
        }
    }

    private void checkwarn(CommandSender sender, String[] args) {
        UUID u = lookupTarget(sender, args, "checkwarn");
        if (u == null) {
            return;
        }
        int n = dao().activeWarnings(u, System.currentTimeMillis()).size();
        String name = bestName(u, args[0]);
        if (n == 0) {
            send(sender, "<#FFFFFF>" + name + " <#B1C7F0>has no active warnings.");
        } else {
            send(sender, "<#FFFFFF>" + name + " <#FF5555>has <#FFFFFF>" + n + " <#FF5555>active "
                    + SauverFormat.pluralize(n, "warning") + ". <#FFFFFF>See <#FFFFFF>/warnings " + name);
        }
    }

    private void warnlist(CommandSender sender, String[] args) {
        long now = System.currentTimeMillis();
        List<Entry> all = dao().allActiveWarnings(now);
        if (all.isEmpty()) {
            send(sender, "<#FFFFFF>There are no active warnings.");
            return;
        }
        int page = args.length > 0 ? Math.max(1, parseIntOr(args[0], 1)) : 1;
        int pages = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.min(page, pages);
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, all.size());
        send(sender, "<#FFFFFF>Active warnings (<#FFFFFF>" + all.size()
                + "<#FFFFFF>) - page <#FFFFFF>" + page + "<#FFFFFF>/<#FFFFFF>" + pages + "<#FFFFFF>.");
        for (int i = from; i < to; i++) {
            Entry e = all.get(i);
            String tname = e.uuid() != null ? bestName(e.uuid(), "?") : "?";
            raw(sender, "<#FFFFFF>  #" + e.randomId() + " <#FFFFFF>" + tname + " <#FFFFFF>by <#FFFFFF>"
                    + e.executorName() + " <#FFFFFF>(expires <#FFFFFF>" + SauverFormat.fancyTime(e.remaining(now)) + "<#FFFFFF>) " + e.reason());
        }
    }

    private void pardon(CommandSender sender, String[] args, Entry.Type type) {
        String word = type == Entry.Type.BAN ? "ban" : "mute";
        if (args.length == 0) {
            usage(sender, "un" + word + " <player> [reason]", "un" + word + " a player");
            return;
        }
        UUID target = resolve(args[0]);
        if (target == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
            return;
        }

        Entry active = type == Entry.Type.BAN ? dao().activeBan(target) : dao().activeMute(target);
        if (active != null && !canRemove(sender, type, active)) {
            err(sender, "<#FF5555>You can only remove " + word + "s you issued.");
            return;
        }
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : DEFAULT_REMOVE_REASON;
        SauverEngine.Result r = SauverEngine.pardon(type, target, bestName(target, args[0]),
                executorUuid(sender), executorName(sender), reason);
        if (!r.ok()) {
            err(sender, "<#FF5555>" + r.error());
            return;
        }
        send(sender, "<#B1C7F0>You un" + word + "ned <#FFFFFF>" + bestName(target, args[0]) + "<#B1C7F0>.");
    }

    private boolean canRemove(CommandSender sender, Entry.Type type, Entry e) {
        String base = type == Entry.Type.BAN ? "veritesauver.unban" : "veritesauver.unmute";
        if (sender.hasPermission(base)) {
            return true;
        }
        if (!sender.hasPermission(base + ".own")) {
            return false;
        }
        UUID who = executorUuid(sender);
        return who != null && who.equals(e.executorUuid());
    }

    private void check(CommandSender sender, String[] args, Entry.Type type) {
        String word = type == Entry.Type.BAN ? "ban" : "mute";
        if (args.length == 0) {
            usage(sender, "check" + word + " <player>", "check a player's " + word + " status");
            return;
        }
        UUID target = resolve(args[0]);
        if (target == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
            return;
        }
        Entry e = type == Entry.Type.BAN ? sauver.activeBan(target) : sauver.activeMute(target);
        String name = bestName(target, args[0]);
        if (e == null) {
            send(sender, "<#FFFFFF>" + name + " <#B1C7F0>is not " + word + "ned.");
            return;
        }
        long now = System.currentTimeMillis();
        String when = e.permanent() ? "<#FF5555>permanent" : "<#FFFFFF>expires in " + SauverFormat.fancyTime(e.remaining(now));
        send(sender, "<#FFFFFF>" + name + " <#FF5555>is " + word + "ned<#FFFFFF>.");
        raw(sender, "<#FFFFFF>  By: <#FFFFFF>" + e.executorName());
        raw(sender, "<#FFFFFF>  Reason: <#FFFFFF>" + e.reason());
        raw(sender, "<#FFFFFF>  Duration: " + when);
        raw(sender, "<#FFFFFF>  ID: <#FFFFFF>#" + e.randomId());
    }

    private void dupeip(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "dupeip <player|IP>", "list accounts sharing an IP");
            return;
        }
        String ip;
        String labelName;
        if (isIpLiteral(args[0])) {
            ip = args[0];
            labelName = ip;
        } else {
            UUID u = resolve(args[0]);
            if (u == null) {
                err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
                return;
            }
            List<String> ips = dao().ipsOf(u);
            if (ips.isEmpty()) {
                err(sender, "<#FF5555>No IP on record for <#FFFFFF>" + args[0] + "<#FF5555>.");
                return;
            }
            ip = ips.get(0);
            labelName = bestName(u, args[0]);
        }
        List<UUID> users = dao().usersOfIp(ip);
        if (users.isEmpty()) {
            send(sender, "<#FFFFFF>No accounts share that IP.");
            return;
        }
        long now = System.currentTimeMillis();
        send(sender, "<#FFFFFF>Accounts sharing <#FFFFFF>" + labelName + "<#FFFFFF>'s IP (<#FFFFFF>" + users.size() + "<#FFFFFF>).");
        for (UUID u : users) {
            String name = dao().nameOf(u);
            if (name == null) {
                name = u.toString().substring(0, 8);
            }
            Entry b = dao().activeBan(u);
            Entry m = dao().activeMute(u);
            String flag = "";
            if (b != null && b.inForce(now)) {
                flag = " <#FF5555>(banned)";
            } else if (m != null && m.inForce(now)) {
                flag = " <#FFFFFF>(muted)";
            }
            raw(sender, "<#FFFFFF>  <#FFFFFF>" + name + flag);
        }
    }

    private void iphistory(CommandSender sender, String[] args) {
        UUID u = lookupTarget(sender, args, "iphistory");
        if (u == null) {
            return;
        }
        List<String> ips = dao().ipsOf(u);
        if (ips.isEmpty()) {
            send(sender, "<#FFFFFF>No IP history on record.");
            return;
        }
        send(sender, "<#FFFFFF>IP history for <#FFFFFF>" + bestName(u, args[0]) + "<#FFFFFF>.");
        for (String ip : ips) {
            raw(sender, "<#FFFFFF>  <#FFFFFF>" + ip);
        }
    }

    private void namehistory(CommandSender sender, String[] args) {
        UUID u = lookupTarget(sender, args, "namehistory");
        if (u == null) {
            return;
        }
        List<String> names = dao().namesOf(u);
        if (names.isEmpty()) {
            send(sender, "<#FFFFFF>No name history on record.");
            return;
        }
        send(sender, "<#FFFFFF>Name history for <#FFFFFF>" + bestName(u, args[0]) + "<#FFFFFF>.");
        for (String n : names) {
            raw(sender, "<#FFFFFF>  <#FFFFFF>" + n);
        }
    }

    private void lastuuid(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "lastuuid <player>", "show the UUID last seen for a name");
            return;
        }
        UUID u = dao().uuidByName(args[0]);
        if (u == null) {
            err(sender, "<#FF5555>No UUID on record for <#FFFFFF>" + args[0] + "<#FF5555>.");
            return;
        }
        send(sender, "<#FFFFFF>" + args[0] + " <#FFFFFF>-> <#FFFFFF>" + u);
    }

    private UUID lookupTarget(CommandSender sender, String[] args, String cmd) {
        if (args.length == 0) {
            usage(sender, cmd + " <player>", "look up a player");
            return null;
        }
        UUID u = resolve(args[0]);
        if (u == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
        }
        return u;
    }

    private void listActive(CommandSender sender, String[] args, Entry.Type type) {
        String word = type == Entry.Type.BAN ? "ban" : "mute";
        long now = System.currentTimeMillis();
        List<Entry> all = dao().activeOfType(type, now);
        if (all.isEmpty()) {
            send(sender, "<#FFFFFF>There are no active " + word + "s.");
            return;
        }
        int page = args.length > 0 ? Math.max(1, parseIntOr(args[0], 1)) : 1;
        int pages = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.min(page, pages);
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, all.size());
        send(sender, "<#FFFFFF>Active " + word + "s (<#FFFFFF>" + all.size()
                + "<#FFFFFF>) - page <#FFFFFF>" + page + "<#FFFFFF>/<#FFFFFF>" + pages + "<#FFFFFF>.");
        for (int i = from; i < to; i++) {
            Entry e = all.get(i);
            String tname = e.uuid() != null ? bestName(e.uuid(), "?") : e.ip();
            String when = e.permanent() ? "<#FF5555>perm" : "<#FFFFFF>" + SauverFormat.fancyTime(e.remaining(now));
            raw(sender, "<#FFFFFF>  #" + e.randomId() + " <#FFFFFF>" + tname + " <#FFFFFF>by <#FFFFFF>"
                    + e.executorName() + " <#FFFFFF>(" + when + "<#FFFFFF>) " + e.reason());
        }
    }

    private void history(CommandSender sender, String[] args) {
        UUID u = lookupTarget(sender, args, "history");
        if (u == null) {
            return;
        }
        List<Entry> hist = dao().history(u, 40);
        if (hist.isEmpty()) {
            send(sender, "<#FFFFFF>" + bestName(u, args[0]) + " <#FFFFFF>has no punishment history.");
            return;
        }
        send(sender, "<#FFFFFF>Punishment history for <#FFFFFF>" + bestName(u, args[0])
                + " <#FFFFFF>(<#FFFFFF>" + hist.size() + "<#FFFFFF>).");
        long now = System.currentTimeMillis();
        for (Entry e : hist) {
            raw(sender, "<#FFFFFF>  " + historyLine(e, now));
        }
    }

    private void staffhistory(CommandSender sender, String[] args) {
        UUID u = lookupTarget(sender, args, "staffhistory");
        if (u == null) {
            return;
        }
        List<Entry> hist = dao().byStaff(u, 40);
        if (hist.isEmpty()) {
            send(sender, "<#FFFFFF>" + bestName(u, args[0]) + " <#FFFFFF>has issued no punishments.");
            return;
        }
        send(sender, "<#FFFFFF>Punishments issued by <#FFFFFF>" + bestName(u, args[0])
                + " <#FFFFFF>(<#FFFFFF>" + hist.size() + "<#FFFFFF>).");
        long now = System.currentTimeMillis();
        for (Entry e : hist) {
            String tname = e.uuid() != null ? bestName(e.uuid(), "?") : e.ip();
            raw(sender, "<#FFFFFF>  [" + e.type().id() + "] <#FFFFFF>" + tname + " " + statusWord(e, now)
                    + " <#FFFFFF>" + e.reason());
        }
    }

    private String historyLine(Entry e, long now) {
        String typeColor = switch (e.type()) {
            case BAN -> "<#FF5555>";
            case MUTE -> "<#FFFFFF>";
            case WARNING -> "<#FFD166>";
            case KICK -> "<#FFFFFF>";
        };
        return typeColor + e.type().id().toUpperCase(Locale.ROOT) + " " + statusWord(e, now)
                + " <#FFFFFF>by <#FFFFFF>" + e.executorName() + " <#FFFFFF>" + e.reason() + " (#" + e.randomId() + ")";
    }

    private String statusWord(Entry e, long now) {
        if (e.removedByName() != null) {
            return "<#B1C7F0>(removed by " + e.removedByName() + ")";
        }
        if (e.type() == Entry.Type.KICK || e.type() == Entry.Type.WARNING) {
            return e.expired(now) ? "<#FFFFFF>(expired)" : "<#FFFFFF>(active)";
        }
        if (!e.active()) {
            return "<#FFFFFF>(inactive)";
        }
        return e.inForce(now) ? "<#FF5555>(active)" : "<#FFFFFF>(expired)";
    }

    private void staffrollback(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "staffrollback <staff> [duration]", "reverse a staff member's recent punishments");
            return;
        }
        UUID staff = resolve(args[0]);
        if (staff == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
            return;
        }
        long now = System.currentTimeMillis();
        long cutoff = 0;
        if (args.length > 1) {
            long window = SauverDuration.parse(args[1]);
            if (window == -1 || window == Entry.PERMANENT) {
                err(sender, "<#FF5555>That duration is invalid: <#FFFFFF>" + args[1] + "<#FF5555>.");
                return;
            }
            cutoff = now - window;
        }
        List<Entry> toLift = dao().activeByStaffSince(staff, cutoff, now);
        if (toLift.isEmpty()) {
            send(sender, "<#FFFFFF>No active punishments by <#FFFFFF>" + bestName(staff, args[0]) + " <#FFFFFF>to roll back.");
            return;
        }
        int lifted = 0;
        for (Entry e : toLift) {
            SauverEngine.Result r = SauverEngine.pardon(e.type(), e.uuid(),
                    e.uuid() != null ? bestName(e.uuid(), "?") : e.ip(),
                    executorUuid(sender), executorName(sender), "Staff rollback of " + bestName(staff, args[0]));
            if (r.ok()) {
                lifted++;
            }
        }
        send(sender, "<#B1C7F0>You rolled back <#FFFFFF>" + lifted + " <#B1C7F0>punishments by <#FFFFFF>"
                + bestName(staff, args[0]) + "<#B1C7F0>.");
    }

    private void prunehistory(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "prunehistory <player> [duration]", "delete a player's inactive punishment history");
            return;
        }
        UUID u = resolve(args[0]);
        if (u == null) {
            err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
            return;
        }
        long now = System.currentTimeMillis();
        long cutoff = 0;
        if (args.length > 1) {
            long window = SauverDuration.parse(args[1]);
            if (window == -1 || window == Entry.PERMANENT) {
                err(sender, "<#FF5555>That duration is invalid: <#FFFFFF>" + args[1] + "<#FF5555>.");
                return;
            }
            cutoff = now - window;
        }
        int removed = dao().pruneHistory(u, cutoff, now);
        send(sender, "<#B1C7F0>You pruned <#FFFFFF>" + removed + " <#B1C7F0>inactive records for <#FFFFFF>"
                + bestName(u, args[0]) + "<#B1C7F0>.");
    }

    private static int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void lockdown(CommandSender sender, String[] args) {
        if (args.length == 0) {
            String state = SauverLockdown.active()
                    ? "<#FF5555>ACTIVE <#FFFFFF>- " + SauverLockdown.reason()
                    : "<#B1C7F0>off";
            send(sender, "<#FFFFFF>Lockdown is " + state + "<#FFFFFF>.");
            usage(sender, "lockdown <reason>", "seal the server");
            usage(sender, "lockdown end", "lift the lockdown");
            return;
        }
        if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("off")) {
            if (!SauverLockdown.active()) {
                err(sender, "<#FF5555>The server is not in lockdown.");
                return;
            }
            SauverLockdown.end();
            send(sender, "<#B1C7F0>Lockdown lifted. Players may join again.");
            return;
        }
        String reason = String.join(" ", args);
        SauverLockdown.begin(reason);
        send(sender, "<#FF5555>The server is now in lockdown. <#FFFFFF>Reason: <#FFFFFF>" + reason);
    }

    private void geoip(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "geoip <player|IP>", "look up the country for a player or IP");
            return;
        }
        String ip;
        if (isIpLiteral(args[0])) {
            ip = args[0];
        } else {
            UUID u = resolve(args[0]);
            if (u == null) {
                err(sender, "<#FF5555>That player is unknown: <#FFFFFF>" + args[0] + "<#FF5555>.");
                return;
            }
            List<String> ips = dao().ipsOf(u);
            ip = ips.isEmpty() ? null : ips.get(0);
        }
        if (ip == null) {
            err(sender, "<#FF5555>No IP on record for that player.");
            return;
        }
        String country = SauverGeoIp.country(ip);
        if (country == null) {
            send(sender, "<#FFFFFF>GeoIP for <#FFFFFF>" + ip
                    + "<#FFFFFF>: unavailable (no GeoLite2 database configured).");
            return;
        }
        send(sender, "<#FFFFFF>GeoIP for <#FFFFFF>" + ip + "<#FFFFFF>: <#FFFFFF>" + country);
    }

    private void whois(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "whois <player>", "show a player's full account dossier");
            return;
        }
        String query = args[0];

        Player online = Bukkit.getPlayerExact(query);
        if (online != null) {
            renderWhois(sender, online.getUniqueId(), online.getName());
            return;
        }
        UUID known = resolve(query);
        if (known != null && dao().hasProfile(known)) {
            renderWhois(sender, known, bestName(known, query));
            return;
        }

        send(sender, "<#FFFFFF>Resolving <#FFFFFF>" + query + " <#FFFFFF>via Mojang...");
        Bukkit.getScheduler().runTaskAsynchronously(sauver.plugin(), () -> {
            SauverMojang.Profile prof = SauverMojang.lookup(query);
            Bukkit.getScheduler().runTask(sauver.plugin(), () -> {
                switch (prof.status()) {
                    case FOUND -> {
                        if (dao().hasProfile(prof.uuid())) {
                            renderWhois(sender, prof.uuid(), prof.name());
                        } else {
                            send(sender, "<#FFFFFF>Whois <#FFFFFF>" + prof.name()
                                    + " <#FFFFFF>(never joined this server).");
                            raw(sender, "<#FFFFFF>  UUID: <#FFFFFF>" + prof.uuid());
                            raw(sender, "<#FFFFFF>  Source: <#FFFFFF>Mojang <#FFFFFF>(no local history)");
                        }
                    }
                    case NOT_FOUND -> err(sender, "<#FF5555>No Minecraft account exists with the name <#FFFFFF>"
                            + query + "<#FF5555>.");
                    case UNKNOWN -> err(sender, "<#FF5555>Could not reach Mojang to resolve <#FFFFFF>"
                            + query + "<#FF5555>. Try again shortly.");
                }
            });
        });
    }

    private void renderWhois(CommandSender sender, UUID u, String fallbackName) {
        SauverDAO.Profile pr = dao().profile(u);
        String name = pr.name() != null ? pr.name() : fallbackName;
        long now = System.currentTimeMillis();

        send(sender, "<#FFFFFF>Whois <#FFFFFF>" + name + (pr.online() ? " <#B1C7F0>(online)" : "") + "<#FFFFFF>.");
        raw(sender, "<#FFFFFF>  UUID: <#FFFFFF>" + u);

        if (pr.names().size() > 1) {
            raw(sender, "<#FFFFFF>  Known names: <#FFFFFF>" + String.join("<#FFFFFF>, <#FFFFFF>", pr.names()));
        }

        String version = SauverProtocol.versionName(pr.protocol());
        raw(sender, "<#FFFFFF>  Version: <#FFFFFF>" + (version != null ? version : "unknown")
                + (pr.protocol() > 0 ? " <#FFFFFF>(protocol " + pr.protocol() + ")" : ""));

        String client = pr.lastClient() != null ? pr.lastClient() : "unknown";
        if (pr.clients().size() > 1) {
            raw(sender, "<#FFFFFF>  Client: <#FFFFFF>" + client
                    + " <#FFFFFF>(all: <#FFFFFF>" + String.join("<#FFFFFF>, <#FFFFFF>", pr.clients()) + "<#FFFFFF>)");
        } else {
            raw(sender, "<#FFFFFF>  Client: <#FFFFFF>" + client);
        }

        raw(sender, "<#FFFFFF>  Referrer: <#FFFFFF>" + (pr.referrer() != null ? pr.referrer() : "unknown"));

        String lastIp = pr.lastIp();
        if (lastIp != null) {
            raw(sender, "<#FFFFFF>  Country: <#FFFFFF>" + countryLabel(lastIp));
            if (sender.hasPermission("veritesauver.whois.ip")) {
                raw(sender, "<#FFFFFF>  Last IP: <#FFFFFF>" + lastIp
                        + " <#FFFFFF>(<#FFFFFF>" + pr.ips().size() + " <#FFFFFF>on record)");
            }
        }

        raw(sender, "<#FFFFFF>  Playtime: <#FFFFFF>" + (pr.playtimeMs() > 0
                ? SauverFormat.fancyTime(pr.playtimeMs()) : "none")
                + " <#FFFFFF>over <#FFFFFF>" + pr.joinCount() + " <#FFFFFF>" + SauverFormat.pluralize(pr.joinCount(), "join"));

        if (pr.firstJoin() > 0) {
            raw(sender, "<#FFFFFF>  First joined: <#FFFFFF>" + SauverFormat.fancyTime(now - pr.firstJoin()) + " <#FFFFFF>ago");
        }
        if (pr.lastSeen() > 0) {
            raw(sender, "<#FFFFFF>  Last seen: " + (pr.online() ? "<#B1C7F0>now"
                    : "<#FFFFFF>" + SauverFormat.fancyTime(now - pr.lastSeen()) + " <#FFFFFF>ago"));
        }

        String punishColor = pr.punishments() > 0 ? "<#FF5555>" : "<#FFFFFF>";
        raw(sender, "<#FFFFFF>  Punishments: " + punishColor + pr.punishments()
                + (pr.punishments() > 0 ? " <#FFFFFF>(see <#FFFFFF>/history " + name + "<#FFFFFF>)" : ""));

        String flagColor = pr.cheatFlags() > 0 ? "<#FF5555>" : "<#FFFFFF>";
        raw(sender, "<#FFFFFF>  Cheat flags: " + flagColor + pr.cheatFlags());
    }

    private String countryLabel(String ip) {
        if (!SauverGeoIp.available()) {
            return "geoip off <#FFFFFF>(no GeoLite2 database)";
        }
        if (SauverGeoIp.isPrivate(ip)) {
            return "unknown <#FFFFFF>(local/proxy IP, enable IP forwarding)";
        }
        String country = SauverGeoIp.country(ip);
        return country != null ? country : "unknown";
    }

    private void seen(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "seen <player>", "show when a player was last online");
            return;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null) {
            send(sender, "<#FFFFFF>" + online.getName() + " <#FFFFFF>is online now.");
            return;
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer(args[0]);
        if (off.getLastSeen() <= 0 && !off.hasPlayedBefore()) {
            err(sender, "<#FF5555>That player has never joined the server.");
            return;
        }
        long ago = System.currentTimeMillis() - off.getLastSeen();
        send(sender, "<#FFFFFF>" + args[0] + " <#FFFFFF>was last seen <#FFFFFF>"
                + SauverFormat.fancyTime(ago) + " <#FFFFFF>ago.");
    }

    private void punishAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("veritesauver.admin")) {
            err(sender, "<#FF5555>You don't have permission to do that.");
            return;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "info";
        switch (sub) {
            case "reload" -> {
                sauver.reload();
                send(sender, "<#B1C7F0>Reloaded config, templates, and geoip.");
            }
            case "info" -> {
                long now = System.currentTimeMillis();
                int bans = dao().activeOfType(Entry.Type.BAN, now).size();
                int mutes = dao().activeOfType(Entry.Type.MUTE, now).size();
                int warns = dao().allActiveWarnings(now).size();
                send(sender, "<#FFFFFF>Veritésauver <#FFFFFF>(moderation).");
                raw(sender, "<#FFFFFF>  Active bans: <#FFFFFF>" + bans);
                raw(sender, "<#FFFFFF>  Active mutes: <#FFFFFF>" + mutes);
                raw(sender, "<#FFFFFF>  Active warnings: <#FFFFFF>" + warns);
                raw(sender, "<#FFFFFF>  GeoIP: " + (SauverGeoIp.available() ? "<#B1C7F0>active" : "<#FFFFFF>unavailable"));
                raw(sender, "<#FFFFFF>  Lockdown: " + (SauverLockdown.active() ? "<#FF5555>active" : "<#B1C7F0>off"));
            }
            default -> usage(sender, "punish <reload|info>", "reload or inspect the moderation component");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        return switch (cmd) {
            case "ban", "mute"      -> tabIssue(sender, "veritesauver." + cmd, args, false);
            case "tempban"          -> tabIssue(sender, "veritesauver.ban", args, true);
            case "tempmute"         -> tabIssue(sender, "veritesauver.mute", args, true);
            case "ipban"            -> tabTarget(sender, "veritesauver.ipban", args);
            case "ipmute"           -> tabTarget(sender, "veritesauver.ipmute", args);
            case "kick"             -> tabTarget(sender, "veritesauver.kick", args);
            case "warn"             -> tabTarget(sender, "veritesauver.warn", args);
            case "unban"            -> tabTarget(sender, "veritesauver.unban", args);
            case "unmute"           -> tabTarget(sender, "veritesauver.unmute", args);
            case "whois"            -> tabWhois(sender, args);
            case "unwarn", "warnings", "checkwarn", "checkban", "checkmute", "dupeip",
                 "iphistory", "namehistory", "lastuuid", "history", "staffhistory",
                 "staffrollback", "prunehistory", "seen"
                                    -> tabTarget(sender, "veritesauver." + cmd, args);
            case "lockdown"         -> lockdownTab(sender, args);
            case "slowmode"         -> sender.hasPermission("veritesauver.slowmode")
                                            ? sauver.chat().slowmodeTab(args.length) : List.of();
            case "punish"           -> punishAdminTab(sender, args);
            default -> List.of();
        };
    }

    private List<String> tabIssue(CommandSender sender, String permission, String[] args, boolean temp) {
        if (!sender.hasPermission(permission)) {
            return List.of();
        }
        if (args.length == 1) {
            return matchOnline(args[0]);
        }
        if (args.length == 2) {
            List<String> out = new ArrayList<>();
            if ("-s".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                out.add("-s");
            }
            if (temp) {
                out.addAll(prefixed(SauverDuration.SUGGESTIONS, args[1]));
            }
            return out;
        }
        return List.of();
    }

    private List<String> tabTarget(CommandSender sender, String permission, String[] args) {
        if (!sender.hasPermission(permission)) {
            return List.of();
        }
        return args.length == 1 ? matchOnline(args[0]) : List.of();
    }

    private List<String> tabWhois(CommandSender sender, String[] args) {
        if (!sender.hasPermission("veritesauver.whois") || args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>(matchOnline(args[0]));
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String n : out) {
            seen.add(n.toLowerCase(Locale.ROOT));
        }
        for (String n : dao().knownNames()) {
            if (out.size() >= 50) {
                break;
            }
            if (n.startsWith(prefix) && seen.add(n)) {
                out.add(n);
            }
        }
        return out;
    }

    private List<String> lockdownTab(CommandSender sender, String[] args) {
        if (!sender.hasPermission("veritesauver.lockdown")) {
            return List.of();
        }
        return args.length == 1 ? prefixed(List.of("end"), args[0]) : List.of();
    }

    private List<String> punishAdminTab(CommandSender sender, String[] args) {
        if (!sender.hasPermission("veritesauver.admin")) {
            return List.of();
        }
        return args.length == 1 ? prefixed(List.of("reload", "info"), args[0]) : List.of();
    }

    private static List<String> matchOnline(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (pl.getName().toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(pl.getName());
            }
        }
        return out;
    }

    private static List<String> prefixed(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
