package org.opentripplanner.netex.mapping;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.netex.mapping.DayTypeRefToServiceIdMapper.generateServiceId;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (01.12.2017)
 */
public class DayTypeRefToServiceIdMapperTest {

    @Test
    public void mapToServiceId() {
        assertEquals("A", generateServiceId(Collections.singletonList("A")));
        assertEquals("A+B", generateServiceId(Arrays.asList("A", "B")));
        assertNull(generateServiceId(Collections.emptyList()));

    }
}