package org.opentripplanner.common.model;

/**
 * An ordered pair of objects of the same type
 *
 * @param <E>
 */
public class P2<E> extends T2<E, E> {

    private static final long serialVersionUID = 1L;

    public static <E> P2<E> createPair(E first, E second) {
        return new P2<E>(first, second);
    }

    public P2(E first, E second) {
        super(first, second);
    }
    
    public P2(E[] entries) {
        super(entries[0], entries[1]);
        if (entries.length != 2) {
            throw new IllegalArgumentException("This only takes arrays of 2 arguments");
        }
    }

    public String toString() {
        return "P2(" + first + ", " + second + ")";
    }
}
