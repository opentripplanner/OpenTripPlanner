package org.opentripplanner.netex.mapping;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MappingUtilsTest {
    private static final String OK = "OK";

    private String mapCalled = null;

    @Test public void mapOptional() {
        MappingUtils.mapOptional(OK, t -> mapCalled = t);
        assertEquals(OK, mapCalled);
    }

    @Test public void doNotMapOptionalWhenValueIsNull() {
        MappingUtils.mapOptional(null, t -> fail("Should not be called if arg is null"));
    }
}