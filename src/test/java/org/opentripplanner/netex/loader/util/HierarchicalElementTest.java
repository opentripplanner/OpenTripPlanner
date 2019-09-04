package org.opentripplanner.netex.loader.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HierarchicalElementTest {
    private static final String HELLO_MY_KING = "Hello my king";
    private static final String HELLO_MY_FRIEND = "Hello my friend";

    // Create a parent - child hierarchy
    private final HierarchicalElement<String> parent = new HierarchicalElement<>();
    private final HierarchicalElement<String> child = new HierarchicalElement<>(parent);

    /**
     * The initial state is expected to be empty.
     */
    @Test public void emptyInitialState() {
        // Assert parent and child is initially empty
        assertNull(parent.get());
        assertNull(child.get());
    }

    /**
     * A child value should only be accessible from the child instance not from the parent instance.
     */
    @Test public void childValueAccessibleToChildOnly() {
        // Add something to the child
        child.set(HELLO_MY_FRIEND);

        // Verify it is ONLY
        // accessible using the child instance
        assertNull(parent.get());
        assertEquals(HELLO_MY_FRIEND, child.get());
    }

    /**
     * When a child and parent have different values the accessor should reflect that.
     */
    @Test public void childAndParentAccess() {
        // Add something to child and something else to the parent
        parent.set(HELLO_MY_KING);
        child.set(HELLO_MY_FRIEND);

        // Verify both have the right value set.
        assertEquals(HELLO_MY_KING, parent.get());
        assertEquals(HELLO_MY_FRIEND, child.get());
    }

    /**
     * When a child do not have a value it should provide the parent value instead.
     * Hence parent and child should return the same value.
     */
    @Test public void emptyChildReturnsParentValue() {
        // Add something to the parent
        parent.set(HELLO_MY_KING);

        // Both child and parent is now returning the parent value
        assertEquals(parent.get(), child.get());
    }
}