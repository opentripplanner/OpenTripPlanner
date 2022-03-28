package org.opentripplanner.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeedScopedIdTest {

    @Test
    void ofNullable() {
        assertEquals(new FeedScopedId("FEED", "ID"), FeedScopedId.ofNullable("FEED", "ID"));
        assertNull(FeedScopedId.ofNullable("FEED", null));
    }
}