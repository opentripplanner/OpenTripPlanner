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

import org.onebusaway2.gtfs.model.IdentityBean;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Decorate with id generation.
 */
class ArrayListWithIdGeneration<T extends IdentityBean<Integer>> extends ArrayList<T> {
    private int maxId = 0;

    ArrayListWithIdGeneration() { }

    ArrayListWithIdGeneration(Collection<? extends T> c) {
        super(c);
        decorateWithIds(this);
    }

    @Override public boolean add(T t) {
        decorateWithId(t);
        return super.add(t);
    }

    @Override public void add(int index, T element) {
        decorateWithId(element);
        super.add(index, element);
    }

    @Override public boolean addAll(Collection<? extends T> c) {
        decorateWithIds(c);
        return super.addAll(c);
    }

    @Override public boolean addAll(int index, Collection<? extends T> c) {
        decorateWithIds(c);
        return super.addAll(index, c);
    }

    private void decorateWithId(T entity) {
        Integer value = entity.getId();
        if (value == null || value == 0) {
            value = maxId + 1;
            entity.setId(value);
        }
        maxId = Math.max(maxId, value);
    }

    private void decorateWithIds(Collection<? extends T> entities) {
        for (T entity : entities) {
            decorateWithId(entity);
        }
    }
}
