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

package teacommontea.captcha.api;

import teacommontea.captcha.CaptchaKind;
import teacommontea.captcha.CaptchaManager;

import org.bukkit.entity.Player;

import java.util.UUID;

public final class CaptchaAPI {

    private CaptchaAPI() {}

    private static CaptchaManager captcha() {
        return CaptchaManager.instance();
    }

    public static boolean enabled() {
        return captcha() != null;
    }

    public static boolean challengeStandard(Player player, String source) {
        return enabled() && captcha().challenge(player, CaptchaKind.STANDARD, source);
    }

    public static boolean challengeDetailed(Player player, String source) {
        return enabled() && captcha().challenge(player, CaptchaKind.DETAILED, source);
    }

    public static boolean challenge(Player player, CaptchaKind kind, String source) {
        return enabled() && captcha().challenge(player, kind, source);
    }

    public static boolean isActive(UUID player) {
        return enabled() && captcha().isActive(player);
    }
}
