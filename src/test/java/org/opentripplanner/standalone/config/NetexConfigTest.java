package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

public class NetexConfigTest {

    @Test
    public void testDefaultPatternMatchers() {
        NetexConfig subject = new NetexConfig(
                new NodeAdapter(MissingNode.getInstance(), "NetexParametersTest")
        );

        assertTrue(subject.ignoreFilePattern.matcher("").matches());
        assertTrue(subject.sharedFilePattern.matcher("shared-data.xml").matches());
        assertTrue(subject.sharedGroupFilePattern.matcher("RUT-anything-shared.xml").matches());
        assertTrue(subject.groupFilePattern.matcher("RUT-anything.xml").matches());
        assertEquals("DefaultFeed", subject.netexFeedId);
    }

    @Test
    public void testLoadingConfigAndPatternMatchers() throws IOException {
        NodeAdapter nodeAdapter = newNodeAdapterForTest(
                "{\n" +
                "    'moduleFilePattern' : 'netex_.*\\\\.zip',\n" +
                "    'ignoreFilePattern' : '(__.*|\\\\..*)',\n" +
                "    'sharedFilePattern' : '_stops.xml',\n" +
                "    'sharedGroupFilePattern' : '_(\\\\w{3})_shared_data.xml',\n" +
                "    'groupFilePattern' : '(\\\\w{3})_.*\\\\.xml',\n" +
                "    'netexFeedId': 'RB'\n" +
                "}"
        );

        NetexConfig subject = new NetexConfig(nodeAdapter);

        assertTrue(subject.ignoreFilePattern.matcher(".ignore").matches());
        assertTrue(subject.ignoreFilePattern.matcher("__ignore").matches());
        assertTrue(subject.sharedFilePattern.matcher("_stops.xml").matches());
        assertTrue(subject.sharedGroupFilePattern.matcher("_RUT_shared_data.xml").matches());
        assertTrue(subject.groupFilePattern.matcher("RUT_anything.xml").matches());
        assertEquals("RB", subject.netexFeedId);
    }
}