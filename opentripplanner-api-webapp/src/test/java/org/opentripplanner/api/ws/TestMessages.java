package org.opentripplanner.api.ws;

import java.util.Locale;

import junit.framework.TestCase;

public class TestMessages extends TestCase {

    public void testRelativeDirection() {

        String e = Message.GEOCODE_FROM_AMBIGUOUS.get();
        String f = Message.GEOCODE_FROM_AMBIGUOUS.get(Locale.CANADA_FRENCH);
        String s = Message.GEOCODE_FROM_AMBIGUOUS.get(new Locale("es"));

        TestCase.assertNotNull(e);
        TestCase.assertNotNull(f);
        TestCase.assertNotNull(s);
        TestCase.assertNotSame(e, f);
        TestCase.assertNotSame(e, s);
        TestCase.assertNotSame(f, s);
    }
}
