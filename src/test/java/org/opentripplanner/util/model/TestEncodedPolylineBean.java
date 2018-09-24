package org.opentripplanner.util.model;

import org.opentripplanner.util.model.EncodedPolylineBean;

import junit.framework.TestCase;

public class TestEncodedPolylineBean extends TestCase {

    public void testEncodedPolylineBean() {
        // test taken from an example usage
        EncodedPolylineBean eplb = new EncodedPolylineBean("wovwF|`pbMgPzg@CHEHCFEFCFEFEFEDGDEDEDGBEBGDG@", null, 17);
        assertEquals("wovwF|`pbMgPzg@CHEHCFEFCFEFEFEDGDEDEDGBEBGDG@", eplb.getPoints());
        assertNull(eplb.getLevels());
        assertEquals(17, eplb.getLength());
    }
}
