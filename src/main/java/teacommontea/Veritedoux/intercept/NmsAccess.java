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

package teacommontea.veritedoux.intercept;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.netty.channel.Channel;

final class NmsAccess {

    static final class Unsupported extends Exception {
        Unsupported(String m) { super(m); }
        Unsupported(String m, Throwable c) { super(m, c); }
    }

    private final int dataVersion;
    private final MethodHandle getHandle;
    private final Field connectionField;
    private final Field rawConnectionField;
    private final Field channelField;
    private final Class<?> chatPacketClass;
    private final Field packetComponentField;
    private final ComponentDecoder decoder;

    interface ComponentDecoder {
        String toPlain(Object nmsComponent) throws Throwable;
    }

    private NmsAccess(int dataVersion, MethodHandle getHandle, Field connectionField, Field rawConnectionField,
                      Field channelField, Class<?> chatPacketClass, Field packetComponentField,
                      ComponentDecoder decoder) {
        this.dataVersion = dataVersion;
        this.getHandle = getHandle;
        this.connectionField = connectionField;
        this.rawConnectionField = rawConnectionField;
        this.channelField = channelField;
        this.chatPacketClass = chatPacketClass;
        this.packetComponentField = packetComponentField;
        this.decoder = decoder;
    }

    int dataVersion() { return dataVersion; }
    Class<?> chatPacketClass() { return chatPacketClass; }

    boolean isChatPacket(Object packet) {
        return packet != null && chatPacketClass.isInstance(packet);
    }

    String plainTextOf(Object packet) {
        try {
            Object component = packetComponentField.get(packet);
            if (component == null) return null;
            return decoder.toPlain(component);
        } catch (Throwable t) {
            return null;
        }
    }

    Channel channelOf(Player p) throws Throwable {
        Object handle = getHandle.invoke(p);
        Object listener = connectionField.get(handle);
        Object connection = rawConnectionField.get(listener);
        return (Channel) channelField.get(connection);
    }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static NmsAccess resolve() throws Unsupported {
        int dv = dataVersionOf();
        try {
            Class<?> craftPlayer = obc("entity.CraftPlayer");
            Method gh = craftPlayer.getMethod("getHandle");
            MethodHandle getHandle = LOOKUP.unreflect(gh);
            Class<?> serverPlayer = gh.getReturnType();

            Class<?> packetClass = firstExisting(
                    "net.minecraft.network.protocol.game.ClientboundSystemChatPacket",
                    "net.minecraft.network.protocol.game.ClientboundChatPacket");
            if (packetClass == null) {
                throw new Unsupported("no known outbound chat packet class on this server");
            }

            Field componentField = firstComponentField(packetClass);
            if (componentField == null) {
                throw new Unsupported("no Component field on " + packetClass.getName());
            }
            componentField.setAccessible(true);

            Field connField = firstFieldOfAnyType(serverPlayer,
                    "net.minecraft.server.network.ServerGamePacketListenerImpl",
                    "net.minecraft.server.network.PlayerConnection",
                    "net.minecraft.server.network.ServerCommonPacketListenerImpl");
            if (connField == null) {
                throw new Unsupported("no packet-listener field on " + serverPlayer.getName());
            }
            connField.setAccessible(true);
            Class<?> listenerType = connField.getType();

            Field rawConn = firstFieldOfAnyType(deepestListener(listenerType),
                    "net.minecraft.network.Connection",
                    "net.minecraft.network.NetworkManager");
            if (rawConn == null) {
                throw new Unsupported("no Connection field on " + listenerType.getName());
            }
            rawConn.setAccessible(true);

            Field chan = firstFieldOfType(rawConn.getType(), Channel.class);
            if (chan == null) {
                throw new Unsupported("no netty Channel field on " + rawConn.getType().getName());
            }
            chan.setAccessible(true);

            ComponentDecoder decoder = ComponentDecoders.resolve();

            return new NmsAccess(dv, getHandle, connField, rawConn, chan, packetClass, componentField, decoder);
        } catch (Unsupported u) {
            throw u;
        } catch (Throwable t) {
            throw new Unsupported("NMS resolution failed: " + t, t);
        }
    }

    private static Class<?> deepestListener(Class<?> listenerType) {
        return listenerType;
    }

    private static Field firstComponentField(Class<?> c) {
        Class<?> comp = classOrNull("net.minecraft.network.chat.Component");
        Class<?> compLegacy = classOrNull("net.minecraft.network.chat.IChatBaseComponent");
        for (Field f : c.getDeclaredFields()) {
            Class<?> t = f.getType();
            if ((comp != null && t == comp) || (compLegacy != null && t == compLegacy)) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    private static Field firstFieldOfAnyType(Class<?> start, String... typeNames) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                String tn = f.getType().getName();
                for (String want : typeNames) {
                    if (tn.equals(want)) return f;
                }
            }
        }
        return null;
    }

    private static Field firstFieldOfType(Class<?> start, Class<?> type) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) return f;
            }
        }
        return null;
    }

    private static Class<?> firstExisting(String... names) {
        for (String n : names) {
            Class<?> c = classOrNull(n);
            if (c != null) return c;
        }
        return null;
    }

    private static Class<?> classOrNull(String n) {
        try { return Class.forName(n); } catch (Throwable t) { return null; }
    }

    private static Class<?> obc(String sub) throws ClassNotFoundException {
        String base = Bukkit.getServer().getClass().getPackage().getName();
        try {
            return Class.forName(base + "." + sub);
        } catch (ClassNotFoundException e) {
            return Class.forName("org.bukkit.craftbukkit." + sub);
        }
    }

    private static int dataVersionOf() {
        try {
            return Bukkit.getUnsafe().getDataVersion();
        } catch (Throwable t) {
            return -1;
        }
    }
}
