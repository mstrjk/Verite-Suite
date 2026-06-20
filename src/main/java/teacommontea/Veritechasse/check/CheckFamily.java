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

package teacommontea.veritechasse.check;

import teacommontea.veritechasse.player.VeritePlayer;

public final class CheckFamily {

    private final VeritePlayer player;
    private final String name;
    private final ConfidenceScorer scorer;
    private final boolean enabled;

    private double violations;

    public CheckFamily(VeritePlayer player, String name, ConfidenceCheck... checks) {
        this.player = player;
        this.name = name;
        this.scorer = new ConfidenceScorer(checks);

        this.enabled = player.getAntiCheat().checkSettings().familyEnabled(name);
    }

    private static final double ALERT_CONFIDENCE_FLOOR = 33.33;

    public void evaluate() {
        teacommontea.veritechasse.manager.VeriteTrace trace = player.getAntiCheat().getTrace();
        org.bukkit.entity.Player pb = org.bukkit.Bukkit.getPlayer(player.getUuid());
        String pname = pb == null ? player.getUuid().toString() : pb.getName();
        if (!enabled) {
            if (trace.isEnabled()) {
                trace.event("family-disabled", player.getUuid(), pname,
                        teacommontea.veritechasse.manager.VeriteTrace.fields("family", name));
            }
            return;
        }
        boolean punish = scorer.evaluate();
        double score = scorer.currentScore();
        boolean lowConfidence = scorer.isLowConfidence();

        if (lowConfidence) {
            violations++;
        } else {
            violations = Math.max(0.0, violations - 0.5);
        }

        boolean passedGate = punish && score < ALERT_CONFIDENCE_FLOOR;

        if (trace.isEnabled() && (punish || lowConfidence)) {
            Check w = scorer.lowestCheck();
            trace.event("family-eval", player.getUuid(), pname,
                    teacommontea.veritechasse.manager.VeriteTrace.fields(
                            "family", name,
                            "punish", punish,
                            "lowConfidence", lowConfidence,
                            "score", score,
                            "floor", ALERT_CONFIDENCE_FLOOR,
                            "passedGate", passedGate,
                            "violations", violations,
                            "worst", w == null ? null : w.getName()));
        }

        if (passedGate) {
            Check worst = scorer.lowestCheck();
            String checkName = worst != null ? worst.getName() : name;
            String description = worst != null ? worst.getDescription() : "";
            String info = worst != null ? worst.getInfo() : "";
            player.getAntiCheat().getViolationManager().handleFamily(
                    player, checkName, description, info, score);
            player.getAntiCheat().getReplayManager().arm(player, checkName, info);
            org.bukkit.entity.Player bukkit = pb;
            if (trace.isEnabled()) {
                trace.event("family-request", player.getUuid(), pname,
                        teacommontea.veritechasse.manager.VeriteTrace.fields(
                                "family", name, "check", checkName, "score", score,
                                "bukkitOnline", bukkit != null));
            }
            if (bukkit != null) {
                player.getAntiCheat().getCaptchaManager().request(
                        bukkit, checkName, info, score);
            }
        }
    }

    public String name() {
        return name;
    }

    public double violations() {
        return violations;
    }

    public double score() {
        return scorer.currentScore();
    }
}
