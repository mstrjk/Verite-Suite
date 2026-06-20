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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SauverMojang {

    public enum Status { FOUND, NOT_FOUND, UNKNOWN }

    public record Profile(UUID uuid, String name, Status status) {
        static Profile found(UUID u, String n) { return new Profile(u, n, Status.FOUND); }
        static final Profile NOT_FOUND = new Profile(null, null, Status.NOT_FOUND);
        static final Profile UNKNOWN   = new Profile(null, null, Status.UNKNOWN);
    }

    private static final String ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private static final ConcurrentHashMap<String, Profile> CACHE = new ConcurrentHashMap<>();

    private SauverMojang() {}

    public static Profile lookup(String name) {
        if (name == null || name.isBlank()) {
            return Profile.NOT_FOUND;
        }
        String key = name.toLowerCase(Locale.ROOT);
        Profile cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Profile result = query(name);
        if (result.status() != Status.UNKNOWN) {
            CACHE.put(key, result);
        }
        return result;
    }

    private static Profile query(String name) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + name))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code == 404 || code == 204) {
                return Profile.NOT_FOUND;
            }
            if (code != 200) {
                return Profile.UNKNOWN;
            }
            String body = resp.body();
            String id = extract(body, "id");
            String canonical = extract(body, "name");
            if (id == null) {
                return Profile.UNKNOWN;
            }
            return Profile.found(dashed(id), canonical == null ? name : canonical);
        } catch (Throwable t) {
            return Profile.UNKNOWN;
        }
    }

    private static String extract(String json, String field) {
        if (json == null) {
            return null;
        }
        String needle = "\"" + field + "\"";
        int k = json.indexOf(needle);
        if (k < 0) {
            return null;
        }
        int colon = json.indexOf(':', k + needle.length());
        if (colon < 0) {
            return null;
        }
        int open = json.indexOf('"', colon + 1);
        if (open < 0) {
            return null;
        }
        int close = json.indexOf('"', open + 1);
        if (close < 0) {
            return null;
        }
        return json.substring(open + 1, close);
    }

    private static UUID dashed(String undashed) {
        if (undashed.length() != 32) {
            return UUID.fromString(undashed);
        }
        String d = undashed.substring(0, 8) + "-" + undashed.substring(8, 12) + "-"
                + undashed.substring(12, 16) + "-" + undashed.substring(16, 20) + "-"
                + undashed.substring(20);
        return UUID.fromString(d);
    }
}
