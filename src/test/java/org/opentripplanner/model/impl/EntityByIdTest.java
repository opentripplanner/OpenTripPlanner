package org.opentripplanner.model.impl;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitEntity;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntityByIdTest {

    public static final String FEED_ID = "F";
    private static final FeedScopedId ID = new FeedScopedId(FEED_ID, "99");
    private static final FeedScopedId FAKE_ID = new FeedScopedId(FEED_ID, "77");

    private static final EntityByIdTest.E E = new E(ID);
    private static final String E_TO_STRING = E.toString();
    private static final String LIST_OF_E_TO_STRING = String.format("[%s]", E_TO_STRING);
    private static final String MAP_OF_E_TO_STRING = String.format("{%s=%s}", ID, E_TO_STRING);

    private final EntityById<E> subject = new EntityById<>();


    @Test public void add() {
        subject.add(E);
        assertEquals(MAP_OF_E_TO_STRING, subject.toString());
    }

    @Test public void addAll() {
        subject.addAll(Collections.emptyList());
        assertEquals("{}", subject.toString());
        subject.addAll(Collections.singletonList(E));
        assertEquals(MAP_OF_E_TO_STRING, subject.toString());
    }

    @Test public void values() {
        subject.add(E);
        assertEquals(LIST_OF_E_TO_STRING, subject.values().toString());
    }

    @Test public void get() {
        subject.add(E);
        assertEquals(E_TO_STRING, subject.get(ID).toString());
    }

    @Test public void asImmutableMap() {
        subject.add(E);
        assertEquals(E_TO_STRING, subject.get(ID).toString());
    }

    @Test public void containsKey() {
        assertFalse(subject.containsKey(ID));
        subject.add(E);
        assertTrue(subject.containsKey(ID));
        assertFalse(subject.containsKey(FAKE_ID));
    }

    static class E extends TransitEntity {
        E(FeedScopedId id) {
            super(id);
        }
        @Override public String toString() {
            return "E-" + getId();
        }
    }
}