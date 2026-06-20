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

package teacommontea.veritechasse.command;

import teacommontea.veritechasse.AntiCheat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class VeriteCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "verite.admin";

    private final AntiCheat antiCheat;

    public VeriteCommand(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        var messages = antiCheat.messages();

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(messages.prefixed("<#FFFFFF>Verité v"
                    + antiCheat.getPlugin().getPluginMeta().getVersion()
                    + "<#FFFFFF>. <#B1C7F0>Cheat detection."));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            antiCheat.getPlugin().reloadConfig();
            antiCheat.reloadMessages();
            sender.sendMessage(messages.prefixed("<#B1C7F0>Configuration reloaded."));
            return true;
        }
        sender.sendMessage(messages.prefixed("<#FFFFFF>Usage: /" + label + " [info|reload]"));
        return true;
    }

    public void replay(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        var messages = antiCheat.messages();
        if (!(sender instanceof org.bukkit.entity.Player viewer)) {
            sender.sendMessage(messages.prefixed("<#FF5555>Replay can only be viewed in game."));
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
            boolean stopped = antiCheat.getReplayManager().stopPlayback(viewer);
            viewer.sendMessage(messages.prefixed(stopped ? "<#B1C7F0>Replay ended." : "<#FFFFFF>No replay playing."));
            return;
        }
        if (args.length < 1) {
            viewer.sendMessage(messages.prefixed("<#FFFFFF>Usage: /replay <player> [id] <#FFFFFF>| /replay stop"));
            return;
        }
        String target = args[0];
        teacommontea.veritechasse.replay.ReplayRecord record = args.length >= 2
                ? antiCheat.getReplayManager().store().load(args[1])
                : antiCheat.getReplayManager().store().latestFor(target);
        if (record == null) {
            viewer.sendMessage(messages.prefixed("<#FF5555>No replay found for <#FFFFFF>" + target + "<#FF5555>."));
            return;
        }
        if (antiCheat.getReplayManager().play(viewer, record)) {
            viewer.sendMessage(messages.prefixed("<#B1C7F0>Playing replay <#FFFFFF>" + record.id
                    + " <#FFFFFF>(" + record.check + "). <#FFFFFF>Use <#FFFFFF>/replay stop <#FFFFFF>to exit."));
        } else {
            viewer.sendMessage(messages.prefixed("<#FF5555>Replay world not ready."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("info", "reload"), args[0]);
        }
        return List.of();
    }

    public List<String> replayTab(String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("stop");
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            List<String> ids = new ArrayList<>();
            for (teacommontea.veritechasse.replay.ReplayRecord r : antiCheat.getReplayManager().store().forPlayer(args[0])) {
                ids.add(r.id);
            }
            return filter(ids, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(lower)) {
                out.add(o);
            }
        }
        return out;
    }
}
