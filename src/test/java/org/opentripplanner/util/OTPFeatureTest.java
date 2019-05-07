package org.opentripplanner.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OTPFeatureTest {


    private OTPFeature subject = OTPFeature.APIBikeRental;
    private Map<OTPFeature, Boolean> backupValues = new HashMap<>();

    @Before
    public void setUp() {
        // OTPFeatures are global, make sure to copy values, and
        // restore them after the test
        for (OTPFeature it : OTPFeature.values()) {
            backupValues.put(it, it.on());
        }
    }

    @After
    public void tearDown() {
        // Restore OTPFeature values
        for (OTPFeature it : OTPFeature.values()) {
            it.set(backupValues.get(it));
        }
    }

    @Test public void on() {
        // If set
        subject.set(true);
        // then expect
        assertTrue(subject.on());
        assertFalse(subject.off());
    }

    @Test public void off() {
        // If set
        subject.set(false);
        // then expect
        assertFalse(subject.on());
        assertTrue(subject.off());
    }

    @Test public void valueAsString() {
        // If set
        subject.set(true);
        // then expect
        assertEquals("on", subject.valueAsString());
        // If set
        subject.set(false);
        // then expect
        assertEquals("off", subject.valueAsString());
    }

    @Test public void allowOTPFeaturesToBeConfigurableFromJSON() throws IOException {
        // Use a mapper to create a JSON configuration
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

        // Given the following config
        JsonNode config = mapper.readTree(
        "{\n"
                + "  features : {\n"
                + "    APIAlertPatcher : 'on',\n"
                + "    APIBikeRental : 'off'\n"
                + "  }\n"
                + "}\n"
        );

        // And features set with opposite value
        OTPFeature.APIAlertPatcher.set(false);
        OTPFeature.APIBikeRental.set(true);

        // And features missing in the config file
        OTPFeature.APIExternalGeocoder.set(true);
        OTPFeature.APIGraphInspectorTile.set(false);

        // When
        OTPFeature.configure(config);

        // Then
        assertTrue(OTPFeature.APIAlertPatcher.on());
        assertTrue(OTPFeature.APIBikeRental.off());
        assertTrue(OTPFeature.APIExternalGeocoder.on());
        assertTrue(OTPFeature.APIGraphInspectorTile.off());
    }
}