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

package teacommontea.veritechasse;

import teacommontea.veritechasse.check.CheckSettings;
import teacommontea.veritechasse.manager.DetectionLog;
import teacommontea.veritechasse.manager.PlayerDataManager;
import teacommontea.veritechasse.manager.TickManager;
import teacommontea.veritechasse.manager.VeriteTrace;
import teacommontea.veritechasse.manager.ViolationManager;
import teacommontea.veritechasse.punish.CaptchaManager;
import teacommontea.veritechasse.replay.ReplayManager;
import teacommontea.veritechasse.listener.PacketListener;
import teacommontea.veritechasse.net.PacketInjector;
import teacommontea.veritechasse.util.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiCheat {

    private final JavaPlugin plugin;
    private final TickManager tickManager = new TickManager();
    private final ViolationManager violationManager = new ViolationManager(this);
    private final PlayerDataManager playerDataManager = new PlayerDataManager(this);
    private Messages messages;
    private DetectionLog detectionLog;
    private VeriteTrace trace;
    private CaptchaManager captchaManager;
    private ReplayManager replayManager;
    private CheckSettings checkSettings;
    private final PacketInjector injector;

    public AntiCheat(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadMessages();
        this.checkSettings = new CheckSettings(plugin);
        this.detectionLog = new DetectionLog(this);
        this.trace = new VeriteTrace(this);
        this.captchaManager = new CaptchaManager(this);
        this.replayManager = new ReplayManager(this);

        PacketListener pl = new PacketListener(this);
        this.injector = new PacketInjector(pl::onPacketReceive, pl::onPacketSend);
    }

    public PacketInjector getInjector() {
        return injector;
    }

    public DetectionLog getDetectionLog() {
        return detectionLog;
    }

    public VeriteTrace getTrace() {
        return trace;
    }

    public ReplayManager getReplayManager() {
        return replayManager;
    }

    public CaptchaManager getCaptchaManager() {
        return captchaManager;
    }

    public void reloadMessages() {

        this.messages = new Messages();
    }

    public CheckSettings checkSettings() {
        return checkSettings;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Messages messages() {
        return messages;
    }

    public TickManager getTickManager() {
        return tickManager;
    }

    public ViolationManager getViolationManager() {
        return violationManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
