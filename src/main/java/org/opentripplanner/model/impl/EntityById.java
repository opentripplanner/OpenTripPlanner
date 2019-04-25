package org.opentripplanner.model.impl;

import org.opentripplanner.model.IdentityBean;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is to provide a map from id to the corresponding entity.
 * It is simply an index of entities.
 *
 * @param <I> the entity id type
 * @param <E> the entity type
 */
public class EntityById<I extends Serializable, E extends IdentityBean<I>> {

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
     * Return a copy of the internal map. Changes in the source are not reflected
     * in the destination (returned Map), and visa versa.
     * <p/>
     * The returned map is immutable.
     */
    Map<I, E> asImmutableMap() {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    public boolean containsKey(I id) {
        return map.containsKey(id);
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * For Entities with mutable ids the internal map becomes invalid after the ids are changed.
     * So to fix this, this method allows the caller to reindex the internal map.
     */
    void reindex() {
        HashMap<I, E> temp = new HashMap<>(map);
        map.clear();
        map.putAll(temp);
    }
}
