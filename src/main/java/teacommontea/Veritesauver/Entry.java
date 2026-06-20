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

package teacommontea.veritesauver;

import java.util.UUID;

public final class Entry {

    public static final long PERMANENT = -1L;
    public static final String GLOBAL_SCOPE = "*";

    public enum Type {
        BAN, MUTE, WARNING, KICK;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Type of(String s) {
            return valueOf(s.toUpperCase(java.util.Locale.ROOT));
        }
    }

    private final long id;
    private final String randomId;
    private final Type type;
    private final UUID uuid;
    private final String ip;
    private final String reason;
    private final UUID executorUuid;
    private final String executorName;
    private final UUID removedByUuid;
    private final String removedByName;
    private final String removalReason;
    private final long dateStart;
    private final long dateEnd;
    private final String serverScope;
    private final String serverOrigin;
    private final int template;
    private final boolean silent;
    private final boolean ipban;
    private final boolean active;

    public Entry(long id, String randomId, Type type, UUID uuid, String ip, String reason,
                 UUID executorUuid, String executorName, UUID removedByUuid, String removedByName,
                 String removalReason, long dateStart, long dateEnd, String serverScope,
                 String serverOrigin, int template, boolean silent, boolean ipban, boolean active) {
        this.id = id;
        this.randomId = randomId;
        this.type = type;
        this.uuid = uuid;
        this.ip = ip;
        this.reason = reason;
        this.executorUuid = executorUuid;
        this.executorName = executorName;
        this.removedByUuid = removedByUuid;
        this.removedByName = removedByName;
        this.removalReason = removalReason;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.serverScope = serverScope;
        this.serverOrigin = serverOrigin;
        this.template = template;
        this.silent = silent;
        this.ipban = ipban;
        this.active = active;
    }

    public long id()            { return id; }
    public String randomId()    { return randomId; }
    public Type type()          { return type; }
    public UUID uuid()          { return uuid; }
    public String ip()          { return ip; }
    public String reason()      { return reason; }
    public UUID executorUuid()  { return executorUuid; }
    public String executorName(){ return executorName; }
    public UUID removedByUuid() { return removedByUuid; }
    public String removedByName(){ return removedByName; }
    public String removalReason(){ return removalReason; }
    public long dateStart()     { return dateStart; }
    public long dateEnd()       { return dateEnd; }
    public String serverScope() { return serverScope; }
    public String serverOrigin(){ return serverOrigin; }
    public int template()       { return template; }
    public boolean silent()     { return silent; }
    public boolean ipban()      { return ipban; }
    public boolean active()     { return active; }

    public boolean permanent() {
        return dateEnd == PERMANENT;
    }

    public boolean expired(long now) {
        return !permanent() && now >= dateEnd;
    }

    public boolean inForce(long now) {
        return active && !expired(now);
    }

    public long duration() {
        return permanent() ? PERMANENT : dateEnd - dateStart;
    }

    public long remaining(long now) {
        if (permanent()) {
            return PERMANENT;
        }
        long left = dateEnd - now;
        return Math.max(0L, left);
    }

    public boolean hasTemplate() {
        return template != 0;
    }

    public Entry withActive(boolean newActive) {
        return new Entry(id, randomId, type, uuid, ip, reason, executorUuid, executorName,
                removedByUuid, removedByName, removalReason, dateStart, dateEnd,
                serverScope, serverOrigin, template, silent, ipban, newActive);
    }

    public Entry withRemoval(UUID byUuid, String byName, String why) {
        return new Entry(id, randomId, type, uuid, ip, reason, executorUuid, executorName,
                byUuid, byName, why, dateStart, dateEnd,
                serverScope, serverOrigin, template, silent, ipban, false);
    }
}
