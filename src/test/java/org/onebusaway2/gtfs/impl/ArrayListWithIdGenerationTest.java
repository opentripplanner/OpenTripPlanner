/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.onebusaway2.gtfs.impl;

import org.junit.Test;
import org.onebusaway2.gtfs.model.IdentityBean;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class ArrayListWithIdGenerationTest {
    @Test
    public void add() throws Exception {
        ArrayListWithIdGeneration<AClass> list = new ArrayListWithIdGeneration<>();

        list.add(new AClass());
        assertEquals(list.get(0).id, 1);

        list.add(new AClass(7));
        list.add(new AClass());
        assertEquals(list.get(1).id, 7);
        assertEquals(list.get(2).id, 8);
    }

    @Test
    public void addIndex() throws Exception {
        ArrayListWithIdGeneration<AClass> list = new ArrayListWithIdGeneration<>(
                singletonList(new AClass(3)));

        list.add(0, new AClass());
        assertEquals(4, list.get(0).id);
        assertEquals(3, list.get(1).id);
    }

    @Test
    public void addAll() throws Exception {
        ArrayListWithIdGeneration<AClass> list = new ArrayListWithIdGeneration<>();

        list.addAll(asList(new AClass(), new AClass(3), new AClass()));

        assertEquals(1, list.get(0).id);
        assertEquals(3, list.get(1).id);
        assertEquals(4, list.get(2).id);
    }

    @Test
    public void addAllIndex() throws Exception {
        ArrayListWithIdGeneration<AClass> list = new ArrayListWithIdGeneration<>(
                asList(new AClass(2), new AClass(4)));

        list.addAll(1, asList(new AClass(), new AClass(3)));

        assertEquals(2, list.get(0).id);
        assertEquals(5, list.get(1).id);
        assertEquals(3, list.get(2).id);
        assertEquals(4, list.get(3).id);
    }

    static class AClass extends IdentityBean<Integer> {
        private int id;

        AClass() {
        }

        AClass(int id) {
            this.id = id;
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public void setId(Integer id) {
            this.id = id;
        }
    }
}