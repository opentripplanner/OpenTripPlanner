package org.opentripplanner.netex.loader.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.opentripplanner.netex.loader.util.E.EASTWOOD;
import static org.opentripplanner.netex.loader.util.E.SCHWARZENEGGER;

/**
 * This test extends the tests performed in {@link HierarchicalMapTest} with respect to
 * the few extra features that is added in the {@link HierarchicalMapById} class.
 */
public class HierarchicalMapByIdTest {

    private final HierarchicalMapById<E> root = new HierarchicalMapById<>();
    private final HierarchicalMapById<E> child = new HierarchicalMapById<>(root);

    /**
     * Add entity using the one argument add method verify that it can be retrieved
     * using its id from the map.
     */
    @Test public void addEntityWithOneArgument() {
        // Given one element added
        child.add(EASTWOOD);
        // The expect that element to be retrieved by its id
        assertEquals(EASTWOOD, child.lookup(EASTWOOD.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEntityWithIllegalKeyArgument() {
        // Prevent using add method with (key, value) -> the key is given: the ID.
        child.add("Illegal key", EASTWOOD);
        fail("Expected an exception, but did not get one.");
    }

    @Test
    public void addToRootAndRetrieveFromChild() {
        root.add(SCHWARZENEGGER);
        assertEquals(SCHWARZENEGGER, child.lookup(SCHWARZENEGGER.getId()));
    }
}