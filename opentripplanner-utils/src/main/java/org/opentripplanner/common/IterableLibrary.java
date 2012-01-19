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

package org.opentripplanner.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IterableLibrary {

    /**
     * Returns a filtered {@link Iterable}, where elements that are not assignable to the target
     * type are excluded from the iterable.
     * 
     * @param <T>
     * @param iterable
     * @param type
     * @return
     */
    public static <T> Iterable<T> filter(final Iterable<?> iterable, final Class<T> type) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new IteratorFilterImpl<T>(iterable.iterator(), type);
            }
        };
    }

    /**
     * Returns a filtered {@link Iterable}, where elements are cast to the target type and a
     * {@link ClassCastException} is thrown if the object is not assignable to the target type.
     * 
     * @param <T>
     * @param iterable
     * @param type
     * @return
     */
    public static <T> Iterable<T> cast(final Iterable<?> iterable) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new IteratorCastImpl<T>(iterable.iterator());
            }
        };
    }

    public static <T> Iterable<T> cast(final Iterable<?> iterable, Class<T> type) {
        return cast(iterable);
    }

    /****
     * Private Methods
     ****/

    private static class IteratorFilterImpl<T> implements Iterator<T> {

        private final Iterator<?> _iterator;

        private final Class<T> _type;

        private T _next = null;

        public IteratorFilterImpl(Iterator<?> iterator, Class<T> type) {
            _iterator = iterator;
            _type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean hasNext() {
            while (_next == null && _iterator.hasNext()) {
                Object obj = _iterator.next();
                if (obj != null && _type.isAssignableFrom(obj.getClass()))
                    _next = (T) obj;
            }
            return _next != null;
        }

        @Override
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();
            T next = _next;
            _next = null;
            return next;
        }

        @Override
        public void remove() {
            _iterator.remove();
        }
    }

    private static class IteratorCastImpl<T> implements Iterator<T> {

        private final Iterator<?> _iterator;

        public IteratorCastImpl(Iterator<?> iterator) {
            _iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return _iterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            return (T) _iterator.next();
        }

        @Override
        public void remove() {
            _iterator.remove();
        }
    }
}
