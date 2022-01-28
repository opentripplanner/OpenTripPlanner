package org.opentripplanner.common.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class T2Test {

    @Test
    void testEquals() {
        var subject = new T2<>("Alf", 1);

        assertEquals(new T2<>("Alf", 1), subject);
        assertEquals(new T2<>("Alf", 1).hashCode(), subject.hashCode());

        // first is different
        assertNotEquals(new T2<>("Alfi", 1), subject);

        // second is different
        assertNotEquals(new T2<>("Alf", 2), subject);

        // Different types, should not fail with exception
        assertNotEquals(new T2<>(1, "Alf"), subject);
    }

    @Test
    void testToString() {
        var subject = new T2<>("Alf", 1);
        assertEquals("T2(Alf, 1)", subject.toString());
    }
}