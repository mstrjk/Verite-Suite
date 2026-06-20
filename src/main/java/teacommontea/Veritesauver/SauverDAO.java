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

import teacommontea.veritesauver.store.SauverStore;
import teacommontea.veritesauver.store.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SauverDAO {

    private static final String NS = "punish::";

    private final SauverStore store;

    public SauverDAO(SauverStore store) {
        this.store = store;
    }

    private Scope meta()                 { return store.scope(NS + "meta"); }
    private Scope active()               { return store.scope(NS + "active"); }
    private Scope entry(long id)         { return store.scope(NS + "entry::" + id); }
    private Scope history(UUID u)        { return store.scope(NS + "history." + u); }
    private Scope byIp(String ip)        { return store.scope(NS + "byip." + ip); }
    private Scope byStaff(UUID u)        { return store.scope(NS + "bystaff." + u); }

    private Scope player(UUID u)         { return store.scope(NS + "player." + u); }
    private Scope ipUsers(String ip)     { return store.scope(NS + "ipusers." + ip); }
    private Scope nameIndex()            { return store.scope(NS + "names"); }

    private Scope activeWarns()          { return store.scope(NS + "activewarns"); }

    public synchronized long nextId() {
        long next = meta().getLong("nextid", 1L);
        meta().set("nextid", next + 1);
        return next;
    }

    public void save(Entry e) {
        Scope s = entry(e.id());
        s.set("randomid", e.randomId());
        s.set("type", e.type().id());
        s.set("uuid", e.uuid() == null ? null : e.uuid().toString());
        s.set("ip", e.ip());
        s.set("reason", e.reason());
        s.set("executoruuid", e.executorUuid() == null ? null : e.executorUuid().toString());
        s.set("executorname", e.executorName());
        s.set("removedbyuuid", e.removedByUuid() == null ? null : e.removedByUuid().toString());
        s.set("removedbyname", e.removedByName());
        s.set("removalreason", e.removalReason());
        s.set("datestart", e.dateStart());
        s.set("dateend", e.dateEnd());
        s.set("serverscope", e.serverScope());
        s.set("serverorigin", e.serverOrigin());
        s.set("template", e.template());
        s.set("silent", e.silent());
        s.set("ipban", e.ipban());
        s.set("active", e.active());

        if (e.uuid() != null) {
            history(e.uuid()).set(String.valueOf(e.id()), e.dateStart());
        }
        if (e.ip() != null) {
            byIp(e.ip()).set(String.valueOf(e.id()), e.dateStart());
        }
        if (e.executorUuid() != null) {
            byStaff(e.executorUuid()).set(String.valueOf(e.id()), e.dateStart());
        }
        refreshActivePointer(e);

        if (e.type() == Entry.Type.WARNING) {
            if (e.active()) {
                activeWarns().set(String.valueOf(e.id()), e.dateStart());
            } else {
                activeWarns().delete(String.valueOf(e.id()));
            }
        }
    }

    private void refreshActivePointer(Entry e) {
        if (e.uuid() == null) {
            return;
        }
        if (e.type() != Entry.Type.BAN && e.type() != Entry.Type.MUTE) {
            return;
        }
        String key = e.type().id() + "." + e.uuid();
        if (e.active()) {
            active().set(key, e.id());
        } else {
            active().delete(key);
        }
    }

    public Entry load(long id) {
        List<Scope.Entry> rows = entry(id).entries();
        if (rows.isEmpty()) {
            return null;
        }
        Field f = new Field(rows);
        return new Entry(
            id,
            f.str("randomid"),
            Entry.Type.of(f.str("type")),
            f.uuid("uuid"),
            f.str("ip"),
            f.str("reason"),
            f.uuid("executoruuid"),
            f.str("executorname"),
            f.uuid("removedbyuuid"),
            f.str("removedbyname"),
            f.str("removalreason"),
            f.lng("datestart", 0L),
            f.lng("dateend", Entry.PERMANENT),
            f.str("serverscope"),
            f.str("serverorigin"),
            (int) f.lng("template", 0L),
            f.bool("silent"),
            f.bool("ipban"),
            f.bool("active")
        );
    }

    public Entry activeBan(UUID u)  { return activePointer(Entry.Type.BAN, u); }
    public Entry activeMute(UUID u) { return activePointer(Entry.Type.MUTE, u); }

    private Entry activePointer(Entry.Type type, UUID u) {
        if (u == null) {
            return null;
        }
        long id = active().getLong(type.id() + "." + u, 0L);
        if (id == 0L) {
            return null;
        }
        return load(id);
    }

    public List<ActivePointer> activePointers() {
        List<ActivePointer> out = new ArrayList<>();
        for (Scope.Entry e : active().entries()) {
            int dot = e.key().indexOf('.');
            if (dot <= 0) {
                continue;
            }
            Entry.Type type;
            try {
                type = Entry.Type.of(e.key().substring(0, dot));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            UUID u;
            try {
                u = UUID.fromString(e.key().substring(dot + 1));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            out.add(new ActivePointer(type, u, asLong(e.value())));
        }
        return out;
    }

    public record ActivePointer(Entry.Type type, UUID uuid, long entryId) {}

    public Entry expire(long id) {
        Entry e = load(id);
        if (e == null || !e.active()) {
            return e;
        }
        Entry lapsed = e.withActive(false);
        save(lapsed);
        return lapsed;
    }

    public Entry activeIpPunishment(Entry.Type type, String ip, long now) {
        if (ip == null) {
            return null;
        }
        for (Entry e : byIp(ip, 0)) {
            if (e.type() == type && e.inForce(now)) {
                return e;
            }
        }
        return null;
    }

    public Entry bannedAltOnIp(UUID self, String ip, long now) {
        if (ip == null) {
            return null;
        }
        for (Entry e : byIp(ip, 0)) {
            if (e.type() != Entry.Type.BAN || !e.inForce(now)) {
                continue;
            }
            if (e.uuid() != null && !e.uuid().equals(self)) {
                return e;
            }
        }
        return null;
    }

    public List<Entry> history(UUID u, int limit) {
        return loadIndexed(history(u), limit);
    }

    public List<Entry> byIp(String ip, int limit) {
        return loadIndexed(byIp(ip), limit);
    }

    public int templateOffenses(UUID u, int templateId, long cutoff) {
        if (templateId == 0) {
            return 0;
        }
        int count = 0;
        for (Entry e : history(u, 0)) {
            if (e.template() == templateId && e.dateStart() >= cutoff) {
                count++;
            }
        }
        return count;
    }

    public List<Entry> allActiveWarnings(long now) {
        List<Entry> out = new ArrayList<>();
        for (Scope.Entry row : activeWarns().entries()) {
            long id;
            try {
                id = Long.parseLong(row.key());
            } catch (NumberFormatException e) {
                continue;
            }
            Entry e = load(id);
            if (e == null || !e.active()) {
                activeWarns().delete(row.key());
                continue;
            }
            if (e.expired(now)) {
                continue;
            }
            out.add(e);
        }
        out.sort((a, b) -> Long.compare(b.dateStart(), a.dateStart()));
        return out;
    }

    public List<Entry> activeWarnings(UUID u, long now) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : history(u, 0)) {
            if (e.type() == Entry.Type.WARNING && e.active() && !e.expired(now)) {
                out.add(e);
            }
        }
        return out;
    }

    public List<Entry> byStaff(UUID u, int limit) {
        return loadIndexed(byStaff(u), limit);
    }

    public List<Entry> activeOfType(Entry.Type type, long now) {
        List<Entry> out = new ArrayList<>();
        for (ActivePointer ptr : activePointers()) {
            if (ptr.type() != type) {
                continue;
            }
            Entry e = load(ptr.entryId());
            if (e != null && e.inForce(now)) {
                out.add(e);
            }
        }
        out.sort((a, b) -> Long.compare(b.dateStart(), a.dateStart()));
        return out;
    }

    public List<Entry> activeByStaffSince(UUID staff, long cutoff, long now) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : byStaff(staff, 0)) {
            if ((e.type() == Entry.Type.BAN || e.type() == Entry.Type.MUTE)
                    && e.inForce(now) && e.dateStart() >= cutoff) {
                out.add(e);
            }
        }
        return out;
    }

    public int pruneHistory(UUID u, long cutoff, long now) {
        int removed = 0;
        for (Entry e : history(u, 0)) {
            if (e.inForce(now)) {
                continue;
            }
            if (e.dateStart() >= cutoff && cutoff != 0) {
                continue;
            }
            deleteEntry(e);
            removed++;
        }
        return removed;
    }

    private void deleteEntry(Entry e) {
        for (Scope.Entry row : entry(e.id()).entries()) {
            entry(e.id()).delete(row.key());
        }
        if (e.uuid() != null) {
            history(e.uuid()).delete(String.valueOf(e.id()));
        }
        if (e.ip() != null) {
            byIp(e.ip()).delete(String.valueOf(e.id()));
        }
        if (e.executorUuid() != null) {
            byStaff(e.executorUuid()).delete(String.valueOf(e.id()));
        }
    }

    private List<Entry> loadIndexed(Scope index, int limit) {
        List<Entry> out = new ArrayList<>();
        List<Scope.Entry> ids = index.entries();
        ids.sort((a, b) -> Long.compare(asLong(b.value()), asLong(a.value())));
        for (Scope.Entry id : ids) {
            if (limit > 0 && out.size() >= limit) {
                break;
            }
            Entry e = load(Long.parseLong(id.key()));
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    private static long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    public void recordLogin(UUID u, String name, String ip, long now) {
        Scope p = player(u);
        if (!p.has("firstjoin")) {
            p.set("firstjoin", now);
        }

        if (p.has("sessionstart")) {
            flushSession(u, now);
        }
        p.set("name", name);
        p.set("lastseen", now);
        p.set("joincount", p.getLong("joincount", 0L) + 1);
        p.set("sessionstart", now);
        if (ip != null) {
            p.set("ip." + ip, now);
            p.set("lastip", ip);
            ipUsers(ip).set(u.toString(), now);
        }
        if (name != null) {
            p.set("namehist." + name.toLowerCase(java.util.Locale.ROOT), now);
            nameIndex().set(name.toLowerCase(java.util.Locale.ROOT), u.toString());
        }
    }

    public void recordClient(UUID u, String brand, int protocol, String referrer, long now) {
        Scope p = player(u);
        if (brand != null && !brand.isBlank()) {
            p.set("client." + brand.toLowerCase(java.util.Locale.ROOT), now);
            p.set("lastclient", brand.toLowerCase(java.util.Locale.ROOT));
        }
        if (protocol > 0) {
            p.set("protocol", protocol);
        }
        if (referrer != null && !referrer.isBlank()) {
            p.set("referrer", referrer);
        }
    }

    public void flushSession(UUID u, long now) {
        Scope p = player(u);
        long start = p.getLong("sessionstart", 0L);
        if (start <= 0 || now <= start) {

            p.set("sessionstart", now);
            return;
        }
        long add = now - start;

        if (add > 30L * 24 * 60 * 60 * 1000) {
            p.set("sessionstart", now);
            return;
        }
        p.set("playtime", p.getLong("playtime", 0L) + add);
        p.set("sessionstart", now);
    }

    public void recordLogout(UUID u, long now) {
        Scope p = player(u);
        long start = p.getLong("sessionstart", 0L);
        if (start > 0 && now > start) {
            long add = now - start;
            if (add <= 30L * 24 * 60 * 60 * 1000) {
                p.set("playtime", p.getLong("playtime", 0L) + add);
            }
        }
        p.delete("sessionstart");
    }

    public boolean hasProfile(UUID u) {
        return player(u).has("firstjoin") || player(u).has("name");
    }

    public Profile profile(UUID u) {
        Scope p = player(u);
        long now = System.currentTimeMillis();
        long playtime = p.getLong("playtime", 0L);
        long session = p.getLong("sessionstart", 0L);
        if (session > 0 && now >= session) {
            playtime += now - session;
        }
        long punishments = history(u, 0).size();
        return new Profile(
            u,
            p.getString("name", null),
            namesOf(u),
            ipsOf(u),
            p.getString("lastip", null),
            clientsOf(u),
            p.getString("lastclient", null),
            p.getInt("protocol", 0),
            p.getString("referrer", null),
            p.getLong("firstjoin", 0L),
            p.getLong("lastseen", 0L),
            p.getLong("joincount", 0L),
            playtime,
            session > 0,
            punishments,
            SauverCheatBridge.flagCount(u)
        );
    }

    public List<String> clientsOf(UUID u) {
        List<Scope.Entry> rows = player(u).entries();
        List<Scope.Entry> clients = new ArrayList<>();
        for (Scope.Entry r : rows) {
            if (r.key().startsWith("client.")) {
                clients.add(r);
            }
        }
        clients.sort((a, b) -> Long.compare(asLong(b.value()), asLong(a.value())));
        List<String> out = new ArrayList<>();
        for (Scope.Entry r : clients) {
            out.add(r.key().substring("client.".length()));
        }
        return out;
    }

    public record Profile(
            UUID uuid,
            String name,
            List<String> names,
            List<String> ips,
            String lastIp,
            List<String> clients,
            String lastClient,
            int protocol,
            String referrer,
            long firstJoin,
            long lastSeen,
            long joinCount,
            long playtimeMs,
            boolean online,
            long punishments,
            long cheatFlags) {}

    public UUID uuidByName(String name) {
        if (name == null) {
            return null;
        }
        String s = nameIndex().getString(name.toLowerCase(java.util.Locale.ROOT), null);
        if (s == null) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String nameOf(UUID u) {
        return player(u).getString("name", null);
    }

    public List<String> knownNames() {
        List<String> out = new ArrayList<>();
        for (Scope.Entry e : nameIndex().entries()) {
            out.add(e.key());
        }
        return out;
    }

    public List<String> ipsOf(UUID u) {
        List<Scope.Entry> rows = player(u).entries();
        List<Scope.Entry> ips = new ArrayList<>();
        for (Scope.Entry r : rows) {
            if (r.key().startsWith("ip.")) {
                ips.add(r);
            }
        }
        ips.sort((a, b) -> Long.compare(asLong(b.value()), asLong(a.value())));
        List<String> out = new ArrayList<>();
        for (Scope.Entry r : ips) {
            out.add(r.key().substring(3));
        }
        return out;
    }

    public List<UUID> usersOfIp(String ip) {
        List<Scope.Entry> rows = ipUsers(ip).entries();
        rows.sort((a, b) -> Long.compare(asLong(b.value()), asLong(a.value())));
        List<UUID> out = new ArrayList<>();
        for (Scope.Entry r : rows) {
            try {
                out.add(UUID.fromString(r.key()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    public List<String> namesOf(UUID u) {
        List<Scope.Entry> rows = player(u).entries();
        List<Scope.Entry> names = new ArrayList<>();
        for (Scope.Entry r : rows) {
            if (r.key().startsWith("namehist.")) {
                names.add(r);
            }
        }
        names.sort((a, b) -> Long.compare(asLong(b.value()), asLong(a.value())));
        List<String> out = new ArrayList<>();
        for (Scope.Entry r : names) {
            out.add(r.key().substring("namehist.".length()));
        }
        return out;
    }

    private static final class Field {
        private final java.util.Map<String, Object> m = new java.util.HashMap<>();

        Field(List<Scope.Entry> rows) {
            for (Scope.Entry r : rows) {
                m.put(r.key(), r.value());
            }
        }

        String str(String k) {
            Object v = m.get(k);
            return v == null ? null : String.valueOf(v);
        }

        UUID uuid(String k) {
            String s = str(k);
            if (s == null) {
                return null;
            }
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        long lng(String k, long def) {
            Object v = m.get(k);
            return v instanceof Number n ? n.longValue() : def;
        }

        boolean bool(String k) {
            Object v = m.get(k);
            return v instanceof Boolean b && b;
        }
    }
}
