package org.opentripplanner.netex.loader.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.netex.loader.util.E.EASTWOOD;
import static org.opentripplanner.netex.loader.util.E.SCHWARZENEGGER;
import static org.opentripplanner.netex.loader.util.SetSupport.listOf;
import static org.opentripplanner.netex.loader.util.SetSupport.setOf;
import static org.opentripplanner.netex.loader.util.SetSupport.sort;

/**
 * This test extends the tests performed in {@link HierarchicalMapTest} with respect to
 * the few extra features that is added in the {@link HierarchicalMapById} class.
 */
public class HierarchicalMapByIdTest {

    private final HierarchicalMapById<E> root = new HierarchicalMapById<>();
    private final HierarchicalMapById<E> child = new HierarchicalMapById<>(root);

    @Test public void testAddAllAndLocalMethods() {
        // Given
        HierarchicalMapById<E> subject = new HierarchicalMapById<>();
        String eId = EASTWOOD.getId();
        String sId = SCHWARZENEGGER.getId();

        // When
        subject.addAll(listOf(EASTWOOD, SCHWARZENEGGER));

        // Then
        assertEquals(setOf(eId, sId),  subject.localKeys());
        assertEquals(EASTWOOD,  subject.localGet(eId));
        assertEquals(sort(listOf(EASTWOOD, SCHWARZENEGGER)),  sort(subject.localValues()));
        assertTrue(subject.localContainsKey(eId));
    }

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

    @Test(expected = IllegalArgumentException.class)
    public void addEntitiesWithIllegalKeyArgument() {
        Map<String, E> input = new HashMap<>();
        input.put("A", EASTWOOD);

        // Prevent using add method with (key, value) -> the key is given: the ID.
        child.addAll(input);
        fail("Expected an exception, but did not get one.");
    }

    @Test
    public void addToRootAndRetrieveFromChild() {
        root.add(SCHWARZENEGGER);
        assertEquals(SCHWARZENEGGER, child.lookup(SCHWARZENEGGER.getId()));
    }
}