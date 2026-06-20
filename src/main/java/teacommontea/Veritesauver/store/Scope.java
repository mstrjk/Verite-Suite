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

package teacommontea.veritesauver.store;

import java.math.BigDecimal;
import java.util.List;

public final class Scope {

    private final SauverStore store;
    private final String scope;

    Scope(SauverStore store, String scope) {
        this.store = store;
        this.scope = scope;
    }

    public void set(String key, Object value) {
        store.set(scope, key, value);
    }

    public void delete(String key) {
        store.delete(scope, key);
    }

    public boolean has(String key) {
        return store.get(scope, key) != null;
    }

    public List<Entry> entries() {
        return store.entries(scope);
    }

    public Object get(String key) {
        return store.get(scope, key);
    }

    public String getString(String key, String def) {
        Object v = get(key);
        return v == null ? def : String.valueOf(v);
    }

    public long getLong(String key, long def) {
        BigDecimal d = getBigDecimal(key);
        return d == null ? def : d.longValue();
    }

    public int getInt(String key, int def) {
        return (int) getLong(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        Object v = get(key);
        return v instanceof Boolean b ? b : def;
    }

    public BigDecimal getBigDecimal(String key) {
        Object v = get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record Entry(String key, Object value) {}
}
