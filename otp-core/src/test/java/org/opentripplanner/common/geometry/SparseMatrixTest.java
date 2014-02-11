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

package org.opentripplanner.common.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class SparseMatrixTest extends TestCase {

    public void testSparseMatrix() {

        List<String> all = new ArrayList<String>();

        SparseMatrix<String> m = new SparseMatrix<String>(8, 300);
        assertNull(m.get(0, 0));
        assertNull(m.get(10, 10));
        assertNull(m.get(-10, -10));
        assertEquals(0, m.size());
        for (String e : m) {
            throw new AssertionError("Should not iterate over empty matrix");
        }
        
        m.put(0, 0, "A");
        assertEquals(1, m.size());
        assertEquals("A", m.get(0, 0));
        m.put(0, 0, "A2");
        assertEquals("A2", m.get(0, 0));
        assertEquals(1, m.size());

        m.put(-1000, -8978, "B");
        all.add("B");
        assertEquals("B", m.get(-1000, -8978));
        m.put(223980, -898978, "C");
        all.add("C");
        assertEquals("C", m.get(223980, -898978));
        assertEquals(3, m.size());

        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                String s = i + ":" + j;
                m.put(i, j, s);
                all.add(s);
            }
        }
        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                String s = m.get(i, j);
                assertEquals(i + ":" + j, s);
            }
        }

        List<String> elements = new ArrayList<String>();
        for (String s : m) {
            elements.add(s);
        }
        Collections.sort(elements);
        Collections.sort(all);
        assertEquals(all, elements);
        assertEquals(402, elements.size());
    }
}