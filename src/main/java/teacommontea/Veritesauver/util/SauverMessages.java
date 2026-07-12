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

package teacommontea.veritesauver.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import teacommontea.util.Messages;

public final class SauverMessages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Messages messages = new Messages();

    public SauverMessages() {}

    public void send(CommandSender to, String miniMessage) {
        to.sendMessage(messages.prefixed(miniMessage).decoration(TextDecoration.ITALIC, false));
    }

    public void info(CommandSender to, String miniMessage) {
        send(to, miniMessage);
    }

    public void err(CommandSender to, String miniMessage) {
        send(to, miniMessage);
    }

    public void raw(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false));
    }

    public static Component screen(String miniMessage) {
        return MM.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false);
    }

    public Messages messages() {
        return messages;
    }
}
