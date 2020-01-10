package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NetexParametersTest {

    @Test
    public void testDefaultPatternMatchers() {
        NetexParameters subject = new NetexParameters(null);

        assertTrue(subject.ignoreFilePattern.matcher("").matches());
        assertTrue(subject.sharedFilePattern.matcher("shared-data.xml").matches());
        assertTrue(subject.sharedGroupFilePattern.matcher("RUT-anything-shared.xml").matches());
        assertTrue(subject.groupFilePattern.matcher("RUT-anything.xml").matches());
        assertEquals("DefaultFeed", subject.netexFeedId);
    }

    @Test
    public void testLoadingConfigAndPatternMatchers() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        String configText = "{\n" +
                "    'moduleFilePattern' : 'netex_.*\\\\.zip',\n" +
                "    'ignoreFilePattern' : '(__.*|\\\\..*)',\n" +
                "    'sharedFilePattern' : '_stops.xml',\n" +
                "    'sharedGroupFilePattern' : '_(\\\\w{3})_shared_data.xml',\n" +
                "    'groupFilePattern' : '(\\\\w{3})_.*\\\\.xml',\n" +
                "    'netexFeedId': 'RB'\n" +
                "}";
        // Replace ' with " and % with \\\\ (double escape char)
        configText = configText.replace("'", "\"");


        JsonNode config = mapper.readTree(configText);


        NetexParameters subject = new NetexParameters(config);

        assertTrue(subject.ignoreFilePattern.matcher(".ignore").matches());
        assertTrue(subject.ignoreFilePattern.matcher("__ignore").matches());
        assertTrue(subject.sharedFilePattern.matcher("_stops.xml").matches());
        assertTrue(subject.sharedGroupFilePattern.matcher("_RUT_shared_data.xml").matches());
        assertTrue(subject.groupFilePattern.matcher("RUT_anything.xml").matches());
        assertEquals("RB", subject.netexFeedId);

    }
}