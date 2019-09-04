package org.opentripplanner.netex.support;

import org.junit.Test;
import org.rutebanken.netex.model.EntityInVersionStructure;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.netex.support.NetexVersionHelper.comparingVersion;
import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionedElementIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.versionOf;

public class NetexVersionHelperTest {

    private static final EntityInVersionStructure E_VER_1 =
            new EntityInVersionStructure().withVersion("1");
    private static final EntityInVersionStructure E_VER_2 =
            new EntityInVersionStructure().withVersion("2");

    @Test public void versionOfTest() {
        assertEquals(1, versionOf(E_VER_1));
    }

    @Test public void latestVersionInTest() {
        assertEquals(2, latestVersionIn(Arrays.asList(E_VER_1, E_VER_2)));
        assertEquals(-1, latestVersionIn(Collections.emptyList()));
    }

    @Test public void lastestVersionedElementInTest() {
        assertEquals(E_VER_2, latestVersionedElementIn(Arrays.asList(E_VER_1, E_VER_2)));
        assertNull(latestVersionedElementIn(Collections.emptyList()));
    }

    @Test public void comparingVersionTest() {
        // Given a comparator (subject under test)
        Comparator<EntityInVersionStructure> subject = comparingVersion();
        // And a entity with version as the E_VER_1 entity
        EntityInVersionStructure sameVersionAs_E_VER_1 = new EntityInVersionStructure().withVersion("1");

        // Then expect equals versions to return zero
        assertEquals(0, subject.compare(E_VER_1, sameVersionAs_E_VER_1));

        // Then expect lesser version to return less than zero
        assertTrue(subject.compare(E_VER_1, E_VER_2) < 0);

        // Then expect higher version to return more than zero
        assertTrue(subject.compare(E_VER_2, E_VER_1) > 0);
    }

}