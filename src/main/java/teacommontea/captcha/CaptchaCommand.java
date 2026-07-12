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

import teacommontea.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CaptchaCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "veritesauver.captcha";

    private final CaptchaManager captcha;
    private final Messages messages;

    public CaptchaCommand(CaptchaManager captcha, Messages messages) {
        this.captcha = captcha;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages m = messages;
        if (!sender.hasPermission(PERMISSION)) {
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
        CaptchaKind kind = mode.equals("detailed") ? CaptchaKind.DETAILED : CaptchaKind.STANDARD;
        String targetArg = args[1];
        String source = "staff: " + sender.getName();

        if (targetArg.equals("*")) {
            int sent = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (captcha.challenge(p, kind, source)) sent++;
            }
            sender.sendMessage(m.prefixed("<#B1C7F0>Sent a " + mode + " captcha to <white>" + sent + " <#B1C7F0>eligible player(s)."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetArg);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(m.prefixed("<red>That player is not online."));
            return true;
        }
        if (!captcha.challenge(target, kind, source)) {
            sender.sendMessage(m.prefixed("<white>" + target.getName()
                    + " <red>could not be challenged (exempt or already has one open)."));
            return true;
        }
        sender.sendMessage(m.prefixed("<#B1C7F0>Sent a " + mode + " captcha to <white>" + target.getName() + "<#B1C7F0>."));
        return true;
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
