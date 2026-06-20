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
import org.bukkit.entity.Player;

import java.util.UUID;

public final class SauverExempt {

    private SauverExempt() {}

    public static String blockReason(org.bukkit.command.CommandSender issuer, UUID targetUuid, Entry.Type type) {
        Player target = targetUuid == null ? null : Bukkit.getPlayer(targetUuid);
        if (target == null) {
            return null;
        }
        boolean issuerConsole = !(issuer instanceof Player);
        String t = type.id();

        if (!issuerConsole && isPermissionExempt(target, t) && !hasBypass(issuer, t)) {
            return target.getName() + " is exempt from being " + pastTense(type) + ".";
        }
        if (SauverConfig.useGroupWeights() && issuer instanceof Player p && !outranks(p, target)) {
            return "You cannot " + type.id() + " a player of equal or higher rank.";
        }
        return null;
    }

    private static boolean isPermissionExempt(Player target, String type) {
        return target.hasPermission("veritesauver.exempt")
                || target.hasPermission("veritesauver.exempt." + type);
    }

    private static boolean hasBypass(org.bukkit.command.CommandSender issuer, String type) {
        return issuer.hasPermission("veritesauver.exempt.bypass")
                || issuer.hasPermission("veritesauver.exempt.bypass." + type);
    }

    private static boolean outranks(Player issuer, Player target) {
        int iw = weight(issuer);
        int tw = weight(target);
        if (iw < 0 || tw < 0) {
            return true;
        }
        return SauverConfig.permitSameWeight() ? iw >= tw : iw > tw;
    }

    private static int weight(Player p) {
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = provider.getMethod("get").invoke(null);
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, p.getUniqueId());
            if (user == null) {
                return -1;
            }
            String primary = (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
            Object groupManager = api.getClass().getMethod("getGroupManager").invoke(api);
            Object group = groupManager.getClass().getMethod("getGroup", String.class).invoke(groupManager, primary);
            if (group == null) {
                return -1;
            }
            Object weightOpt = group.getClass().getMethod("getWeight").invoke(group);
            Object w = weightOpt.getClass().getMethod("orElse", Object.class).invoke(weightOpt, 0);
            return w instanceof Integer i ? i : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String pastTense(Entry.Type type) {
        return switch (type) {
            case BAN -> "banned";
            case MUTE -> "muted";
            case WARNING -> "warned";
            case KICK -> "kicked";
        };
    }
}
