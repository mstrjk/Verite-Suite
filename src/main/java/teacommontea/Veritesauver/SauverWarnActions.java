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

import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

public final class SauverWarnActions {

    private record Step(int count, List<String> commands) {}

    private static final List<Step> LADDER = List.of(
        new Step(3, List.of("tempmute $player 1d Reached 3 warnings")),
        new Step(5, List.of("tempban $player 1d Reached 5 warnings"))
    );

    private SauverWarnActions() {}

    static void onWarn(UUID targetUuid, String targetName, int activeCount) {
        for (Step step : LADDER) {
            if (step.count() == activeCount) {
                runStep(step, targetName);
            }
        }
    }

    private static void runStep(Step step, String targetName) {
        for (String raw : step.commands()) {
            String cmd = raw.replace("$player", targetName);
            Bukkit.getScheduler().runTask(Sauver.instance().plugin(), () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("veritesauver.notify.broadcast")) {
                Sauver.instance().messages().send(p, "<#FFFFFF>Auto-action: <#FFFFFF>" + targetName
                        + " <#FFFFFF>hit <#FF5555>" + step.count() + " <#FFFFFF>warnings.");
            }
        }
    }
}
