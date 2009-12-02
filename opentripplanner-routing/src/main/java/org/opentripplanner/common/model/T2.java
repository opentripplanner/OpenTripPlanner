package org.opentripplanner.common.model;

import java.io.Serializable;

public class T2<E1, E2> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final E1 _first;

    private final E2 _second;

    public T2(E1 first, E2 second) {
        _first = first;
        _second = second;
    }

    public E1 getFirst() {
        return _first;
    }

    public E2 getSecond() {
        return _second;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_first == null) ? 0 : _first.hashCode());
        result = prime * result + ((_second == null) ? 0 : _second.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        T2<?, ?> other = (T2<?, ?>) obj;
        if (_first == null) {
            if (other._first != null)
                return false;
        } else if (!_first.equals(other._first))
            return false;
        if (_second == null) {
            if (other._second != null)
                return false;
        } else if (!_second.equals(other._second))
            return false;
        return true;
    }
}
