package org.opentripplanner.model.impl;

import org.opentripplanner.model.IdentityBean;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EntityMap<I extends Serializable, E extends IdentityBean<I>> {

    private final Map<I, E> map = new HashMap<>();

    public void add(E entity) {
        map.put(entity.getId(), entity);
    }

    public void addAll(Collection<E> routes) {
        routes.forEach(this::add);
    }

    /** Delegates to {@link java.util.Map#values()} */
    public Collection<E> values() {
        return map.values();
    }

    public E get(I id) {
        return map.get(id);
    }

    /**
     * Return a copy of the internal map. Changes in source (EntityMap) are not reflected
     * in the destination (returned Map), and visa versa.
     */
    public Map<I, E> asMap() {
        return new HashMap<>(map);
    }

    public boolean containsKey(I id) {
        return map.containsKey(id);
    }

    @Override
    public String toString() {
        return map.toString();
    }

    public void reindex() {
        HashMap<I, E> temp = new HashMap<>(map);
        map.clear();
        map.putAll(temp);
    }
}
