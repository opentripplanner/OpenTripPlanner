package org.opentripplanner.netex.loader.util;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.netex.loader.util.E.EASTWOOD;
import static org.opentripplanner.netex.loader.util.E.REAGAN;
import static org.opentripplanner.netex.loader.util.E.REAGAN_2;
import static org.opentripplanner.netex.loader.util.E.REAGAN_3;
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

    @Test public void isLatestVersionWithNoElements() {
        // Any element is the latest version if the collection is empty
        assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN));
    }


    @Test public void isLatestVersionWithOneRootElement() {
        // Given one element in the root
        root.add(REAGAN_2);

        // Then expect an older element to return false
        assertFalse(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN));

        // And expect the same element to be true (equals)
        assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_2));

        // And expect a newer element (same id) to be true
        assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_3));

        // And expect an element with none existing id (map key) to return true
        assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(SCHWARZENEGGER));
    }

    @Test public void isLatestVersionWithTwoRootElementsAndOneChild() {
        // Given two versions in root and one in the child
        root.add(REAGAN_2);
        root.add(REAGAN_3);
        child.add(REAGAN_2);

        // At root then expect version 3, but not version 2 to return true
        assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_3));
        assertFalse(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_2));

        // At the child on the other hand only version 2 exist, so we
        // expect version 2, but not version 1 to return true
        assertTrue(child.isNewerOrSameVersionComparedWithExistingValues(REAGAN_2));
        assertFalse(child.isNewerOrSameVersionComparedWithExistingValues(REAGAN));
    }
}