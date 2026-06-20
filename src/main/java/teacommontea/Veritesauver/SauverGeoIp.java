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

import java.io.File;
import java.net.InetAddress;

public final class SauverGeoIp {

    private static volatile Object reader;
    private static volatile boolean attempted;

    private SauverGeoIp() {}

    private static final String MIRROR_URL =
        "https://raw.githubusercontent.com/P3TERX/GeoLite.mmdb/download/GeoLite2-Country.mmdb";

    static void load(File dataFolder) {
        attempted = true;
        reader = null;
        File db = new File(dataFolder, "GeoLite2-Country.mmdb");
        if (db.exists()) {
            bind(db);
            return;
        }
        Sauver s = Sauver.instance();
        if (s == null || s.plugin() == null) {
            return;
        }

        s.plugin().getServer().getScheduler().runTaskAsynchronously(s.plugin(), () -> {
            if (download(db)) {
                bind(db);
                Sauver cur = Sauver.instance();
                if (cur != null && cur.plugin() != null) {
                    cur.plugin().getLogger().info("GeoLite2 country database downloaded; geoip is now active.");
                }
            }
        });
    }

    private static boolean download(File db) {
        try {
            File parent = db.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            java.net.URL url = java.net.URI.create(MIRROR_URL).toURL();
            java.net.URLConnection conn = url.openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            File tmp = new File(db.getAbsolutePath() + ".part");
            try (java.io.InputStream in = conn.getInputStream()) {
                java.nio.file.Files.copy(in, tmp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            java.nio.file.Files.move(tmp.toPath(), db.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Throwable t) {
            Sauver s = Sauver.instance();
            if (s != null && s.plugin() != null) {
                s.plugin().getLogger().warning("Could not download GeoLite2 database (geoip stays off): " + t.getMessage());
            }
            return false;
        }
    }

    private static void bind(File db) {
        try {
            Class<?> builderClass = Class.forName("com.maxmind.geoip2.DatabaseReader$Builder");
            Object builder = builderClass.getConstructor(File.class).newInstance(db);
            reader = builderClass.getMethod("build").invoke(builder);
        } catch (Throwable t) {
            reader = null;
        }
    }

    public static String country(String ip) {
        Object r = reader;
        if (r == null) {
            return null;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            Object response = r.getClass().getMethod("country", InetAddress.class).invoke(r, addr);
            Object country = response.getClass().getMethod("getCountry").invoke(response);
            Object name = country.getClass().getMethod("getName").invoke(country);
            return name == null ? null : name.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean available() {
        return reader != null;
    }

    public static boolean isPrivate(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isAnyLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isSiteLocalAddress();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean attempted() {
        return attempted;
    }
}
