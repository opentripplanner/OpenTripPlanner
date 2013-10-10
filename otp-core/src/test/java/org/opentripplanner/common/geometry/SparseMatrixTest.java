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
import java.util.List;

import junit.framework.TestCase;

import org.opentripplanner.common.geometry.SparseMatrix.Visitor;

public class SparseMatrixTest extends TestCase {

    public void testSparseMatrix() {
        
        SparseMatrix<String> m = new SparseMatrix<String>(8, 300);
        assertNull(m.get(0, 0));
        assertNull(m.get(10, 10));
        assertNull(m.get(-10, -10));
        assertEquals(0, m.size());
        m.put(0, 0, "A");
        assertEquals(1, m.size());
        assertEquals("A", m.get(0, 0));
        m.put(0, 0, "A2");
        assertEquals("A2", m.get(0, 0));
        assertEquals(1, m.size());

        m.put(-1000, -8978, "B");
        assertEquals("B", m.get(-1000, -8978));
        m.put(223980, -898978, "C");
        assertEquals("C", m.get(223980, -898978));
        assertEquals(3, m.size());

        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                m.put(i, j, i + ":" + j);
            }
        }
        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                String s = m.get(i, j);
                assertEquals(i + ":" + j, s);
            }
        }

        final List<String> elements = new ArrayList<String>();
        m.iterate(new Visitor<String>() {
            @Override
            public void visit(String s) {
                elements.add(s);
            }
        });
        assertEquals(402, elements.size());
    }
}