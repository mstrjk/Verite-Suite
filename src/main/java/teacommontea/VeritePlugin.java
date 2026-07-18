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

package teacommontea;

import org.bukkit.Bukkit;
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

import teacommontea.captcha.CaptchaCommand;
import teacommontea.captcha.CaptchaManager;
import teacommontea.util.Messages;

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

    private boolean douxEnabled;
    private boolean sauverEnabled;
    private boolean vanishEnabled;

    private teacommontea.veritedoux.intercept.ChatIntercept chatIntercept;

    private teacommontea.veritevanish.Vanish vanish;

    private Sauver sauver;
    private SauverCommands sauverCommands;

    private CaptchaManager captcha;

    private static final String[] SAUVER_COMMANDS = {
            "ban", "tempban", "mute", "tempmute", "ipban", "ipmute", "kick",
            "warn", "unwarn", "warnings", "checkwarn", "warnlist",
            "unban", "unmute", "checkban", "checkmute",
            "dupeip", "iphistory", "namehistory", "lastuuid",
            "banlist", "mutelist", "history", "staffhistory", "staffrollback", "prunehistory",
            "lockdown", "geoip", "whois", "seen", "punish",
            "bc", "chatclear", "chatmute", "slowmode"};

    private final Messages messages = new Messages();

    private void msg(CommandSender to, String miniMessage) {
        to.sendMessage(messages.prefixed(miniMessage));
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        extractResource("sieve/settings.yml", "settings.yml");

        teacommontea.veritedoux.SelfHeal.healSettings(this);
        readComponentToggles();

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
        if (vanishEnabled) {
            bringUpVanish();
        } else {
            getLogger().info("[Verite] Veritevanish (staff vanish) disabled in settings.yml.");
        }

        registerCommand("verite", this);

        if (sauverEnabled && sauverCommands != null) {
            for (String name : SAUVER_COMMANDS) {
                registerCommand(name, sauverCommands);
            }
            if (captcha != null) {
                registerCommand("captcha", new CaptchaCommand(captcha, messages));
            }
        }
        if (vanishEnabled && vanish != null) {
            registerCommand("vanish", new teacommontea.veritevanish.VanishCommand(vanish));
        }
    }

    private void bringUpVanish() {
        try {
            this.vanish = teacommontea.veritevanish.Vanish.enable(this);
        } catch (Exception e) {
            getLogger().warning("[Verite] Veritevanish failed to start (vanish off): " + e.getMessage());
            this.vanish = null;
        }
    }

    @Override
    public void onDisable() {
        if (chatIntercept != null) {
            chatIntercept.shutdown();
            chatIntercept = null;
        }
        if (douxEnabled) {
            Sieve.shutdown();
        }
        if (captcha != null) {
            captcha.shutdown();
            captcha = null;
        }
        if (sauver != null) {
            sauver.disable();
            sauver = null;
        }
        if (vanish != null) {
            vanish.disable();
            vanish = null;
        }
    }

    private void readComponentToggles() {
        File f = new File(getDataFolder(), "settings.yml");
        if (f.isFile()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            this.douxEnabled = y.getBoolean("components.veritedoux.enabled", true);
            this.sauverEnabled = y.getBoolean("components.veritesauver.enabled", true);
            this.vanishEnabled = y.getBoolean("components.veritevanish.enabled", true);
            Messages.setPrefix(y.getString("prefix", Messages.DEFAULT_PREFIX));
        } else {

            this.douxEnabled = true;
            this.sauverEnabled = true;
            this.vanishEnabled = true;
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

    private static final long FILTER_LOAD_BUDGET_MS = 30_000;

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

        if (!loadFilterBounded()) {

            douxEnabled = false;
            return;
        }

        try {
            Sieve.enableStore(SieveStore.open(this));
        } catch (Exception e) {
            getLogger().warning("[Verite] Si.EVE store off (count() will return 0): " + e.getMessage());
        }

    }

    private boolean loadFilterBounded() {
        final Throwable[] failure = new Throwable[1];
        Thread worker = new Thread(() -> {
            try {

                SieveCommands.loadAll(this);
            } catch (Throwable t) {
                failure[0] = t;
            }
        }, "Verite-FilterLoad");
        worker.setDaemon(true);
        worker.start();
        try {
            worker.join(FILTER_LOAD_BUDGET_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            getLogger().severe("[Verite] filter load interrupted during boot:");
            ie.printStackTrace();
            return false;
        }

        if (worker.isAlive()) {

            StringBuilder where = new StringBuilder("[Verite] chat-filter load exceeded "
                    + FILTER_LOAD_BUDGET_MS + "ms budget and STALLED. Worker stack at timeout:\n");
            for (StackTraceElement el : worker.getStackTrace()) {
                where.append("    at ").append(el).append('\n');
            }
            getLogger().severe(where.toString());
            worker.interrupt();
            return false;
        }
        if (failure[0] != null) {
            getLogger().severe("[Verite] chat-filter load FAILED with an exception:");
            failure[0].printStackTrace();
            return false;
        }
        return true;
    }

    private static final class ChatEventGuard implements org.bukkit.event.Listener {
        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = true)
        public void onChat(io.papermc.paper.event.player.AsyncChatEvent e) {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(e.message());
            if (text != null && !text.isEmpty()
                    && Sieve.check(e.getPlayer().getUniqueId(), text) != Sieve.Result.CLEAN) {
                e.setCancelled(true);
            }
        }
    }

    private void bringUpModeration() {
        try {
            this.sauver = Sauver.enable(this);
            this.sauverCommands = new SauverCommands(sauver);

            this.captcha = new CaptchaManager(this, messages);
            getServer().getPluginManager().registerEvents(captcha.standardListener(), this);
            getServer().getPluginManager().registerEvents(captcha.detailedListener(), this);
        } catch (Exception e) {
            getLogger().warning("[Verite] Veritesauver failed to start (moderation off): " + e.getMessage());
            this.sauverEnabled = false;
        }
    }

    private void registerCommand(String name, Object handler) {
        org.bukkit.command.PluginCommand pc = getCommand(name);
        if (pc != null) {
            pc.setExecutor((CommandExecutor) handler);
            pc.setTabCompleter((TabCompleter) handler);
            return;
        }

        getLogger().warning("[Verite] /" + name + " was not in the command map; registering dynamically.");
        try {
            java.lang.reflect.Constructor<org.bukkit.command.PluginCommand> ctor =
                    org.bukkit.command.PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            ctor.setAccessible(true);
            org.bukkit.command.PluginCommand cmd = ctor.newInstance(name, this);
            cmd.setExecutor((CommandExecutor) handler);
            cmd.setTabCompleter((TabCompleter) handler);

            java.lang.reflect.Method getMap = Bukkit.getServer().getClass().getMethod("getCommandMap");
            org.bukkit.command.CommandMap map = (org.bukkit.command.CommandMap) getMap.invoke(Bukkit.getServer());
            map.register(getName().toLowerCase(java.util.Locale.ROOT), cmd);
        } catch (Exception e) {
            getLogger().warning("[Verite] Dynamic registration of /" + name + " failed: " + e);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            String on = "<#B1C7F0>on", off = "<#FF5555>off";
            msg(sender, "<#FFFFFF>Veritédoux <#FFFFFF>(chat filter)<#FFFFFF>: " + (douxEnabled ? on : off));
            msg(sender, "<#FFFFFF>Veritésauver <#FFFFFF>(moderation)<#FFFFFF>: " + (sauverEnabled ? on : off));
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            msg(sender, "<#FFFFFF>Verité <#FFFFFF>v<#FFFFFF>" + getDescription().getVersion()
                    + "<#FFFFFF>. <#FFFFFF>Chat filter <#FFFFFF>+ <#FFFFFF>moderation.");
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
            default -> msg(sender, "<#FFFFFF>/verite <#FFFFFF>status <#FFFFFF>| <#FFFFFF>info <#FFFFFF>| <#FFFFFF>reload <#FFFFFF>| <#FFFFFF>count <#FFFFFF><<#FFFFFF>player<#FFFFFF>>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("verite")) {
            List<String> out = new ArrayList<>();
            if (args.length == 1) {
                for (String s : List.of("status", "info", "reload", "count")) if (s.startsWith(args[0].toLowerCase())) out.add(s);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("count")) {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
            return out;
        }
        return List.of();
    }
}
