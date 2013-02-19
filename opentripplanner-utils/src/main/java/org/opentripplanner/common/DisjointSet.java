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

package org.opentripplanner.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.util.MapUtils;

/** basic union-find data structure with path compression */
public class DisjointSet<T> {

    ArrayList<Integer> sets = new ArrayList<Integer>();
    HashMap<T, Integer> setMapping = new HashMap<T, Integer>();
    
    public DisjointSet() {}
    
    public int union(T element1, T element2) {
        Integer p1 = find(element1);
        Integer p2 = find(element2);
        
        if (p1.equals(p2)) {
            return p1;
        }
        
        int p1size = -sets.get(p1);
        int p2size = -sets.get(p2);

        int totalSize = p1size + p2size;
        
        if (p1size > p2size) {
            sets.set(p2, p1);
            sets.set(p1, -totalSize);
            return p2;
        } else {
            sets.set(p1, p2);
            sets.set(p2, -totalSize);
            return p1;
        }
    }
    
    public int find(T element) {
        Integer i = setMapping.get(element);
        if (i == null) {
            setMapping.put(element, sets.size());
            sets.add(-1);
            return sets.size() -1;
        }
        return compact(i);
    }

    public boolean exists(T element) {
        return setMapping.containsKey(element);
    }
    
    public List<Set<T>> sets() {
        HashMap<Integer, Set<T>> out = new HashMap<Integer, Set<T>>();
        for (Map.Entry<T, Integer> entry : setMapping.entrySet()) {
            MapUtils.addToMapSet(out, compact(entry.getValue()), entry.getKey());
        }
        return new ArrayList<Set<T>>(out.values());
    }

    private int compact(int i) {
        int key = sets.get(i);
        if (key < 0) {
            return i;
        }
        int j = compact(key);
        sets.set(i, j);
        return j;
    }

    public int size(int component) {
        return -sets.get(component);
    }
}
