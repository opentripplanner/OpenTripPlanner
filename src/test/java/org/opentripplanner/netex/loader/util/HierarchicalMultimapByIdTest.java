package org.opentripplanner.netex.loader.util;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.netex.loader.util.E.EASTWOOD;
import static org.opentripplanner.netex.loader.util.E.REAGAN;
import static org.opentripplanner.netex.loader.util.E.REAGAN_2;
import static org.opentripplanner.netex.loader.util.E.SCHWARZENEGGER;
import static org.opentripplanner.netex.loader.util.SetSupport.listOf;
import static org.opentripplanner.netex.loader.util.SetSupport.sort;

public class HierarchicalMultimapByIdTest {
    private final HierarchicalMultimapById<E> root = new HierarchicalMultimapById<>();
    private final HierarchicalMultimapById<E> child = new HierarchicalMultimapById<>(root);

    /**
     * Add entity using the one argument add method verify that it can be retrieved
     * using its id from the map.
     */
    @Test public void addEntityWithOneArgument() {
        // Given one element added
        child.add(EASTWOOD);

        // Then expect the element to be retrieved by its id
        assertEquals(Collections.singletonList(EASTWOOD), child.lookup(EASTWOOD.getId()));

        // And expect an empty collection if no element exist with such a key
        assertTrue(child.lookup(SCHWARZENEGGER.getId()).isEmpty());
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
        assertEquals(listOf(SCHWARZENEGGER), child.lookup(SCHWARZENEGGER.getId()));
    }

    @Test
    public void lookupVersionedElement() {
        // Given to versions of the same element added
        root.add(REAGAN);
        root.add(REAGAN_2);

        // When
        E latestVersion = child.lookupLastVersionById(REAGAN.getId());

        // Then
        assertEquals(REAGAN_2, latestVersion);
        assertNotEquals(REAGAN, latestVersion);

        // Also verify all Reagans are present in the collection
        assertEquals(
                sort(listOf(REAGAN, REAGAN_2)),
                sort(child.lookup(REAGAN.getId()))
        );
    }
}