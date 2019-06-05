package org.opentripplanner.netex.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NetexObjectDecoratorTest {
    private static final String OK = "OK";

    private String mapCalled = null;

    @Test public void ifOptionalElementExistThenAssertHandlerIsCalled() {
        NetexObjectDecorator.withOptional(OK, t -> mapCalled = t);
        assertEquals(OK, mapCalled);
    }

    @Test public void doNotCallHandlerIfOptionalValueIsNull() {
        NetexObjectDecorator.withOptional(null, t -> fail("Should not be called if arg is null"));
    }
}