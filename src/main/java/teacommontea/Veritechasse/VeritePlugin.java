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

package teacommontea.veritechasse;

import teacommontea.veritechasse.command.AlertsCommand;
import teacommontea.veritechasse.command.VeriteCommand;
import teacommontea.veritechasse.listener.BukkitListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import teacommontea.veritedoux.Sieve;
import teacommontea.veritedoux.SieveCommands;
import teacommontea.veritedoux.SieveStore;

import teacommontea.veritesauver.Sauver;
import teacommontea.veritesauver.command.SauverCommands;

public final class VeritePlugin extends JavaPlugin
        implements CommandExecutor, TabCompleter {

    private static final String[] SIEVE_CONFIGS = {
            "config_en.eve", "config_es.eve", "config_fr.eve",
            "config_it.eve", "config_pt.eve", "config_de.eve",
            "config_en_profanity.eve"};

    private boolean chasseEnabled;
    private boolean douxEnabled;
    private boolean traceConsole;
    private boolean sauverEnabled;

    private Sauver sauver;
    private SauverCommands sauverCommands;

    private static final String[] SAUVER_COMMANDS = {
            "ban", "tempban", "mute", "tempmute", "ipban", "ipmute", "kick",
            "warn", "unwarn", "warnings", "checkwarn", "warnlist",
            "unban", "unmute", "checkban", "checkmute",
            "dupeip", "iphistory", "namehistory", "lastuuid",
            "banlist", "mutelist", "history", "staffhistory", "staffrollback", "prunehistory",
            "lockdown", "geoip", "whois", "seen", "punish",
            "bc", "chatclear", "chatmute", "slowmode"};

    private AntiCheat antiCheat;
    private int tickTaskId = -1;

    private final teacommontea.veritechasse.util.Messages messages =
            new teacommontea.veritechasse.util.Messages(null);

    private void msg(org.bukkit.command.CommandSender to, String miniMessage) {
        to.sendMessage(messages.prefixed(miniMessage));
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        extractResource("sieve/settings.yml", "settings.yml");

        teacommontea.veritedoux.SelfHeal.healSettings(this);
        readComponentToggles();

        if (chasseEnabled) {
            bringUpAntiCheat();
        } else {
            getLogger().info("[Verite] Veritechasse (anticheat) disabled in settings.yml.");
        }
        if (douxEnabled) {
            bringUpFilter();
        } else {
            getLogger().info("[Verite] Veritedoux (chat filter) disabled in settings.yml.");
        }
        if (sauverEnabled) {
            bringUpModeration();
        } else {
            getLogger().info("[Verite] Veritesauver (moderation) disabled in settings.yml.");
        }

        registerCommand("verite", this);
        if (chasseEnabled && antiCheat != null) {
            registerCommand("alerts", this);
            registerCommand("replay", this);
            registerCommand("captcha", new teacommontea.veritechasse.command.CaptchaCommand(antiCheat));
        }

        if (sauverEnabled && sauverCommands != null) {
            for (String name : SAUVER_COMMANDS) {
                registerCommand(name, sauverCommands);
            }
        }
    }

    @Override
    public void onDisable() {
        if (tickTaskId != -1) {
            getServer().getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        if (antiCheat != null) {
            antiCheat.getDetectionLog().flushAll();
            antiCheat.getTrace().shutdown();
            antiCheat = null;
        }
        if (douxEnabled) {
            Sieve.shutdown();
        }
        if (sauver != null) {
            sauver.disable();
            sauver = null;
        }
    }

    private void readComponentToggles() {
        File f = new File(getDataFolder(), "settings.yml");
        if (f.isFile()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            this.chasseEnabled = y.getBoolean("components.veritechasse.enabled", true);
            this.douxEnabled = y.getBoolean("components.veritedoux.enabled", true);
            this.sauverEnabled = y.getBoolean("components.veritesauver.enabled", true);
            teacommontea.veritechasse.util.Messages.setPrefix(
                    y.getString("prefix", teacommontea.veritechasse.util.Messages.DEFAULT_PREFIX));
        } else {

            this.chasseEnabled = true;
            this.douxEnabled = true;
            this.sauverEnabled = true;
        }
    }

    private void extractResource(String resource, String targetName) {
        File out = new File(getDataFolder(), targetName);
        if (out.exists()) return;
        try (java.io.InputStream in = getResource(resource)) {
            if (in != null) java.nio.file.Files.copy(in, out.toPath());
        } catch (Exception e) {
            getLogger().warning("[Verite] could not extract " + targetName + ": " + e.getMessage());
        }
    }

    private void bringUpAntiCheat() {

        this.antiCheat = new AntiCheat(this);
        teacommontea.veritechasse.api.Verite.init(antiCheat);

        getServer().getPluginManager().registerEvents(new BukkitListener(antiCheat), this);
        getServer().getPluginManager().registerEvents(antiCheat.getCaptchaManager().standard(), this);
        getServer().getPluginManager().registerEvents(antiCheat.getCaptchaManager().detailed(), this);
        getServer().getPluginManager().registerEvents(antiCheat.getReplayManager(), this);

        SnapshotCapture capture = new SnapshotCapture();
        this.tickTaskId = getServer().getScheduler().runTaskTimer(this, () -> {
            antiCheat.getTickManager().tick();
            for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                var data = antiCheat.getPlayerDataManager().get(p.getUniqueId());
                if (data != null) {
                    data.getChecks().onTick(p);
                    data.guiMoveExempt = capture.computeGuiExempt(p);
                    capture.captureSnapshot(data, p);
                    data.getReplay().tick(p);
                }
            }
        }, 1L, 1L).getTaskId();
    }

    private void bringUpFilter() {
        for (String config : SIEVE_CONFIGS) {
            File edited = new File(getDataFolder(), config);
            if (!edited.exists()) {
                try (java.io.InputStream in = getResource("sieve/" + config)) {
                    if (in != null) java.nio.file.Files.copy(in, edited.toPath());
                } catch (Exception e) {
                    getLogger().warning("[Verite] could not extract " + config + ": " + e.getMessage());
                }
            }
        }

        teacommontea.veritedoux.SelfHeal.healEve(this, SIEVE_CONFIGS);

        SieveCommands.loadAll(this);
        try {
            Sieve.enableStore(SieveStore.open(this));
        } catch (Exception e) {
            getLogger().warning("[Verite] Si.EVE store off (count() will return 0): " + e.getMessage());
        }
    }

    private void bringUpModeration() {
        try {
            this.sauver = Sauver.enable(this);
            this.sauverCommands = new SauverCommands(sauver);
        } catch (Exception e) {
            getLogger().warning("[Verite] Veritesauver failed to start (moderation off): " + e.getMessage());
            this.sauverEnabled = false;
        }
    }

    private void registerCommand(String name, Object handler) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor((CommandExecutor) handler);
            getCommand(name).setTabCompleter((TabCompleter) handler);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("alerts")) {
            if (!chasseEnabled) { msg(sender, "<#FF5555>Veritéchasse is disabled in settings.yml."); return true; }
            return new AlertsCommand(antiCheat).onCommand(sender, command, label, args);
        }
        if (cmd.equals("replay")) {
            if (!chasseEnabled) { msg(sender, "<#FF5555>Veritéchasse is disabled in settings.yml."); return true; }
            new VeriteCommand(antiCheat).replay(sender, args);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            String on = "<#B1C7F0>on", off = "<#FF5555>off";
            msg(sender, "<#FFFFFF>Veritéchasse <#FFFFFF>(anticheat)<#FFFFFF>: " + (chasseEnabled ? on : off));
            msg(sender, "<#FFFFFF>Veritédoux <#FFFFFF>(chat filter)<#FFFFFF>: " + (douxEnabled ? on : off));
            msg(sender, "<#FFFFFF>Veritésauver <#FFFFFF>(moderation)<#FFFFFF>: " + (sauverEnabled ? on : off));
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            msg(sender, "<#FFFFFF>Verité <#FFFFFF>v<#FFFFFF>" + getPluginMeta().getVersion()
                    + "<#FFFFFF>. <#FFFFFF>Accuracy-first anticheat <#FFFFFF>+ <#FFFFFF>chat filter.");
            return true;
        }

        if (!sender.hasPermission("verite.admin")) {
            msg(sender, "<#FF5555>You don't have permission to do that.");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (douxEnabled) {
                    SieveCommands.loadAll(this);
                    msg(sender, "<#FFFFFF>Reloaded <#B1C7F0>Veritédoux <#FFFFFF>(chat filter)<#FFFFFF>.");
                }
                if (chasseEnabled && antiCheat != null) {
                    reloadConfig();
                    antiCheat.reloadMessages();
                    msg(sender, "<#FFFFFF>Reloaded <#B1C7F0>Veritéchasse <#FFFFFF>config. <#FFFFFF>(check toggles need a restart.)");
                }
                if (sauverEnabled && sauver != null) {
                    sauver.reload();
                    msg(sender, "<#FFFFFF>Reloaded <#B1C7F0>Veritésauver <#FFFFFF>config, templates, and geoip.");
                }
            }
            case "count" -> {
                if (!douxEnabled) { msg(sender, "<#FF5555>The chat filter is disabled."); return true; }
                if (args.length < 2) { msg(sender, "<#FFFFFF>Usage<#FFFFFF>: <#FFFFFF>/verite count <#FFFFFF><<#FFFFFF>player<#FFFFFF>>"); return true; }
                org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
                msg(sender, "<#FFFFFF>" + args[1] + " <#FFFFFF>has <#FF5555>"
                        + teacommontea.veritedoux.Sieve.count(t.getUniqueId()) + " <#FFFFFF>flags.");
            }
            case "trace" -> {
                if (!chasseEnabled || antiCheat == null) { msg(sender, "<#FF5555>Veritéchasse is disabled."); return true; }
                teacommontea.veritechasse.manager.VeriteTrace t = antiCheat.getTrace();
                String mode = args.length >= 2 ? args[1].toLowerCase() : "status";
                switch (mode) {
                    case "on" -> { t.setEnabled(true); msg(sender, "<#B1C7F0>Trace ON <#FFFFFF>→ plugins/Verite/verite-trace.jsonl"); }
                    case "off" -> { t.setEnabled(false); msg(sender, "<#FF5555>Trace OFF."); }
                    case "console" -> { boolean now = !traceConsole; traceConsole = now; t.setEchoConsole(now); msg(sender, "<#FFFFFF>Trace console echo <#FFFFFF>" + (now ? "<#B1C7F0>on" : "<#FF5555>off")); }
                    default -> msg(sender, "<#FFFFFF>Trace is <#FFFFFF>" + (t.isEnabled() ? "<#B1C7F0>on" : "<#FF5555>off") + "<#FFFFFF>. <#FFFFFF>/verite trace <#FFFFFF><<#FFFFFF>on<#FFFFFF>|<#FFFFFF>off<#FFFFFF>|<#FFFFFF>console<#FFFFFF>>");
                }
            }
            default -> msg(sender, "<#FFFFFF>/verite <#FFFFFF>status <#FFFFFF>| <#FFFFFF>info <#FFFFFF>| <#FFFFFF>reload <#FFFFFF>| <#FFFFFF>count <#FFFFFF><<#FFFFFF>player<#FFFFFF>> <#FFFFFF>| <#FFFFFF>trace");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("replay") && chasseEnabled && antiCheat != null) {
            return new VeriteCommand(antiCheat).replayTab(args);
        }
        if (cmd.equals("verite")) {
            List<String> out = new ArrayList<>();
            if (args.length == 1) {
                for (String s : List.of("status", "info", "reload", "count", "trace")) if (s.startsWith(args[0].toLowerCase())) out.add(s);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("count")) {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("trace")) {
                for (String s : List.of("on", "off", "console")) if (s.startsWith(args[1].toLowerCase())) out.add(s);
            }
            return out;
        }
        return List.of();
    }

    public AntiCheat getAntiCheat() {
        return antiCheat;
    }
}
