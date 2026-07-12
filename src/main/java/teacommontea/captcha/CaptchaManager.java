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

import teacommontea.captcha.CaptchaCompleteEvent;
import teacommontea.captcha.CaptchaKind;
import teacommontea.captcha.CaptchaOutcome;
import teacommontea.captcha.PunishmentRequestedEvent;
import teacommontea.util.Messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CaptchaManager {

    public static final String BYPASS_PERMISSION = "verite.bypass";

    public static final String NOTIFY_PERMISSION = "veritesauver.notify.captcha";

    private static final long COOLDOWN_MS = 30_000L;
    private static final long WARN_COOLDOWN_MS = 3_000L;
    private static final long ESCALATION_WINDOW_MS = 300_000L;
    private static final long WARN_WINDOW_MS = 60_000L;
    private static final int WARN_ESCALATE_AFTER = 3;
    private static final double WARN_PUSH_HORIZONTAL = 1.0;
    private static final double WARN_PUSH_VERTICAL = 0.2;

    private static volatile CaptchaManager instance;

    private final JavaPlugin plugin;
    private final Messages messages;
    private final CaptchaStandard standard;
    private final CaptchaMap detailed;

    private final LinkedHashMap<String, Tier> tiers = new LinkedHashMap<>();
    private final Map<UUID, Long> lastChallenge = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastWarn = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPass = new ConcurrentHashMap<>();
    private final Map<UUID, Long> warnWindowStart = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> warnCount = new ConcurrentHashMap<>();

    public CaptchaManager(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.standard = new CaptchaStandard(plugin, messages, this);
        this.detailed = new CaptchaMap(plugin, messages, this);
        loadTiers();
        instance = this;
    }

    public static CaptchaManager instance() {
        return instance;
    }

    public void shutdown() {
        if (instance == this) {
            instance = null;
        }
    }

    public CaptchaStandard standardListener() {
        return standard;
    }

    public CaptchaMap detailedListener() {
        return detailed;
    }

    public boolean challenge(Player player, CaptchaKind kind, String source) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return false;
        }
        UUID u = player.getUniqueId();
        if (standard.isActive(u) || detailed.isActive(u)) {
            return false;
        }
        String evidence = source == null ? "api" : source;
        if (kind == CaptchaKind.DETAILED) {
            detailed.open(player, "manual", evidence, 0.0);
        } else {
            standard.open(player, "manual", evidence, 0.0);
        }
        return true;
    }

    public boolean isActive(UUID player) {
        return standard.isActive(player) || detailed.isActive(player);
    }

    public CaptchaStandard standard() {
        return standard;
    }

    public CaptchaMap detailed() {
        return detailed;
    }

    private void loadTiers() {
        tiers.clear();
        put(Tier.DETAILED, "EntitySpoof", "Spoofer", "VoidBearer", "VClip",
                "GroundSpoof", "NoFall", "Phase");
        put(Tier.STANDARD, "AutoClicker", "AutoFarm", "AutoFish", "FastPlace", "Timer",
                "Baritone", "AutoWalk", "KillAura", "Reach", "Hitbox", "Nuker", "Criticals", "Velocity",
                "AutoBlock", "FastBow", "Inventory", "FastBreak", "FastUse", "GuiMove",
                "GuiInteract", "ChunkOverloader");
    }

    private void put(Tier tier, String... prefixes) {
        for (String prefix : prefixes) {
            tiers.put(prefix, tier);
        }
    }

    public Tier tierFor(String checkName) {
        Tier best = Tier.WARN;
        int bestLen = -1;
        for (Map.Entry<String, Tier> e : tiers.entrySet()) {
            if (checkName.startsWith(e.getKey()) && e.getKey().length() > bestLen) {
                best = e.getValue();
                bestLen = e.getKey().length();
            }
        }
        return best;
    }

    public void request(Player player, String check, String evidence, double score) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Tier tier = tierFor(check);
        UUID u = player.getUniqueId();
        long now = System.currentTimeMillis();

        trace("request-enter", player, check, fields(
                "tier", tier.name(), "score", score, "bypass", player.hasPermission(BYPASS_PERMISSION)));

        if (player.hasPermission(BYPASS_PERMISSION)) {
            log(player, check, evidence, score, tier, "skip-bypass");
            trace("skip-bypass", player, check, fields("tier", tier.name()));
            return;
        }

        boolean stdActive = standard.isActive(u);
        boolean detActive = detailed.isActive(u);
        if (stdActive || detActive) {
            log(player, check, evidence, score, tier, "skip-active");
            trace("skip-active", player, check, fields("standardActive", stdActive, "detailedActive", detActive));
            return;
        }

        if (tier == Tier.WARN) {
            Long windowStart = warnWindowStart.get(u);
            int count;
            if (windowStart == null || now - windowStart >= WARN_WINDOW_MS) {
                warnWindowStart.put(u, now);
                count = 1;
            } else {
                count = warnCount.getOrDefault(u, 0) + 1;
            }
            warnCount.put(u, count);

            if (count >= WARN_ESCALATE_AFTER) {
                warnWindowStart.remove(u);
                warnCount.remove(u);
                tier = Tier.STANDARD;
                log(player, check, evidence, score, tier, "warn-escalated");
                trace("warn-escalated", player, check, fields("warnCount", count, "escalateAfter", WARN_ESCALATE_AFTER));
            } else {
                Long lastWarnAt = lastWarn.get(u);
                long sinceWarn = lastWarnAt == null ? -1 : now - lastWarnAt;
                if (lastWarnAt != null && sinceWarn < WARN_COOLDOWN_MS) {
                    trace("warn-cooldown", player, check, fields(
                            "warnCount", count, "msSinceWarn", sinceWarn, "warnCooldownMs", WARN_COOLDOWN_MS));
                    return;
                }
                lastWarn.put(u, now);
                log(player, check, evidence, score, tier, "warn-push");
                trace("warn-push", player, check, fields(
                        "warnCount", count, "escalateAfter", WARN_ESCALATE_AFTER, "msSinceWarn", sinceWarn));
                runMain(() -> {
                    if (player.isOnline()) {
                        warnPush(player);
                    }
                });
                return;
            }
        }

        boolean escalated = false;
        if (tier == Tier.STANDARD) {
            Long passedAt = lastPass.get(u);
            if (passedAt != null && now - passedAt < ESCALATION_WINDOW_MS) {
                tier = Tier.DETAILED;
                escalated = true;
            }
        }

        Long last = lastChallenge.get(u);
        long sinceChallenge = last == null ? -1 : now - last;
        if (last != null && sinceChallenge < COOLDOWN_MS) {
            log(player, check, evidence, score, tier, "skip-cooldown");
            trace("skip-cooldown", player, check, fields(
                    "tier", tier.name(), "msSinceChallenge", sinceChallenge, "cooldownMs", COOLDOWN_MS));
            return;
        }
        lastChallenge.put(u, now);

        if (tier == Tier.DETAILED) {
            log(player, check, evidence, score, tier, escalated ? "detailed-opened-escalated" : "detailed-opened");
            trace("open-detailed", player, check, fields("escalated", escalated, "score", score));
            final String fCheck = check;
            final String fEvidence = evidence;
            runMain(() -> {
                if (player.isOnline()) {
                    try {
                        detailed.open(player, fCheck, fEvidence, score);
                        trace("open-detailed-done", player, fCheck, null);
                    } catch (Throwable t) {
                        trace("open-detailed-threw", player, fCheck, fields("error", String.valueOf(t)));
                    }
                } else {
                    trace("open-detailed-offline", player, fCheck, null);
                }
            });
        } else {
            log(player, check, evidence, score, tier, "standard-opened");
            trace("open-standard", player, check, fields("score", score));
            final String fCheck = check;
            final String fEvidence = evidence;
            runMain(() -> {
                if (player.isOnline()) {
                    try {
                        standard.open(player, fCheck, fEvidence, score);
                        trace("open-standard-done", player, fCheck, null);
                    } catch (Throwable t) {
                        trace("open-standard-threw", player, fCheck, fields("error", String.valueOf(t)));
                    }
                } else {
                    trace("open-standard-offline", player, fCheck, null);
                }
            });
        }
    }

    private void trace(String stage, Player player, String check, java.util.Map<String, Object> extra) {
    }

    private static java.util.Map<String, Object> fields(Object... kv) {
        return new java.util.HashMap<>();
    }

    private void runMain(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private void log(Player player, String check, String evidence, double score, Tier tier, String action) {
    }

    private void warnPush(Player player) {
        Vector dir = player.getLocation().getDirection();
        dir.setY(0.0);
        if (dir.lengthSquared() < 1.0e-4) {
            return;
        }
        dir.normalize().multiply(WARN_PUSH_HORIZONTAL);
        dir.setY(WARN_PUSH_VERTICAL);
        player.setVelocity(dir);
    }

    void resolved(Player player, CaptchaKind kind, CaptchaOutcome outcome, long durationMs,
                  List<String> failedChoices, String check, String evidence, double confidence) {
        boolean punish = outcome != CaptchaOutcome.PASS;
        if (!punish) {
            lastPass.put(player.getUniqueId(), System.currentTimeMillis());
        }
        CaptchaCompleteEvent complete = new CaptchaCompleteEvent(
                player, kind, outcome, durationMs, failedChoices, punish);
        Bukkit.getPluginManager().callEvent(complete);

        announce(player, kind, outcome, durationMs, failedChoices.size());

        if (!punish) {
            return;
        }
        PunishmentRequestedEvent.Cause cause = outcome == CaptchaOutcome.TIMEOUT
                ? PunishmentRequestedEvent.Cause.CAPTCHA_TIMEOUT
                : PunishmentRequestedEvent.Cause.CAPTCHA_FAILED;
        String kickMessage = "<red>Failed verification.";
        PunishmentRequestedEvent request = new PunishmentRequestedEvent(
                player, cause, check, evidence, confidence, kickMessage);
        Bukkit.getPluginManager().callEvent(request);

        if (request.isCancelled() || !player.isOnline()) {
            return;
        }
        player.kick(messages.parse(request.getKickMessage()));
    }

    private void announce(Player player, CaptchaKind kind, CaptchaOutcome outcome,
                          long durationMs, int fails) {
        String word = switch (outcome) {
            case PASS -> "<#B1C7F0>passed";
            case FAIL -> "<red>failed";
            case TIMEOUT -> "<red>timed out on";
        };
        String body = "<white>" + player.getName() + " <white>" + word + " <white>the "
                + kind.name().toLowerCase() + " captcha <white>(<white>"
                + formatTime(durationMs) + "<white>, <white>" + fails + " <white>wrong).";
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission(NOTIFY_PERMISSION)) {
                continue;
            }
            staff.sendMessage(messages.prefixed(body));
        }
    }

    static String formatTime(long millis) {
        long secs = millis / 1000;
        long tenths = (millis - secs * 1000) / 100;
        return secs + "." + tenths + "s";
    }
}
