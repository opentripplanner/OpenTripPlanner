package org.opentripplanner.util;

import org.junit.Test;

import java.util.Collections;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.util.MapUtils.mapToList;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (30.10.2017)
 */
public class MapUtilsTest {
    @Test
    public void mapToListTest() throws Exception {
        assertNull(mapToList(null, identity()));
        assertTrue(mapToList(Collections.emptyList(), identity()).isEmpty());
        assertEquals(singletonList(5), mapToList(singleton(5), identity()));
    }
}