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

package org.opentripplanner.util;

import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.TestCase;

public class TestJoinedList extends TestCase {
    @SuppressWarnings("unchecked")
    public void testJoinedList() {
        ArrayList<Integer> list1 = new ArrayList<Integer>();
        list1.add(0);
        list1.add(1);
        list1.add(2);
        ArrayList<Integer> list2 = new ArrayList<Integer>();
        list2.add(3);
        list2.add(4);
        list2.add(5);
        JoinedList<Integer> joined = new JoinedList<Integer>(list1, list2);
        assertTrue(joined.get(0) == 0);
        assertTrue(joined.get(3) == 3);
        
        Iterator<Integer> it = joined.iterator();
        for (int i = 0; i < 6; ++i) {
            assertTrue(it.hasNext());
            assertTrue(it.next() == i);
        }
        assertFalse(it.hasNext());
    }
}
