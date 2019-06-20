package org.opentripplanner.netex.loader.util;

import com.google.common.collect.Multimap;
import org.rutebanken.netex.model.EntityInVersionStructure;

import java.util.Collection;

import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionedElementIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.versionOf;

/**
 * A hierarchical multimap indexing a collections of {@link EntityInVersionStructure} values by
 * their {@code id}. Use the one argument {@link #add(EntityInVersionStructure)} method to add
 * elements to the map.
 *
 * @param <V> the value type
 */
public class HierarchicalVersionMapById<V extends EntityInVersionStructure>
        extends HierarchicalMultimap<String, V>
        implements ReadOnlyHierarchicalVersionMapById<V> {

    /** Create a root for the hierarchy */
    public HierarchicalVersionMapById() { }

    /** Create a child of the given {@code parent}. */
    public HierarchicalVersionMapById(HierarchicalVersionMapById<V> parent) {
        super(parent);
    }

    /**
     * Add an entity and use its Id as key to index it.
     */
    public void add(V entity) {
        super.add(entity.getId(), entity);
    }

    /** Add all given entities to local map */
    public void addAll(Collection<V> entities) {
        for (V it : entities) {
            add(it);
        }
    }

    /**
     * Use the {@link #add(EntityInVersionStructure)} method!
     * @throws IllegalArgumentException This method throws an exception to prevent adding
     *                                  elements with a key different than the element id.
     */
    @Override
    public void add(String key, V value) {
        throw new IllegalArgumentException("Use the add method with just one argument instead.");
    }


    @Override
    // We need to override this method because the super method uses the the #add(Strinng, V)
    // method - which throws an exception.
    public void addAll(Multimap<String, V> other) {
        throw new IllegalArgumentException("Use the add method with just one argument instead.");
    }

    @Override
    public V lookupLastVersionById(String id) {
        return latestVersionedElementIn(lookup(id));
    }

    /**
     * Return {@code true} if the given {@code value.version} is larger or equals to all the
     * maximum version of all elements in the collection returned using the
     * {@link #lookup(Object)} method.
     * <p/>
     * Note! This method do not check all values in the hierarchy, only the elements
     * in the first collection found.
     */
    public boolean isNewerOrSameVersionComparedWithExistingValues(V value) {
        return versionOf(value) >= latestVersionIn(lookup(value.getId()));
    }
}
