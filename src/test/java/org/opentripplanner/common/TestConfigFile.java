package org.opentripplanner.common;

import junit.framework.TestCase;
import org.junit.Test;

import javax.validation.constraints.AssertTrue;
import java.util.Arrays;
import java.util.prefs.Preferences;

/**
 * Created by abyrd on 10/09/14.
 */
public class TestConfigFile extends TestCase {

    private static String CONFIG1 = "src/test/resources/test1.config";

    @Test
    public void testLoadPreferences() throws Exception {
        OTPConfigPreferences config = OTPConfigPreferences.fromFile(CONFIG1);
        // System.out.println(config.toString());
        assertTrue(config.nodeExists("/origins/filters/graphGeographicFilter"));
        assertTrue (config.nodeExists("/request"));
        assertFalse(config.nodeExists("/origins/filters/badFilter"));
        String val;
        Preferences node;
        node = config.node("/origins/filters/graphGeographicFilter");
        assertTrue(node.getBoolean("stopsOnly", false));
        assertTrue(node.get("stopsOnly", "default").equals("true"));
        assertTrue(node.get("nothing", "default").equals("default"));
        node = config.node("batch");
        assertEquals(node.getInt("threshold", 999), 3000);
        assertEquals(node.getDouble("pi", Math.PI), Math.PI);
        node = config.node("origins/filters");
        assertEquals("/origins/filters", node.absolutePath());
        node = config.node("/request");
        assertTrue(Arrays.asList(node.keys()).contains("routerId"));
        assertFalse(Arrays.asList(node.keys()).contains("missingThing"));
    }

}
