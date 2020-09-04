package org.opentripplanner.graph_builder.module;

import org.junit.Test;

public class GtfsFeedIdTest {
    @Test
    public void shouldUseProvidedFeedId() {
        assert(new GtfsFeedId.Builder().id("abcd").build().getId().equals("abcd"));
    }

    @Test
    public void canAutoGenerateFeedId() {
        assert(Integer.valueOf(new GtfsFeedId.Builder().id("").build().getId()) > 0);
    }

    @Test(expected = RuntimeException.class)
    public void throwsErrorOnDuplicateFeedId() {
        new GtfsFeedId.Builder().id("abc123").build();
        new GtfsFeedId.Builder().id("abc123").build();
    }
}