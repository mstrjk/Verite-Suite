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

package teacommontea.veritechasse.check.impl.hackedclient;

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.Check;
import teacommontea.veritechasse.check.CheckInfo;
import teacommontea.veritechasse.check.PacketCheck;
import teacommontea.veritechasse.player.VeritePlayer;

import java.nio.charset.StandardCharsets;

@CheckInfo(name = "HackedClientA", description = "Cheat channel or unrecognized client brand.")
public final class HackedClientA extends Check implements PacketCheck {

    private static final String[] CHEAT_CHANNELS = {
        "baritone", "wurst", "meteor", "impact", "aristois", "lambda",
        "liquidbounce", "future", "rusherhack", "seppuku", "kami",
        "salhack", "aresclient", "pyro", "konas"
    };

    private static final String[] BEDROCK_MARKERS = {
        "geyser", "floodgate", "bedrock"
    };

    private static final String[] KNOWN_CLIENTS = {
        "vanilla", "fabric", "forge", "neoforge", "optifine", "lunarclient", "lunar",
        "badlion", "feather", "labymod", "pojav", "legacyfabric", "quilt", "sodium",
        "iris", "modloader", "rift"
    };

    private boolean brandChecked;

    public HackedClientA(VeritePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final VeritePacketEvent event) {
        if (event.getPacketType() != VeritePacketType.PLUGIN_MESSAGE) return;

        String channel = event.getChannelName();
        if (channel == null) return;
        String lower = channel.toLowerCase();

        for (String marker : CHEAT_CHANNELS) {
            if (lower.contains(marker)) {
                flag("channel=" + channel);
                return;
            }
        }

        if (!lower.equals("minecraft:brand") && !lower.equals("brand")) return;
        if (brandChecked) return;
        brandChecked = true;

        byte[] data = event.getData();
        if (data == null || data.length == 0) return;
        String brand = sanitize(new String(data, StandardCharsets.UTF_8)).toLowerCase().trim();
        player.clientBrand = brand;

        for (String b : BEDROCK_MARKERS) {
            if (brand.contains(b)) {
                player.bedrockClient = true;
                player.knownClient = true;
                return;
            }
        }
        for (String k : KNOWN_CLIENTS) {
            if (brand.contains(k)) {
                player.knownClient = true;
                return;
            }
        }

        flag("unrecognized brand=" + (brand.isEmpty() ? "<empty>" : brand));
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 0x20 && c < 0x7F) sb.append(c);
        }
        return sb.toString();
    }
}
