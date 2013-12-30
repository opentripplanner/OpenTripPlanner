/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapUtils {

    /**
     * An extremely common pattern: add an item to a set in a hash value, creating that set if
     * necessary
     * TODO: Replace with Guava
     */
    public static final <T, U> void addToMapSet(Map<T, Set<U>> mapList, T key, U value) {
        Set<U> list = mapList.get(key);
        if (list == null) {
            list = new HashSet<U>();
            mapList.put(key, list);
        }
        list.add(value);
    }

    /**
     * An extremely common pattern: add an item to a list in a hash value, creating that list if
     * necessary
     * TODO: Replace with Guava
     */
    public static final <T, U> void addToMapList(Map<T, List<U>> mapList, T key, U value) {
        List<U> list = mapList.get(key);
        if (list == null) {
            list = new ArrayList<U>();
            mapList.put(key, list);
        }
        list.add(value);
    }

    public static final <T> boolean addToMaxMap(Map<T, Double> map, T key, double value) {
        Double oldValue = map.get(key);
        if (oldValue == null || value > oldValue) {
            map.put(key, value);
            return true;
        }
        return false;
    }

    public static <T, U> void addToMapListUnique(Map<T, List<U>> mapList,
            T key, List<U> values) {
        List<U> list = mapList.get(key);
        if (list == null) {
            list = new ArrayList<U>(values.size());
            mapList.put(key, list);
        }
        for (U value : values) {
            if (!list.contains(value)) {
                list.add(value);
            }
        }
    }

    public static <T, U, V extends Collection<U>> void mergeInUnique(Map<T, V> mapList,
            Map<T, V> from) {
        for (Map.Entry<T, V> entry : from.entrySet()) {
            T key = entry.getKey();
            V value = entry.getValue();
            V originalValue = mapList.get(key);
            if (originalValue != null) {
                HashSet<U> originalSet = new HashSet<U>(originalValue);
                for (U item : value) {
                    if (!originalSet.contains(item))
                        originalValue.add(item);
                }
            } else {
                mapList.put(key, value);
            }
        }
    }
}
