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
import teacommontea.veritechasse.manager.ViolationManager;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AlertsCommand implements CommandExecutor {

    private final AntiCheat antiCheat;

    public AlertsCommand(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(antiCheat.messages().prefixed("<#FF5555>Only players can toggle alerts."));
            return true;
        }
        if (!player.hasPermission(ViolationManager.ALERT_PERMISSION)) {
            return true;
        }
        VeritePlayer data = antiCheat.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) {
            return true;
        }
        data.alertsEnabled = !data.alertsEnabled;
        String message = data.alertsEnabled
                ? "<#FFFFFF>Alerts <#B1C7F0>enabled<#FFFFFF>."
                : "<#FFFFFF>Alerts <#FF5555>disabled<#FFFFFF>.";
        player.sendMessage(antiCheat.messages().prefixed(message));
        return true;
    }
}
