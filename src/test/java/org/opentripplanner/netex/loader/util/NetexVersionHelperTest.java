package org.opentripplanner.netex.loader.util;

import org.junit.Test;
import org.rutebanken.netex.model.EntityInVersionStructure;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NetexVersionHelperTest {

    private static final EntityInVersionStructure E_VER_1 =
            new EntityInVersionStructure().withVersion("1");
    private static final EntityInVersionStructure E_VER_2 =
            new EntityInVersionStructure().withVersion("2");

    @Test public void versionOf() {
        assertEquals(1, NetexVersionHelper.versionOf(E_VER_1));
    }

    @Test public void latestVersion() {
        assertEquals(2, NetexVersionHelper.latestVersionIn(Arrays.asList(E_VER_1, E_VER_2)));
        assertEquals(-1, NetexVersionHelper.latestVersionIn(Collections.emptyList()));
    }

    @Test public void lastestElement() {
        assertEquals(E_VER_2, NetexVersionHelper.lastestVersionedElementIn(Arrays.asList(E_VER_1, E_VER_2)));
        assertNull(NetexVersionHelper.lastestVersionedElementIn(Collections.emptyList()));
    }
}