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

import java.util.HashMap;
import java.util.Random;

import org.junit.Test;
import org.opentripplanner.common.DisjointSet;

import junit.framework.TestCase;

public class TestDisjointSet  extends TestCase{

    @Test
    public void testSimple() {
        DisjointSet<String> set = new DisjointSet<String>();
        set.union("cats", "dogs");
        assertEquals(set.size(set.find("cats")), 2);
        
        assertEquals(set.find("cats"), set.find("dogs"));
        assertTrue(set.find("cats") != set.find("sparrows"));
        assertEquals(set.size(set.find("sparrows")), 1);

        set.union("sparrows", "robins");
        assertEquals(set.size(set.find("robins")), 2);

        assertTrue(set.sets().size() == 2);
        
        assertTrue(set.find("dogs") != set.find("robins"));
        assertEquals(set.find("sparrows"), set.find("robins"));
        
        set.union("sparrows", "dogs");
        assertEquals(set.find("dogs"), set.find("robins"));
        assertEquals(set.size(set.find("cats")), 4);

        assertTrue(set.sets().size() == 1);
    }
    
    public void testRandom() {
        DisjointSet<Integer> set = new DisjointSet<Integer>();
        Random random = new Random(1);
        for (int i = 0; i < 150; ++i) {
            set.union(Math.abs(random.nextInt() % 700), Math.abs(random.nextInt() % 700));
        }

        HashMap<Integer, Integer> seen = new HashMap<Integer, Integer>();
        int sizeSum = 0;
        for (int i = 0; i < 700; ++i) {
            int key = set.find(i);
            int size = set.size(key);
            
            Integer lastSize = seen.get(key);
            assertTrue(lastSize == null || size == lastSize);
            if (lastSize == null) {
                seen.put(key, size);
                sizeSum += size;
            }
            assertTrue(size >= 1 && size <= 150);
            
        }
        assertEquals(700, sizeSum);
    }
}
