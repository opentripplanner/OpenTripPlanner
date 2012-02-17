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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/** Treat multiple lists as a single list */
public class JoinedList<E> implements List<E> {

    private List<E>[] lists;
    private int totalSize;

    public JoinedList(List<E> ... lists) {
        this.lists = lists;
        totalSize = 0;
        for (List<E> list : lists) {
            totalSize += list.size();
        }
    }

    @Override
    public boolean add(E arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int arg0, E arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends E> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object needle) {
        for (List<E> list : lists) {
            if (list.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> needles) {
        for (Object needle : needles) {
            if (!contains(needle)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public E get(int i) {
        for (List<E> list : lists) {
            if (i < list.size()) {
                return list.get(i);
            }
            i -= list.size();
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }

    @Override
    public int indexOf(Object needle) {
        int i = 0;
        for (List<E> list : lists) {
            int index = list.indexOf(needle);
            if (index >= 0) {
                return index + i;
            }
            i += list.size();
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        for (List<E> list : lists) {
            if (!list.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<E> iterator() {
        return new JoinedListIterator();
    }
    
    class JoinedListIterator implements ListIterator<E> {

        private int listIndex;
        private ListIterator<E> iterator;
        JoinedListIterator(){
            listIndex = 0;
        }
        
        @Override
        public boolean hasNext() {
            if (iterator == null) {
                iterator = lists[listIndex].listIterator();
            }
            while (!iterator.hasNext()) {
                listIndex += 1;
                if (listIndex == lists.length) {
                    return false;
                }
                iterator = lists[listIndex].listIterator();
            }
            return listIndex != lists.length;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (iterator == null) {
                iterator = lists[listIndex].listIterator();
            }
            while (!iterator.hasNext()) {
                listIndex += 1;
                if (listIndex == lists.length) {
                    throw new NoSuchElementException();
                }
                iterator = lists[listIndex].listIterator();
            }
            return iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasPrevious() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextIndex() {
            int nextIndex = 0;
            for (int i = 0; i < listIndex; ++i) {
                nextIndex += lists[i].size();
            }
            return nextIndex + iterator.nextIndex();
        }

        @Override
        public E previous() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int previousIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E arg0) {
            throw new UnsupportedOperationException();
        }
        
    }

    @Override
    public int lastIndexOf(Object needle) {
        int i = totalSize;
        for (List<E> list : lists) {
            i -= list.size();
            int index = list.lastIndexOf(needle);
            if (index >= 0) {
                return i + index;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        return new JoinedListIterator();
    }

    @Override
    public ListIterator<E> listIterator(int arg0) {
        //fixme: this is slow and stupid
        JoinedListIterator result = new JoinedListIterator();
        for (int i = 0; i < arg0; ++i) {
            result.next();
        }
        return result;
    }

    @Override
    public boolean remove(Object arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E set(int arg0, E arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return totalSize;
    }

    @Override
    public List<E> subList(int arg0, int arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[totalSize];
        Iterator<E> i = iterator();
        int p = 0;
        while (i.hasNext()) {
            array[p++] = i.next();
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] out) {
        if (out.length < totalSize) {
            out = (T[]) Arrays.copyOf(out, totalSize, out.getClass());
        }
        Iterator<E> i = iterator();
        int p = 0;
        while (i.hasNext()) {
            out[p++] = (T) i.next();
        }
        if (out.length > totalSize) {
            out[p] = null;
        }
        return out;
    }
}
