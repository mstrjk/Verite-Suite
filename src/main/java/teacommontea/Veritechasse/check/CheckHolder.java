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

import teacommontea.veritechasse.net.VeritePacketEvent;
import teacommontea.veritechasse.net.VeritePacketType;
import teacommontea.veritechasse.check.impl.AutoClickerA;
import teacommontea.veritechasse.check.impl.AutoClickerB;
import teacommontea.veritechasse.check.impl.AutoClickerC;
import teacommontea.veritechasse.check.impl.AutoClickerD;
import teacommontea.veritechasse.check.impl.FastBowA;
import teacommontea.veritechasse.check.impl.FastBowB;
import teacommontea.veritechasse.check.impl.FastPlaceA;
import teacommontea.veritechasse.check.impl.FastPlaceB;
import teacommontea.veritechasse.check.impl.FastPlaceC;
import teacommontea.veritechasse.check.impl.FastPlaceD;
import teacommontea.veritechasse.check.impl.FastPlaceE;
import teacommontea.veritechasse.check.impl.FlightA;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldA;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldB;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldC;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldD;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldE;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldF;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldG;
import teacommontea.veritechasse.check.impl.scaffold.ScaffoldH;
import teacommontea.veritechasse.check.impl.airplace.AirPlaceA;
import teacommontea.veritechasse.check.impl.airplace.AirPlaceB;
import teacommontea.veritechasse.check.impl.badpackets.BadPacketInvalidPitch;
import teacommontea.veritechasse.check.impl.badpackets.BadPacketInvalidPosition;
import teacommontea.veritechasse.check.impl.badpackets.BadPacketInvalidRotation;
import teacommontea.veritechasse.check.impl.badpackets.BadPacketOffhandCrash;
import teacommontea.veritechasse.check.impl.autoblock.AutoBlockA;
import teacommontea.veritechasse.check.impl.criticals.CriticalsA;
import teacommontea.veritechasse.check.impl.criticals.CriticalsB;
import teacommontea.veritechasse.check.impl.fastbreak.FastBreakA;
import teacommontea.veritechasse.check.impl.fastuse.FastUseA;
import teacommontea.veritechasse.check.impl.groundspoof.GroundSpoofA;
import teacommontea.veritechasse.check.impl.chunkoverloader.ChunkOverloaderA;
import teacommontea.veritechasse.check.impl.autofish.AutoFishA;
import teacommontea.veritechasse.check.impl.autowalk.AutoWalkA;
import teacommontea.veritechasse.check.impl.baritone.BaritoneA;
import teacommontea.veritechasse.check.impl.nofall.NoFallA;
import teacommontea.veritechasse.check.impl.phase.PhaseA;
import teacommontea.veritechasse.check.impl.blink.BlinkA;
import teacommontea.veritechasse.check.impl.elytrafly.ElytraFlyA;
import teacommontea.veritechasse.check.impl.inventory.InventoryA;
import teacommontea.veritechasse.check.impl.strafe.StrafeA;
import teacommontea.veritechasse.check.impl.hackedclient.HackedClientA;
import teacommontea.veritechasse.check.impl.length.LengthA;
import teacommontea.veritechasse.check.impl.voidbearer.VoidBearerA;
import teacommontea.veritechasse.check.impl.hitbox.HitboxA;
import teacommontea.veritechasse.check.impl.autofarm.AutoFarmA;
import teacommontea.veritechasse.check.impl.autofarm.AutoFarmB;
import teacommontea.veritechasse.check.impl.autofarm.InteractionSampler;
import teacommontea.veritechasse.check.impl.nuker.NukerA;
import teacommontea.veritechasse.check.impl.nuker.NukerB;
import teacommontea.veritechasse.check.impl.motion.MotionA;
import teacommontea.veritechasse.check.impl.motion.MotionB;
import teacommontea.veritechasse.check.impl.motion.MotionC;
import teacommontea.veritechasse.check.impl.motion.MotionD;
import teacommontea.veritechasse.check.impl.killaura.KillAuraA;
import teacommontea.veritechasse.check.impl.killaura.KillAuraB;
import teacommontea.veritechasse.check.impl.killaura.KillAuraC;
import teacommontea.veritechasse.check.impl.killaura.KillAuraD;
import teacommontea.veritechasse.check.impl.killaura.KillAuraE;
import teacommontea.veritechasse.check.impl.reach.ReachA;
import teacommontea.veritechasse.check.impl.reach.ReachB;
import teacommontea.veritechasse.check.impl.spoofer.SpooferA;
import teacommontea.veritechasse.check.impl.sprint.SprintA;
import teacommontea.veritechasse.check.impl.step.AntiLevitationA;
import teacommontea.veritechasse.check.impl.step.FastClimbA;
import teacommontea.veritechasse.check.impl.step.HighJumpA;
import teacommontea.veritechasse.check.impl.step.HighJumpB;
import teacommontea.veritechasse.check.impl.step.StepA;
import teacommontea.veritechasse.check.impl.step.WallClimbA;
import teacommontea.veritechasse.check.impl.vclip.VClipA;
import teacommontea.veritechasse.check.impl.velocity.VelocityA;
import teacommontea.veritechasse.check.impl.velocity.VelocityB;
import teacommontea.veritechasse.check.impl.boatfly.BoatFlyA;
import teacommontea.veritechasse.check.impl.boatfly.BoatFlyB;
import teacommontea.veritechasse.check.impl.boatfly.BoatFlyC;
import teacommontea.veritechasse.check.impl.entityspoof.EntitySpoofA;
import teacommontea.veritechasse.check.impl.entityspoof.EntitySpoofB;
import teacommontea.veritechasse.check.impl.entityspoof.EntitySpoofC;
import teacommontea.veritechasse.check.impl.entityspoof.EntitySpoofD;
import teacommontea.veritechasse.check.impl.entityspoof.EntitySpoofE;
import teacommontea.veritechasse.check.impl.entityspoof.EntitySpoofF;
import teacommontea.veritechasse.check.impl.entityspoof.EntitySpoofG;
import teacommontea.veritechasse.check.impl.noslow.NoSlowA;
import teacommontea.veritechasse.check.impl.noslow.NoSlowB;
import teacommontea.veritechasse.check.impl.gui.GuiMoveA;
import teacommontea.veritechasse.check.impl.JesusA;
import teacommontea.veritechasse.check.impl.JesusB;
import teacommontea.veritechasse.check.impl.JesusC;
import teacommontea.veritechasse.check.impl.SpeedA;
import teacommontea.veritechasse.check.impl.SpeedB;
import teacommontea.veritechasse.check.impl.SpeedC;
import teacommontea.veritechasse.check.impl.SpeedD;
import teacommontea.veritechasse.check.impl.SpeedE;
import teacommontea.veritechasse.check.impl.SpeedF;
import teacommontea.veritechasse.check.impl.SpeedG;
import teacommontea.veritechasse.check.impl.SpeedH;
import teacommontea.veritechasse.check.impl.TimerA;
import teacommontea.veritechasse.check.impl.TimerB;
import teacommontea.veritechasse.check.impl.TimerC;
import teacommontea.veritechasse.check.impl.TimerD;
import teacommontea.veritechasse.check.impl.TimerE;
import teacommontea.veritechasse.check.impl.TowerA;
import teacommontea.veritechasse.check.impl.TowerB;
import teacommontea.veritechasse.player.BlockBreak;
import teacommontea.veritechasse.player.BlockPlace;
import teacommontea.veritechasse.player.VeritePlayer;

import java.util.ArrayList;
import java.util.List;

public final class CheckHolder {

    private final VeritePlayer player;

    private final List<Check> all = new ArrayList<>();
    private final List<PacketCheck> packetChecks = new ArrayList<>();
    private final List<BlockPlaceCheck> blockPlaceChecks = new ArrayList<>();
    private final List<BlockBreakCheck> blockBreakChecks = new ArrayList<>();

    private final FlightA flightA;
    private final FastBowB fastBowB;
    private final SpeedA speedA;
    private final SpeedB speedB;
    private final SpeedC speedC;
    private final SpeedD speedD;
    private final SpeedE speedE;
    private final SpeedF speedF;
    private final SpeedG speedG;
    private final SpeedH speedH;

    private final CheckFamily speedFamily;
    private final CheckFamily jesusFamily;
    private final CheckFamily timerFamily;
    private final CheckFamily fastPlaceFamily;

    private final AirPlaceA airPlaceA;
    private final AirPlaceB airPlaceB;
    private final CheckFamily airPlaceFamily;

    private final GroundSpoofA groundSpoofA;
    private final CheckFamily groundSpoofFamily;

    private final StepA stepA;
    private final CheckFamily stepFamily;

    private final HighJumpA highJumpA;
    private final HighJumpB highJumpB;
    private final CheckFamily highJumpFamily;

    private final FastClimbA fastClimbA;
    private final CheckFamily fastClimbFamily;

    private final WallClimbA wallClimbA;
    private final CheckFamily wallClimbFamily;

    private final AntiLevitationA antiLevitationA;
    private final CheckFamily antiLevitationFamily;

    private final VelocityA velocityA;
    private final VelocityB velocityB;
    private final CheckFamily velocityFamily;

    private final CriticalsA criticalsA;
    private final CriticalsB criticalsB;
    private final CheckFamily criticalsFamily;

    private final SprintA sprintA;
    private final CheckFamily sprintFamily;

    private final AutoBlockA autoBlockA;
    private final CheckFamily autoBlockFamily;

    private final FastUseA fastUseA;
    private final CheckFamily fastUseFamily;

    private final VClipA vClipA;
    private final CheckFamily vClipFamily;

    private final ReachA reachA;
    private final ReachB reachB;
    private final CheckFamily reachFamily;

    private final MotionA motionA;
    private final MotionB motionB;
    private final MotionC motionC;
    private final MotionD motionD;
    private final CheckFamily motionAFamily;
    private final CheckFamily motionBFamily;
    private final CheckFamily motionCFamily;
    private final CheckFamily motionDFamily;

    private final HitboxA hitboxA;
    private final CheckFamily hitboxFamily;

    private final NukerA nukerA;
    private final NukerB nukerB;
    private final CheckFamily nukerFamily;

    private final InteractionSampler interactionSampler = new InteractionSampler();
    private final AutoFarmA autoFarmA;
    private final AutoFarmB autoFarmB;
    private final CheckFamily autoFarmFamily;

    private final FastBreakA fastBreakA;
    private final CheckFamily fastBreakFamily;

    private final SpooferA spooferA;
    private final CheckFamily spooferFamily;

    private final LengthA lengthA;

    private final ChunkOverloaderA chunkOverloaderA;
    private final CheckFamily chunkOverloaderFamily;

    private final VoidBearerA voidBearerA;
    private final CheckFamily voidBearerFamily;

    private final StrafeA strafeA;
    private final CheckFamily strafeFamily;

    private final ElytraFlyA elytraFlyA;
    private final CheckFamily elytraFlyFamily;

    private final BlinkA blinkA;
    private final CheckFamily blinkFamily;

    private final InventoryA inventoryA;
    private final CheckFamily inventoryFamily;

    private final BaritoneA baritoneA;
    private final CheckFamily baritoneFamily;

    private final AutoWalkA autoWalkA;
    private final CheckFamily autoWalkFamily;

    private final PhaseA phaseA;
    private final CheckFamily phaseFamily;

    private final NoFallA noFallA;
    private final CheckFamily noFallFamily;

    private final AutoFishA autoFishA;
    private final CheckFamily autoFishFamily;

    private final KillAuraA killAuraA;
    private final KillAuraB killAuraB;
    private final KillAuraC killAuraC;
    private final KillAuraD killAuraD;
    private final KillAuraE killAuraE;
    private final CheckFamily killAuraFamily;

    private final ScaffoldA scaffoldA;
    private final ScaffoldB scaffoldB;
    private final ScaffoldC scaffoldC;
    private final ScaffoldD scaffoldD;
    private final ScaffoldE scaffoldE;
    private final ScaffoldF scaffoldF;
    private final ScaffoldG scaffoldG;
    private final ScaffoldH scaffoldH;
    private final CheckFamily scaffoldFamily;

    private final BoatFlyA boatFlyA;
    private final BoatFlyB boatFlyB;
    private final BoatFlyC boatFlyC;
    private final CheckFamily boatFlyFamily;

    private final EntitySpoofA entitySpoofA;
    private final EntitySpoofB entitySpoofB;
    private final EntitySpoofC entitySpoofC;
    private final EntitySpoofD entitySpoofD;
    private final EntitySpoofE entitySpoofE;
    private final EntitySpoofF entitySpoofF;
    private final EntitySpoofG entitySpoofG;
    private final CheckFamily entitySpoofFamily;

    private final NoSlowA noSlowA;
    private final NoSlowB noSlowB;
    private final CheckFamily noSlowBlockFamily;
    private final CheckFamily noSlowUseFamily;

    private final GuiMoveA guiMoveA;
    private final CheckFamily guiFamily;
    private final CheckFamily autoClickerFamily;
    private final CheckFamily fastBowFamily;
    private final CheckFamily towerFamily;

    public CheckHolder(VeritePlayer player) {
        this.player = player;
        this.flightA = new FlightA(player);

        this.speedA = new SpeedA(player);
        this.speedB = new SpeedB(player);
        this.speedC = new SpeedC(player);
        this.speedD = new SpeedD(player);
        this.speedE = new SpeedE(player);
        this.speedF = new SpeedF(player);
        this.speedG = new SpeedG(player);
        this.speedH = new SpeedH(player);

        FastPlaceA fastPlaceA = new FastPlaceA(player);
        FastPlaceB fastPlaceB = new FastPlaceB(player);
        FastPlaceC fastPlaceC = new FastPlaceC(player);
        FastPlaceD fastPlaceD = new FastPlaceD(player);
        FastPlaceE fastPlaceE = new FastPlaceE(player);

        AutoClickerA autoClickerA = new AutoClickerA(player);
        AutoClickerB autoClickerB = new AutoClickerB(player);
        AutoClickerC autoClickerC = new AutoClickerC(player);
        AutoClickerD autoClickerD = new AutoClickerD(player);

        FastBowA fastBowA = new FastBowA(player);
        this.fastBowB = new FastBowB(player);

        TowerA towerA = new TowerA(player);
        TowerB towerB = new TowerB(player);

        JesusA jesusA = new JesusA(player);
        JesusB jesusB = new JesusB(player);
        JesusC jesusC = new JesusC(player);

        TimerA timerA = new TimerA(player);
        TimerB timerB = new TimerB(player);
        TimerC timerC = new TimerC(player);
        TimerD timerD = new TimerD(player);
        TimerE timerE = new TimerE(player);

        for (Check c : new Check[]{flightA, speedA, speedB, speedC, speedD, speedE, speedF, speedG, speedH,
                fastPlaceA, fastPlaceB, fastPlaceC, fastPlaceD, fastPlaceE,
                autoClickerA, autoClickerB, autoClickerC, autoClickerD, fastBowA, fastBowB, towerA,
                jesusA, jesusB, jesusC, timerA, timerB, timerC, timerD, timerE, towerB}) {
            register(c);
        }

        this.speedFamily = new CheckFamily(player, "Speed", speedA, speedB, speedC, speedD, speedE, speedF, speedG, speedH);
        this.jesusFamily = new CheckFamily(player, "Jesus", jesusA, jesusB, jesusC);
        this.timerFamily = new CheckFamily(player, "Timer", timerA, timerB, timerC, timerD, timerE);

        this.airPlaceA = new AirPlaceA(player);
        this.airPlaceB = new AirPlaceB(player);
        register(airPlaceA);
        register(airPlaceB);
        this.airPlaceFamily = new CheckFamily(player, "AirPlace", airPlaceA, airPlaceB);

        register(new BadPacketInvalidPitch(player));
        register(new BadPacketOffhandCrash(player));
        register(new BadPacketInvalidPosition(player));
        register(new BadPacketInvalidRotation(player));
        register(new HackedClientA(player));

        this.groundSpoofA = new GroundSpoofA(player);
        register(groundSpoofA);
        this.groundSpoofFamily = new CheckFamily(player, "GroundSpoof", groundSpoofA);

        this.stepA = new StepA(player);
        register(stepA);
        this.stepFamily = new CheckFamily(player, "Step", stepA);

        this.highJumpA = new HighJumpA(player);
        this.highJumpB = new HighJumpB(player);
        register(highJumpA);
        register(highJumpB);
        this.highJumpFamily = new CheckFamily(player, "HighJump", highJumpA, highJumpB);

        this.fastClimbA = new FastClimbA(player);
        register(fastClimbA);
        this.fastClimbFamily = new CheckFamily(player, "FastClimb", fastClimbA);

        this.wallClimbA = new WallClimbA(player);
        register(wallClimbA);
        this.wallClimbFamily = new CheckFamily(player, "WallClimb", wallClimbA);

        this.antiLevitationA = new AntiLevitationA(player);
        register(antiLevitationA);
        this.antiLevitationFamily = new CheckFamily(player, "AntiLevitation", antiLevitationA);

        this.velocityA = new VelocityA(player);
        this.velocityB = new VelocityB(player);
        register(velocityA);
        register(velocityB);
        this.velocityFamily = new CheckFamily(player, "Velocity", velocityA, velocityB);

        this.criticalsA = new CriticalsA(player);
        this.criticalsB = new CriticalsB(player);
        register(criticalsA);
        register(criticalsB);
        this.criticalsFamily = new CheckFamily(player, "Criticals", criticalsA, criticalsB);

        this.sprintA = new SprintA(player);
        register(sprintA);
        this.sprintFamily = new CheckFamily(player, "Sprint", sprintA);

        this.autoBlockA = new AutoBlockA(player);
        register(autoBlockA);
        this.autoBlockFamily = new CheckFamily(player, "AutoBlock", autoBlockA);

        this.fastUseA = new FastUseA(player);
        register(fastUseA);
        this.fastUseFamily = new CheckFamily(player, "FastUse", fastUseA);

        this.vClipA = new VClipA(player);
        register(vClipA);
        this.vClipFamily = new CheckFamily(player, "VClip", vClipA);

        this.motionA = new MotionA(player);
        this.motionB = new MotionB(player);
        this.motionC = new MotionC(player);
        this.motionD = new MotionD(player);
        register(motionA);
        register(motionB);
        register(motionC);
        register(motionD);
        this.motionAFamily = new CheckFamily(player, "MotionA", motionA);
        this.motionBFamily = new CheckFamily(player, "MotionB", motionB);
        this.motionCFamily = new CheckFamily(player, "MotionC", motionC);
        this.motionDFamily = new CheckFamily(player, "MotionD", motionD);

        this.hitboxA = new HitboxA(player);
        register(hitboxA);
        this.hitboxFamily = new CheckFamily(player, "Hitbox", hitboxA);

        this.nukerA = new NukerA(player);
        this.nukerB = new NukerB(player);
        register(nukerA);
        register(nukerB);
        this.nukerFamily = new CheckFamily(player, "Nuker", nukerA, nukerB);

        this.autoFarmA = new AutoFarmA(player, interactionSampler);
        this.autoFarmB = new AutoFarmB(player, interactionSampler);
        register(autoFarmA);
        register(autoFarmB);
        this.autoFarmFamily = new CheckFamily(player, "AutoFarm", autoFarmA, autoFarmB);

        this.fastBreakA = new FastBreakA(player);
        register(fastBreakA);
        this.fastBreakFamily = new CheckFamily(player, "FastBreak", fastBreakA);

        this.spooferA = new SpooferA(player);
        register(spooferA);
        this.spooferFamily = new CheckFamily(player, "Spoofer", spooferA);

        this.lengthA = new LengthA(player);
        register(lengthA);

        this.chunkOverloaderA = new ChunkOverloaderA(player);
        register(chunkOverloaderA);
        this.chunkOverloaderFamily = new CheckFamily(player, "ChunkOverloader", chunkOverloaderA);

        this.voidBearerA = new VoidBearerA(player);
        register(voidBearerA);
        this.voidBearerFamily = new CheckFamily(player, "VoidBearer", voidBearerA);

        this.strafeA = new StrafeA(player);
        register(strafeA);
        this.strafeFamily = new CheckFamily(player, "Strafe", strafeA);

        this.elytraFlyA = new ElytraFlyA(player);
        register(elytraFlyA);
        this.elytraFlyFamily = new CheckFamily(player, "ElytraFly", elytraFlyA);

        this.blinkA = new BlinkA(player);
        register(blinkA);
        this.blinkFamily = new CheckFamily(player, "Blink", blinkA);

        this.inventoryA = new InventoryA(player);
        register(inventoryA);
        this.inventoryFamily = new CheckFamily(player, "Inventory", inventoryA);

        this.baritoneA = new BaritoneA(player);
        register(baritoneA);
        this.baritoneFamily = new CheckFamily(player, "Baritone", baritoneA);

        this.autoWalkA = new AutoWalkA(player);
        register(autoWalkA);
        this.autoWalkFamily = new CheckFamily(player, "AutoWalk", autoWalkA);

        this.phaseA = new PhaseA(player);
        register(phaseA);
        this.phaseFamily = new CheckFamily(player, "Phase", phaseA);

        this.noFallA = new NoFallA(player);
        register(noFallA);
        this.noFallFamily = new CheckFamily(player, "NoFall", noFallA);

        this.autoFishA = new AutoFishA(player);
        register(autoFishA);
        this.autoFishFamily = new CheckFamily(player, "AutoFish", autoFishA);

        this.reachA = new ReachA(player);
        this.reachB = new ReachB(player);
        register(reachA);
        register(reachB);
        this.reachFamily = new CheckFamily(player, "Reach", reachA, reachB);

        this.killAuraA = new KillAuraA(player);
        this.killAuraB = new KillAuraB(player);
        this.killAuraC = new KillAuraC(player);
        this.killAuraD = new KillAuraD(player);
        this.killAuraE = new KillAuraE(player);
        register(killAuraA);
        register(killAuraB);
        register(killAuraC);
        register(killAuraD);
        register(killAuraE);
        this.killAuraFamily = new CheckFamily(player, "KillAura",
                killAuraA, killAuraB, killAuraC, killAuraD, killAuraE);

        this.scaffoldA = new ScaffoldA(player);
        this.scaffoldB = new ScaffoldB(player);
        this.scaffoldC = new ScaffoldC(player);
        this.scaffoldD = new ScaffoldD(player);
        this.scaffoldE = new ScaffoldE(player);
        this.scaffoldF = new ScaffoldF(player);
        this.scaffoldG = new ScaffoldG(player);
        this.scaffoldH = new ScaffoldH(player);
        for (Check c : new Check[]{scaffoldA, scaffoldB, scaffoldC, scaffoldD, scaffoldE, scaffoldF, scaffoldG, scaffoldH}) {
            register(c);
        }

        this.scaffoldFamily = new CheckFamily(player, "Scaffold",
                scaffoldA, scaffoldB, scaffoldC, scaffoldD, scaffoldE, scaffoldF, scaffoldG, scaffoldH);

        this.boatFlyA = new BoatFlyA(player);
        this.boatFlyB = new BoatFlyB(player);
        this.boatFlyC = new BoatFlyC(player);
        register(boatFlyA);
        register(boatFlyB);
        register(boatFlyC);

        this.boatFlyFamily = new CheckFamily(player, "BoatFly", boatFlyA, boatFlyB, boatFlyC);

        this.entitySpoofA = new EntitySpoofA(player);
        this.entitySpoofB = new EntitySpoofB(player);
        this.entitySpoofC = new EntitySpoofC(player);
        this.entitySpoofD = new EntitySpoofD(player);
        this.entitySpoofE = new EntitySpoofE(player);
        this.entitySpoofF = new EntitySpoofF(player);
        this.entitySpoofG = new EntitySpoofG(player);
        for (Check c : new Check[]{entitySpoofA, entitySpoofB, entitySpoofC, entitySpoofD, entitySpoofE, entitySpoofF, entitySpoofG}) {
            register(c);
        }

        this.entitySpoofFamily = new CheckFamily(player, "EntitySpoof",
                entitySpoofA, entitySpoofB, entitySpoofC, entitySpoofD, entitySpoofE, entitySpoofF, entitySpoofG);

        this.noSlowA = new NoSlowA(player);
        this.noSlowB = new NoSlowB(player);
        register(noSlowA);
        register(noSlowB);
        this.noSlowBlockFamily = new CheckFamily(player, "NoSlowA", noSlowA);
        this.noSlowUseFamily = new CheckFamily(player, "NoSlowB", noSlowB);

        this.guiMoveA = new GuiMoveA(player);
        register(guiMoveA);
        this.guiFamily = new CheckFamily(player, "GuiInteract", guiMoveA);
        this.fastPlaceFamily = new CheckFamily(player, "FastPlace", fastPlaceA, fastPlaceB, fastPlaceC, fastPlaceD, fastPlaceE);
        this.autoClickerFamily = new CheckFamily(player, "AutoClicker", autoClickerA, autoClickerB, autoClickerC, autoClickerD);
        this.fastBowFamily = new CheckFamily(player, "FastBow", fastBowA, fastBowB);
        this.towerFamily = new CheckFamily(player, "Tower", towerA, towerB);
    }

    private void register(Check check) {
        all.add(check);
        if (check instanceof PacketCheck pc) packetChecks.add(pc);
        if (check instanceof BlockPlaceCheck bpc) blockPlaceChecks.add(bpc);
        if (check instanceof BlockBreakCheck bbc) blockBreakChecks.add(bbc);
    }

    public void markTeleport() {
        flightA.markTeleport();
        speedA.markTeleport();
        speedB.markTeleport();
        speedC.markTeleport();
        speedD.markTeleport();
        speedE.markTeleport();
        speedF.markTeleport();
        speedG.markTeleport();
        speedH.markTeleport();
        groundSpoofA.markTeleport();
        stepA.markTeleport();
        highJumpA.markTeleport();
        highJumpB.markTeleport();
        fastClimbA.markTeleport();
        wallClimbA.markTeleport();
        antiLevitationA.markTeleport();
        motionA.markTeleport();
        motionB.markTeleport();
        motionC.markTeleport();
        motionD.markTeleport();
        vClipA.markTeleport();
        chunkOverloaderA.markTeleport();
        strafeA.markTeleport();
        blinkA.markTeleport();
        baritoneA.markTeleport();
        autoWalkA.markTeleport();
        phaseA.markTeleport();
        noFallA.markImpulse();
    }

    public void markVelocity() {
        flightA.markKnockback();
        speedA.markVelocity();
        speedB.markVelocity();
        speedC.markVelocity();
        speedD.markVelocity();
        speedE.markVelocity();
        speedF.markVelocity();
        speedG.markVelocity();
        speedH.markVelocity();
        motionB.markVelocity();
        motionD.markVelocity();
        vClipA.markVelocity();
        strafeA.markVelocity();
        elytraFlyA.markVelocity();
        noFallA.markImpulse();
        noFallA.markDamage();
    }

    public void onBowShoot(int tick) {
        fastBowB.onBowShoot(tick);
        fastBowFamily.evaluate();
    }

    public void exempt(int ticks) {
        if (ticks <= 0) return;
        highJumpB.exempt(ticks);
        speedH.exempt(ticks);
    }

    public void onPacketReceive(VeritePacketEvent event) {
        syncPlayerState(event);
        for (PacketCheck check : packetChecks) {
            check.onPacketReceive(event);
        }

        if (event.isFlying()) {
            speedFamily.evaluate();
            jesusFamily.evaluate();
            timerFamily.evaluate();
            spooferFamily.evaluate();
            chunkOverloaderFamily.evaluate();
            voidBearerFamily.evaluate();
            strafeFamily.evaluate();
            elytraFlyFamily.evaluate();
            blinkFamily.evaluate();
            baritoneFamily.evaluate();
            autoWalkFamily.evaluate();
            phaseFamily.evaluate();
            noFallFamily.evaluate();
            noSlowBlockFamily.evaluate();
            noSlowUseFamily.evaluate();
            guiFamily.evaluate();
            groundSpoofFamily.evaluate();
            stepFamily.evaluate();
            highJumpFamily.evaluate();
            fastClimbFamily.evaluate();
            wallClimbFamily.evaluate();
            antiLevitationFamily.evaluate();
            motionAFamily.evaluate();
            motionBFamily.evaluate();
            motionCFamily.evaluate();
            motionDFamily.evaluate();
            velocityFamily.evaluate();
            sprintFamily.evaluate();
            vClipFamily.evaluate();
            killAuraFamily.evaluate();
        }
        if (event.getPacketType() == VeritePacketType.ANIMATION
                || event.getPacketType() == VeritePacketType.ATTACK) {
            autoClickerFamily.evaluate();
        }

        if (event.getPacketType() == VeritePacketType.ATTACK) {
            player.lastAttackTick = player.currentTick();
            teacommontea.veritechasse.engine.entity.TrackedEntity target =
                    player.entityTracker.get(event.getInteractEntityId());
            if (target != null) {
                lengthA.onEntityInteract(target.x, target.y + target.height / 2.0, target.z);
            }
            criticalsFamily.evaluate();
            autoBlockFamily.evaluate();
            reachFamily.evaluate();
            hitboxFamily.evaluate();
            killAuraFamily.evaluate();
        }
        if (event.getPacketType() == VeritePacketType.PLAYER_DIGGING) {
            fastUseFamily.evaluate();
        }
    }

    private void syncPlayerState(VeritePacketEvent event) {
        if (!event.isFlying()) return;
        VeritePacketEvent flying = event;
        if (flying.hasRotationChanged()) {
            player.lastYaw = player.yaw;
            player.lastPitch = player.pitch;
            player.yaw = flying.getLocation().getYaw();
            player.pitch = flying.getLocation().getPitch();
            player.rotations.update(player.yaw, player.pitch);
            if (player.currentTick() - player.lastAttackTick > 40) {
                player.rotations.calibrate();
            }
        }
        if (flying.hasPositionChanged()) {
            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;
            player.x = flying.getLocation().getX();
            player.y = flying.getLocation().getY();
            player.z = flying.getLocation().getZ();
        }
        player.onGround = flying.isOnGround();
    }

    public void onBlockPlace(BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceChecks) {
            check.onBlockPlace(place);
        }
        fastPlaceFamily.evaluate();
        towerFamily.evaluate();
        lengthA.onBlockInteract(place.position.getX(), place.position.getY(),
                place.position.getZ(), "place");

        org.bukkit.entity.Player bukkit = org.bukkit.Bukkit.getPlayer(player.getUuid());
        if (bukkit != null) {
            airPlaceA.onPlace(bukkit, place);
            airPlaceB.onPlace(bukkit, place);
            airPlaceFamily.evaluate();

            scaffoldA.onPlace(bukkit, place);
            scaffoldB.onPlace(bukkit, place);
            scaffoldC.onPlace(bukkit, place);
            scaffoldD.onPlace(bukkit, place);
            scaffoldE.onPlace(bukkit, place);
            scaffoldF.onPlace(bukkit, place);
            scaffoldG.onPlace(bukkit, place);
            scaffoldH.onPlace(bukkit, place);
            scaffoldFamily.evaluate();
        }

        interactionSampler.record(System.currentTimeMillis(),
                place.position.getX(), place.position.getY(), place.position.getZ(),
                player.yaw, player.pitch);
        autoFarmA.evaluate();
        autoFarmB.evaluate();
        autoFarmFamily.evaluate();
    }

    public void onTick(org.bukkit.entity.Player bukkit) {
        scaffoldC.onTick(bukkit);
    }

    public void onContainerOpen(org.bukkit.Location playerLoc, org.bukkit.Location containerLoc) {
        lengthA.onContainerOpen(playerLoc, containerLoc);
    }

    public void onInventoryClick() {
        inventoryA.onInventoryClick();
        inventoryFamily.evaluate();
    }

    public void onFishBite() {
        autoFishA.onBite();
    }

    public void onFishReel() {
        autoFishA.onReel();
        autoFishFamily.evaluate();
    }

    public void onVehicleMove(double boatX, double boatY, double boatZ) {
        org.bukkit.entity.Player bukkit = org.bukkit.Bukkit.getPlayer(player.getUuid());
        if (bukkit == null) return;
        boatFlyA.onVehicleMove(bukkit, boatY);
        boatFlyB.onVehicleMove(bukkit, boatY);
        boatFlyC.onVehicleMove(bukkit, boatX, boatZ);
        boatFlyFamily.evaluate();

        entitySpoofA.evaluate(bukkit);
        entitySpoofB.evaluate(bukkit);
        entitySpoofF.evaluate(bukkit);
        entitySpoofC.onVehicleMove(bukkit);
        entitySpoofD.onVehicleMove(bukkit, boatX, boatZ);
        entitySpoofE.onVehicleMove(bukkit, boatY);
        entitySpoofG.onVehicleMove(bukkit, boatY);
        entitySpoofFamily.evaluate();
    }

    public void onBlockBreak(BlockBreak blockBreak) {
        for (BlockBreakCheck check : blockBreakChecks) {
            check.onBlockBreak(blockBreak);
        }
        lengthA.onBlockInteract(blockBreak.position.getX(), blockBreak.position.getY(),
                blockBreak.position.getZ(), "break");
        nukerFamily.evaluate();
        fastBreakFamily.evaluate();

        interactionSampler.record(System.currentTimeMillis(),
                blockBreak.position.getX(), blockBreak.position.getY(), blockBreak.position.getZ(),
                player.yaw, player.pitch);
        autoFarmA.evaluate();
        autoFarmB.evaluate();
        autoFarmFamily.evaluate();
    }

    public List<Check> all() {
        return all;
    }
}
