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

package teacommontea.veritechasse.manager;

import net.kyori.adventure.text.Component;
import teacommontea.veritechasse.AntiCheat;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.player.VeritePlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ViolationManager {

    public static final String ALERT_PERMISSION = "verite.alerts";

    private static final String DEFAULT_FORMAT =
            "<white>%player% <#FF5555>failed <#B1C7F0>%check%<white>.";
    private static final String DEFAULT_CONSOLE =
            "[Verité] %player% failed %check%";
    private static final long ALERT_THROTTLE_MS = 1500L;

    private final AntiCheat antiCheat;
    private final Map<String, Long> lastAlert = new HashMap<>();

    public ViolationManager(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    public void handle(Check check, String reason) {
        Player bukkit = Bukkit.getPlayer(check.getPlayer().getUuid());
        if (bukkit == null) return;
        antiCheat.getDetectionLog().record(check.getPlayer().getUuid(), bukkit.getName(),
                "FLAG", check.getName(), reason, -1.0, "", "alert");
        alert(bukkit, check.getPlayer().getUuid(), check.getName(), check.getDescription(), reason);
    }

    public void handleFamily(VeritePlayer player, String checkName, String description,
                             String info, double confidenceScore) {
        Player bukkit = Bukkit.getPlayer(player.getUuid());
        if (bukkit == null) return;
        antiCheat.getDetectionLog().record(player.getUuid(), bukkit.getName(),
                "FAMILY", checkName, info, confidenceScore, "", "alert");
        alert(bukkit, player.getUuid(), checkName, description, info);
    }

    private void alert(Player bukkit, UUID uuid, String label, String description, String info) {
        String throttleKey = uuid + ":" + label;
        long now = System.currentTimeMillis();
        Long last = lastAlert.get(throttleKey);
        if (last != null && now - last < ALERT_THROTTLE_MS) return;
        lastAlert.put(throttleKey, now);

        org.bukkit.configuration.file.YamlConfiguration cfg = teacommontea.veritesauver.SauverConfig.yaml();

        String body = apply(cfg.getString("alerts.format", DEFAULT_FORMAT), bukkit, label, info);
        Component component = antiCheat.messages().prefixed(body)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        buildHover(bukkit, label, description, info)));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission(ALERT_PERMISSION)) continue;
            VeritePlayer staffData = antiCheat.getPlayerDataManager().get(staff.getUniqueId());
            if (staffData == null || staffData.alertsEnabled) {
                staff.sendMessage(component);
            }
        }

        if (cfg.getBoolean("alerts.print-to-console", true)) {
            String console = apply(cfg.getString("alerts.console-format", DEFAULT_CONSOLE), bukkit, label, info);
            Bukkit.getConsoleSender().sendMessage(antiCheat.messages().parse(console));
        }
    }

    private Component buildHover(Player p, String check, String description, String info) {
        VeritePlayer data = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        String brand = data == null || data.clientBrand.isEmpty() ? "vanilla" : data.clientBrand;
        org.bukkit.Location loc = p.getLocation();

        StringBuilder sb = new StringBuilder();
        sb.append("<#B1C7F0>").append(check).append("<white>: <white>").append(description).append("\n");
        if (info != null && !info.isEmpty()) {
            sb.append("\n<white>Evidence: <white>").append(info).append("\n");
        }
        sb.append("\n<white>Ping: <white>").append(p.getPing()).append("ms")
                .append("  <white>TPS: <white>").append(String.format("%.1f", Bukkit.getTPS()[0]))
                .append("\n<white>Client: <white>").append(brand)
                .append("\n<white>Gamemode: <white>").append(p.getGameMode().name().toLowerCase())
                .append("  <white>Ground: <white>").append(data != null && data.snapshot.valid && data.snapshot.bukkitOnGround)
                .append("\n<white>Pos: <white>").append(loc.getBlockX()).append(", ")
                .append(loc.getBlockY()).append(", ").append(loc.getBlockZ())
                .append(" <white>(").append(loc.getWorld().getName()).append(")");
        return antiCheat.messages().parse(sb.toString());
    }

    private String apply(String template, Player p, String check, String info) {
        VeritePlayer data = antiCheat.getPlayerDataManager().get(p.getUniqueId());
        String brand = data == null || data.clientBrand.isEmpty() ? "vanilla" : data.clientBrand;
        return template
                .replace("%player%", p.getName())
                .replace("%check%", check)
                .replace("%info%", info == null ? "" : info)
                .replace("%ping%", String.valueOf(p.getPing()))
                .replace("%tps%", String.format("%.1f", Bukkit.getTPS()[0]))
                .replace("%client-brand%", brand)
                .replace("%x%", String.valueOf(p.getLocation().getBlockX()))
                .replace("%y%", String.valueOf(p.getLocation().getBlockY()))
                .replace("%z%", String.valueOf(p.getLocation().getBlockZ()))
                .replace("%world%", p.getWorld().getName());
    }
}
