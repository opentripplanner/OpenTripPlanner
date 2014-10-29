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

import com.csvreader.CsvReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static java.lang.Boolean.FALSE;

class Table implements Iterator<Map<String, String>>, AutoCloseable {
    private Boolean next;
    final private CsvReader csvReader;
    final private Map<String, Integer> headers;

    Table(InputStream inputStream) {
        csvReader = new CsvReader(inputStream, Charset.forName("UTF-8"));
        Map<String, Integer> map = Maps.newHashMap();

        try {
            if (csvReader.readHeaders()) {
                final int count = csvReader.getHeaderCount();

                for (int i = 0; i < count; i++) {
                    map.put(csvReader.getHeader(i), i);
                }
            } else {
                throw new RuntimeException("Could not read headers");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        headers = Collections.unmodifiableMap(map);
    }

    @Override
    public boolean hasNext() {
        if (next == null) {
            try {
                next = csvReader.readRecord();
                if (!next) csvReader.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return next;
    }

    @Override
    public Map<String, String> next() {
        if (hasNext()) {
            next = null;

            return new Row(csvReader);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        if (FALSE.equals(next)) return;

        csvReader.close();

        next = FALSE;
    }

    final private class Row implements Map<String, String> {
        final private List<String> columns;

        private Row(CsvReader csvReader) {
            final int count = csvReader.getColumnCount();
            List<String> list = Lists.newArrayListWithCapacity(count);

            for (int i = 0; i < count; i++) {
                try {
                    list.add(csvReader.get(i));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            columns = Collections.unmodifiableList(list);
        }

        @Override
        public int size() {
            return columns.size();
        }

        @Override
        public boolean isEmpty() {
            return columns.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return headers.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return columns.contains(value);
        }

        @Override
        public String get(Object key) {
            Integer integer = headers.get(key);

            if (integer != null) return columns.get(integer);

            return null;
        }

        @Override
        public String put(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> keySet() {
            return headers.keySet();
        }

        @Override
        public Collection<String> values() {
            return columns;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            Set<Entry<String, String>> set = Sets.newHashSet();

            for (String string : keySet()) {
                set.add(new Cell(string));
            }

            return set;
        }

        final private class Cell implements Entry<String, String> {
            final private String key;

            private Cell(String key) {
                this.key = key;
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getValue() {
                return get(key);
            }

            @Override
            public String setValue(String value) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
