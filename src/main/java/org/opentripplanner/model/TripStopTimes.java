package org.opentripplanner.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A multimap from Trip to a sorted list of StopTimes.
 * <p/>
 * The list of stop times  for a given trip is guarantied to be sorted.
 */
public class TripStopTimes {
    private static final List<StopTime> EMPTY_LIST = Collections.emptyList();

    private Map<Trip, List<StopTime>> map = new HashMap<>();

    /**
     * Return a unmodifiable, nullsafe list of stop times for the given trip.
     * An <em>empty</em>empty list is returned if no values exist for a given key.
     */
    public List<StopTime> get(Trip key) {
        List<StopTime> list = map.get(key);
        return list == null ? EMPTY_LIST : Collections.unmodifiableList(list);
    }

    public void addAll(Collection<StopTime> values) {
        Set<Trip> keysUpdated = new HashSet<>();
        for (StopTime value : values) {
            Trip key = value.getTrip();
            keysUpdated.add(key);
            map.computeIfAbsent(key, trip -> new ArrayList<>()).add(value);
        }
        // Sort and updated stops for all keys touched.
        for (Trip key : keysUpdated) {
            Collections.sort(map.get(key));
        }
    }

    public void replace(Trip key, Collection<StopTime> list) {
        map.replace(key, sort(list));
    }

    /**
     * Return a copy of the internal map. Changes in the source are not reflected
     * in the destination (returned Map), and visa versa.
     * <p/>
     * The returned map is immutable.
     */
    public Map<Trip, List<StopTime>> asImmutableMap() {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    public int size() {
        return map.size();
    }

    /**
     * Return a iterable set of keys. Please do not remove keys the effect is undefined.
     */
    public Iterable<Trip> keys() {
        return map.keySet();
    }

    /**
     * The Trip is mutable (the id may change). If the #hashCode changes the map must be
     * reindex to work properly.
     */
    public void reindex() {
        map = new HashMap<>(map);
    }


    /* private methods */

    private static List<StopTime> sort(Collection<StopTime> list) {
        List<StopTime> values = new ArrayList<>(list);
        Collections.sort(values);
        return values;
    }

    /**
     * Return a all values for all trips merged into a set.
     */
    public Set<StopTime> valuesAsSet() {
        return map.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
