package org.opentripplanner.transit.raptor.util.paretoset;

import java.util.Iterator;
import java.util.stream.Stream;


/**
 * {@link ParetoSet} with the possibility to set a index marker, which
 * can be used to list all elements added after the marker is set.
 *
 *
 * @param <T> the element type
 */
public class ParetoSetWithMarker<T> extends ParetoSet<T> {
    private int marker = 0;

    public ParetoSetWithMarker(ParetoComparator<T> comparator) {
        super(comparator);
    }

    public ParetoSetWithMarker(ParetoComparator<T> comparator, ParetoSetEventListener<? super T> eventListener) {
        super(comparator, eventListener);
    }

    @Override
    public void clear() {
        super.clear();
        marker = 0;
    }

    /**
     * Create a stream for all elements added after the marker.
     */
    public Stream<T> streamAfterMarker() {
        return stream(marker);
    }

    public Iterable<T> tailAfterMarker() {
        return iterableArray(copyArray(marker));
    }

    /**
     * Move the marker after the last element in the set.
     */
    public void markAtEndOfSet() {
        marker = size();
    }

    @Override
    protected void notifyElementMoved(int fromIndex, int toIndex) {
        if(fromIndex == marker) {
            marker = toIndex;
        }
    }

    public static<T> Iterable<T> iterableArray(T[] array) {
        return () -> new Iterator<>() {
            private int pos=0;

            public boolean hasNext() {
                return array.length>pos;
            }

            public T next() {
                return array[pos++];
            }

            public void remove() {
                throw new UnsupportedOperationException("Cannot remove an element of an array.");
            }
        };
    }
}
