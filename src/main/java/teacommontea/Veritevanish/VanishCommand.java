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

package teacommontea.veritevanish;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import teacommontea.util.Messages;

import java.util.ArrayList;
import java.util.List;

public final class VanishCommand implements CommandExecutor, TabCompleter {

    private final Vanish vanish;
    private final Messages messages = new Messages();

    public VanishCommand(Vanish vanish) {
        this.vanish = vanish;
    }

    private void msg(CommandSender to, String mini) {
        to.sendMessage(messages.prefixed(mini));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                msg(sender, "<#FF5555>Console must name a player: <#FFFFFF>/vanish <player>");
                return true;
            }
            if (!p.hasPermission("verite.vanish")) {
                msg(sender, "<#FF5555>You may not vanish.");
                return true;
            }
            boolean nowVanished = vanish.toggle(p);
            msg(sender, nowVanished
                    ? "<#B1C7F0>You are now <#FFFFFF>vanished<#B1C7F0>."
                    : "<#B1C7F0>You are now <#FFFFFF>visible<#B1C7F0>.");
            return true;
        }

        if (!sender.hasPermission("verite.vanish.others")) {
            msg(sender, "<#FF5555>You may not vanish other players.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            msg(sender, "<#FFFFFF>" + args[0] + " <#FF5555>is not online.");
            return true;
        }
        boolean nowVanished = vanish.toggle(target);
        msg(sender, "<#FFFFFF>" + target.getName() + " <#B1C7F0>is now <#FFFFFF>"
                + (nowVanished ? "vanished" : "visible") + "<#B1C7F0>.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("verite.vanish.others")) {
            String pre = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {

                if (vanish.isVanished(p.getUniqueId()) && !Vanish.canSee(
                        sender instanceof Player sp ? sp : null)) {
                    continue;
                }
                if (p.getName().toLowerCase().startsWith(pre)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
