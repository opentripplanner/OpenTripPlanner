package org.opentripplanner.common.model;

public class P2<E> extends T2<E, E> {

    private static final long serialVersionUID = 1L;

    public static <E> P2<E> createPair(E first, E second) {
        return new P2<E>(first, second);
    }

    public P2(E first, E second) {
        super(first, second);
    }
}
