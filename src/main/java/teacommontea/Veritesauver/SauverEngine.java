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

import teacommontea.veritesauver.util.SauverFormat;
import teacommontea.veritesauver.util.SauverMessages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class SauverEngine {

    public static final String CONSOLE_NAME = "Console";

    private SauverEngine() {}

    private static SauverDAO dao() {
        return Sauver.instance().dao();
    }

    private static SauverMessages msg() {
        return Sauver.instance().messages();
    }

    public record Result(boolean ok, Entry entry, String error) {
        static Result fail(String why) { return new Result(false, null, why); }
        static Result done(Entry e)    { return new Result(true, e, null); }
    }

    public static Result issue(Entry.Type type, UUID targetUuid, String targetIp, String targetName,
                               String reason, UUID executorUuid, String executorName,
                               long durationMillis, boolean silent, boolean ipban) {
        if (targetUuid == null && targetIp == null) {
            return Result.fail("A punishment needs a target uuid or IP.");
        }
        long now = System.currentTimeMillis();
        long dateEnd = durationMillis == Entry.PERMANENT ? Entry.PERMANENT : now + durationMillis;
        long id = dao().nextId();
        Entry e = new Entry(
            id, String.valueOf(id), type, targetUuid, targetIp, reason,
            executorUuid, executorName, null, null, null,
            now, dateEnd, Entry.GLOBAL_SCOPE, Entry.GLOBAL_SCOPE,
            0, silent, ipban, true);
        dao().save(e);

        enforce(e, targetName);
        announce(e, targetName);
        SauverEvents.fireAdded(e);
        return Result.done(e);
    }

    public static Result kick(UUID targetUuid, String targetName, String reason,
                              UUID executorUuid, String executorName, boolean silent) {
        Player online = targetUuid == null ? null : Bukkit.getPlayer(targetUuid);
        if (online == null) {
            return Result.fail(targetName + " is not online.");
        }
        long now = System.currentTimeMillis();
        long id = dao().nextId();
        Entry e = new Entry(
            id, String.valueOf(id), Entry.Type.KICK, targetUuid,
            addressOf(online), reason, executorUuid, executorName, null, null, null,
            now, now, Entry.GLOBAL_SCOPE, Entry.GLOBAL_SCOPE, 0, silent, false, false);
        dao().save(e);

        online.kick(SauverMessages.screen("<#FF5555>You were kicked.<newline><newline>"
                + "<#FFFFFF>Reason: <#FFFFFF>" + safeReason(reason)));
        announce(e, targetName);
        SauverEvents.fireAdded(e);
        return Result.done(e);
    }

    private static String addressOf(Player p) {
        return p.getAddress() == null || p.getAddress().getAddress() == null
                ? null : p.getAddress().getAddress().getHostAddress();
    }

    public static long warningExpire() {
        return SauverConfig.warningExpire();
    }

    public static Result warn(UUID targetUuid, String targetName, String reason,
                              UUID executorUuid, String executorName, boolean silent) {
        if (targetUuid == null) {
            return Result.fail("A warning needs a known player.");
        }
        long now = System.currentTimeMillis();
        long id = dao().nextId();
        Entry e = new Entry(
            id, String.valueOf(id), Entry.Type.WARNING, targetUuid, null, reason,
            executorUuid, executorName, null, null, null,
            now, now + warningExpire(), Entry.GLOBAL_SCOPE, Entry.GLOBAL_SCOPE,
            0, silent, false, true);
        dao().save(e);

        announce(e, targetName);
        SauverEvents.fireAdded(e);

        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null && !silent) {
            msg().send(online, "<#FF5555>You were warned. <#FFFFFF>Reason: <#FFFFFF>" + safeReason(reason));
        }
        int active = dao().activeWarnings(targetUuid, now).size();
        SauverWarnActions.onWarn(targetUuid, targetName, active);
        return Result.done(e);
    }

    public static Result unwarn(UUID targetUuid, String targetName, UUID removedByUuid,
                                String removedByName, String removalReason) {
        long now = System.currentTimeMillis();
        java.util.List<Entry> active = dao().activeWarnings(targetUuid, now);
        if (active.isEmpty()) {
            return Result.fail(targetName + " has no active warnings.");
        }
        Entry latest = active.get(0);
        Entry removed = latest.withRemoval(removedByUuid, removedByName, removalReason);
        dao().save(removed);
        SauverEvents.fireRemoved(removed);
        return Result.done(removed);
    }

    public static Result issueTemplate(Entry.Type type, UUID targetUuid, String targetName,
                                       SauverTemplates.Template template, UUID executorUuid,
                                       String executorName, boolean silent) {
        if (targetUuid == null) {
            return Result.fail("A template punishment needs a known player.");
        }
        long now = System.currentTimeMillis();
        int templateId = SauverTemplates.templateId(type, template.name());
        long cutoff = template.expireLadder() == Entry.PERMANENT ? 0 : now - template.expireLadder();
        int prior = dao().templateOffenses(targetUuid, templateId, cutoff);
        SauverTemplates.Rung rung = SauverTemplates.rungFor(template, prior);

        long dateEnd = rung.duration() == Entry.PERMANENT ? Entry.PERMANENT : now + rung.duration();
        long id = dao().nextId();
        boolean enforced = type == Entry.Type.BAN || type == Entry.Type.MUTE;
        Entry e = new Entry(
            id, String.valueOf(id), type, targetUuid, null, rung.reason(),
            executorUuid, executorName, null, null, null,
            now, type == Entry.Type.KICK ? now : dateEnd, Entry.GLOBAL_SCOPE, Entry.GLOBAL_SCOPE,
            templateId, silent, false, enforced || type == Entry.Type.WARNING);
        dao().save(e);

        enforce(e, targetName);
        announce(e, targetName);
        SauverEvents.fireAdded(e);
        runActions(rung.actions(), targetName);
        return Result.done(e);
    }

    private static void runActions(java.util.List<String> actions, String targetName) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (String raw : actions) {
            String cmd = raw.replace("$playerName", targetName).replace("$player", targetName);
            String trimmed = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            Bukkit.getScheduler().runTask(Sauver.instance().plugin(), () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed));
        }
    }

    public static Result issueIp(Entry.Type type, UUID targetUuid, String ip, String targetName,
                                 String reason, UUID executorUuid, String executorName,
                                 long durationMillis, boolean silent) {
        if (ip == null) {
            return Result.fail("No IP is known for " + targetName + " (they must have logged in once).");
        }
        long now = System.currentTimeMillis();
        long dateEnd = durationMillis == Entry.PERMANENT ? Entry.PERMANENT : now + durationMillis;
        long id = dao().nextId();
        Entry e = new Entry(
            id, String.valueOf(id), type, targetUuid, ip, reason,
            executorUuid, executorName, null, null, null,
            now, dateEnd, Entry.GLOBAL_SCOPE, Entry.GLOBAL_SCOPE,
            0, silent, true, true);
        dao().save(e);

        if (type == Entry.Type.BAN) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (ip.equals(addressOf(p))) {
                    p.kick(SauverMessages.screen(banScreen(e)));
                }
            }
        }
        announce(e, targetName);
        SauverEvents.fireAdded(e);
        return Result.done(e);
    }

    public static Result pardon(Entry.Type type, UUID targetUuid, String targetName,
                                UUID removedByUuid, String removedByName, String removalReason) {
        Entry active = type == Entry.Type.BAN ? dao().activeBan(targetUuid) : dao().activeMute(targetUuid);
        if (active == null || !active.active()) {
            return Result.fail("That player is not " + (type == Entry.Type.BAN ? "banned" : "muted") + ".");
        }
        Entry pardoned = active.withRemoval(removedByUuid, removedByName, removalReason);
        dao().save(pardoned);

        SauverEvents.fireRemoved(pardoned);
        return Result.done(pardoned);
    }

    private static void enforce(Entry e, String targetName) {
        if (e.type() != Entry.Type.BAN || e.uuid() == null) {
            return;
        }
        Player online = Bukkit.getPlayer(e.uuid());
        if (online != null) {
            online.kick(SauverMessages.screen(banScreen(e)));
        }
    }

    public static String banScreen(Entry e) {
        String when = e.permanent()
                ? "<#FF5555>This ban is permanent."
                : "<#FF5555>Expires in <#FFFFFF>" + SauverFormat.fancyTime(e.remaining(System.currentTimeMillis())) + "<#FF5555>.";
        return "<#FF5555><bold>You are banned.</bold><newline><newline><#FFFFFF>Reason: <#FFFFFF>" + safeReason(e.reason())
                + "<newline>" + when + "<newline><newline><#FFFFFF>Appeal with ID <#FFFFFF>#" + e.randomId();
    }

    public static String muteLine(Entry e) {
        String when = e.permanent()
                ? "<#FF5555>This mute is permanent."
                : "<#FF5555>Expires in <#FFFFFF>" + SauverFormat.fancyTime(e.remaining(System.currentTimeMillis())) + "<#FF5555>.";
        return "<#FF5555>You are muted. <#FFFFFF>Reason: <#FFFFFF>" + safeReason(e.reason()) + " " + when;
    }

    private static void announce(Entry e, String targetName) {
        String verb = switch (e.type()) {
            case BAN -> e.permanent() ? "permanently banned" : "banned";
            case MUTE -> e.permanent() ? "permanently muted" : "muted";
            case WARNING -> "warned";
            case KICK -> "kicked";
        };
        String dur = e.permanent() ? "" : " <#FFFFFF>for <#FFFFFF>" + SauverFormat.fancyTime(e.duration());
        if (e.silent()) {
            String line = "<#555555>[<#FFFFFF>Silent<#555555>] <#FFFFFF>" + targetName + " <#FFFFFF>was <#FF5555>" + verb
                    + " <#FFFFFF>by <#FFFFFF>" + e.executorName();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("veritesauver.notify.silent")) {
                    msg().send(p, line);
                }
            }
            SauverEvents.fireBroadcast(line, targetName);
            return;
        }
        String line = "<#FFFFFF>" + targetName + " <#FFFFFF>was <#FF5555>" + verb + dur
                + " <#FFFFFF>by <#FFFFFF>" + e.executorName() + "<#FFFFFF>. Reason: <#FFFFFF>" + safeReason(e.reason());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("veritesauver.notify.broadcast")) {
                msg().send(p, line);
            }
        }
        SauverEvents.fireBroadcast(line, targetName);
    }

    private static String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "No reason specified." : reason;
    }
}
