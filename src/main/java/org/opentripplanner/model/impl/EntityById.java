package org.opentripplanner.model.impl;

import org.opentripplanner.model.TransitEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to provide a map from id to the corresponding entity.
 * It is simply an index of entities.
 *
 * @param <I> the entity id type
 * @param <E> the entity type
 */
public class EntityById<I extends Serializable, E extends TransitEntity<I>> {

    private final Map<I, E> map = new HashMap<>();

    public void add(E entity) {
        map.put(entity.getId(), entity);
    }

    public void addAll(Collection<E> entities) {
        entities.forEach(this::add);
    }

    /** Delegates to {@link java.util.Map#values()} */
    public Collection<E> values() {
        return map.values();
    }

    /**
     * @param id the id whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    public E get(I id) {
        return map.get(id);
    }

    /**
     * Returns the number of key-value mappings in this map.
     */
    public int size() {
        return map.size();
    }

    public boolean containsKey(I id) {
        return map.containsKey(id);
    }

    @Override
    public String toString() {
        return map.toString();
    }

    int removeIf(Predicate<E> test) {
        Collection<E> newSet = map
                .values()
                .stream()
                .filter(Predicate.not(test))
                .collect(Collectors.toList());

        int size = map.size();
        if(newSet.size() == size) {
            return 0;
        }
        map.clear();
        addAll(newSet);
        return size - map.size();
    }

    /**
     * Return a copy of the internal map. Changes in the source are not reflected
     * in the destination (returned Map), and visa versa.
     * <p>
     * The returned map is immutable.
     */
    Map<I, E> asImmutableMap() {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

}
