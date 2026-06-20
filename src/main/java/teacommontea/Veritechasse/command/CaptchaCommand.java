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
import teacommontea.veritechasse.punish.CaptchaManager;
import teacommontea.veritechasse.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CaptchaCommand implements CommandExecutor, TabCompleter {

    private static final String STAFF_CHECK = "staff";

    private final AntiCheat antiCheat;

    public CaptchaCommand(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages m = antiCheat.messages();
        if (!sender.hasPermission("verite.admin")) {
            sender.sendMessage(m.prefixed("<red>You do not have permission."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(m.prefixed("<white>Usage: <white>/captcha <standard|detailed> <player|*>"));
            return true;
        }
        String mode = args[0].toLowerCase();
        if (!mode.equals("standard") && !mode.equals("detailed")) {
            sender.sendMessage(m.prefixed("<red>Invalid mode. Use <white>standard <red>or <white>detailed<red>."));
            return true;
        }
        String targetArg = args[1];
        String senderName = sender.getName();

        if (targetArg.equals("*")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(CaptchaManager.BYPASS_PERMISSION) || isActive(p)) continue;
                challenge(p, mode, senderName);
            }
            sender.sendMessage(m.prefixed("<#B1C7F0>Sent a " + mode + " captcha to all eligible players."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetArg);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(m.prefixed("<red>That player is not online."));
            return true;
        }
        if (isActive(target)) {
            sender.sendMessage(m.prefixed("<white>" + target.getName() + " <red>already has a captcha open."));
            return true;
        }
        challenge(target, mode, senderName);
        sender.sendMessage(m.prefixed("<#B1C7F0>Sent a " + mode + " captcha to <white>" + target.getName() + "<#B1C7F0>."));
        return true;
    }

    private void challenge(Player p, String mode, String staffName) {
        CaptchaManager cm = antiCheat.getCaptchaManager();
        String evidence = "staff: " + staffName;
        if (mode.equals("detailed")) {
            cm.detailed().open(p, STAFF_CHECK, evidence, 0.0);
        } else {
            cm.standard().open(p, STAFF_CHECK, evidence, 0.0);
        }
    }

    private boolean isActive(Player p) {
        CaptchaManager cm = antiCheat.getCaptchaManager();
        return cm.standard().isActive(p.getUniqueId()) || cm.detailed().isActive(p.getUniqueId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String mode : List.of("standard", "detailed")) {
                if (mode.startsWith(args[0].toLowerCase())) out.add(mode);
            }
            return out;
        }
        if (args.length == 2) {
            List<String> out = new ArrayList<>();
            out.add("*");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
            return out;
        }
        return List.of();
    }
}
