/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.gtfs.format;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Feed implements Map<String, Iterable<Map<String, String>>>, AutoCloseable {
    final private LinkedList<AutoCloseable> autoCloseables = new LinkedList<>();
    final private Map<String, Iterable<Map<String, String>>> tables;

    public Feed(String filename) {
        final ZipFile zipFile;
        try {
            zipFile = new ZipFile(filename);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        autoCloseables.add(zipFile);

        Map<String, Iterable<Map<String, String>>> map = Maps.newHashMap();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            map.put(zipEntry.getName(), new Iterable<Map<String, String>>() {
                @Override
                public Table iterator() {
                    try {
                        Table table = new Table(zipFile.getInputStream(zipEntry));
                        autoCloseables.addFirst(table);
                        return table;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        tables = Collections.unmodifiableMap(map);
    }

    @Override
    public int size() {
        return tables.size();
    }

    @Override
    public boolean isEmpty() {
        return tables.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return tables.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return tables.containsValue(value);
    }

    @Override
    public Iterable<Map<String, String>> get(Object key) {
        return tables.get(key);
    }

    @Override
    public Iterable<Map<String, String>> put(String key, Iterable<Map<String, String>> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Map<String, String>> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Iterable<Map<String, String>>> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return tables.keySet();
    }

    @Override
    public Collection<Iterable<Map<String, String>>> values() {
        return tables.values();
    }

    @Override
    public Set<Entry<String, Iterable<Map<String, String>>>> entrySet() {
        return tables.entrySet();
    }

    @Override
    public void close() {
        // Attempt to close all resources. If this should fail, continue to close the others anyway.
        for (AutoCloseable autoCloseable : autoCloseables) {
            try {
                autoCloseable.close();
            } catch (Exception e) {}
        }
    }
}
