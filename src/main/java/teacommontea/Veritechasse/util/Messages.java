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

package teacommontea.veritechasse.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static final String DEFAULT_PREFIX = "<#555555>[<#B1C7F0>Verité<#555555>]<reset>";

    private static volatile String prefix = DEFAULT_PREFIX;

    public static void setPrefix(String p) {
        prefix = (p == null || p.isBlank()) ? DEFAULT_PREFIX : p;
    }

    public static String prefix() {
        return prefix;
    }

    public Messages(String ignored) {}
    public Messages() {}

    public Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    public Component prefixed(String miniMessage) {
        return MM.deserialize(prefix + " " + miniMessage);
    }
}
