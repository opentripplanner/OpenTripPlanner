package org.opentripplanner.graph_builder.impl.raptor;

import java.util.Arrays;
import java.util.Iterator;

/**
 * This is a totally unsafe way to use an array to implement an unordered, non-unique 
 * container type.  It should be very fast.
 * 
 * @author novalis
 */
public class ArrayBag<T> implements Iterable<T> {
    T[] items;
    int last;
    protected ArrayBagIterator iterator = new ArrayBagIterator();

    class ArrayBagIterator implements Iterator<T> {
        private int nextIndex;
        void reset() {
            nextIndex = 0;
        }

        public boolean hasNext() {
            return nextIndex < last;
        }

        public T next () {
            if (nextIndex >= last) {
                throw new RuntimeException("Iteration should be over");
            }
            return items[nextIndex++];
        }

        public void remove() {
            if (nextIndex == last) {
                last -= 1;
            } else {
                items[--nextIndex] = items[--last];
            }
            items[last] = null;
        }
    }

    public int size() {
        return last;
    }
    
    @SuppressWarnings("unchecked")
    ArrayBag() {
        items = (T[]) new Object[2];
        last = 0;
    }

    public void add(T e) {
        if (last == items.length) {
            items = Arrays.copyOf(items, items.length * 2);
        }
        items[last] = e;
        last += 1;
    }

    public ArrayBagIterator iterator() {
        iterator.nextIndex = 0;
        return iterator;
    }

    public T item() {
        if (last == 0)
            throw new UnsupportedOperationException();
        return items[last - 1];
    }

    public boolean isEmpty() {
        return last == 0;
    }

}