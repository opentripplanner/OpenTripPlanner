package org.opentripplanner.standalone.config;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.standalone.config.StorageParameters.uriFromJson;
import static org.opentripplanner.standalone.config.StorageParameters.uriFromString;
import static org.opentripplanner.standalone.config.StorageParameters.uris;

public class StorageParametersTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

    @Test
    public void testUri() {
        assertEquals("file:///path/a.obj",  uriFromString("file", "file:///path/a.obj").toString());
        assertEquals("gs://bucket/path/a.obj",  uriFromString("gsFile", "gs://bucket/path/a.obj").toString());
    }

    @Test
    public void testUriWithNode() throws IOException {
        JsonNode node;

        node = MAPPER.readTree("{ foo : 'gs://bucket/path/a.obj' }");
        assertEquals("gs://bucket/path/a.obj", uriFromJson("foo", node).toString());

        node = MAPPER.readTree("{ }");
        assertNull("Missing node should be null", uriFromJson("foo", node));

        node = MAPPER.readTree("{ 'foo' : '' }");
        assertNull("Missing node should be null", uriFromJson("foo", node));
    }

    @Test
    public void testUris() throws IOException {
        JsonNode node = MAPPER.readTree("{ foo : [] }");
        assertEquals("[]", uris("foo", node).toString());

        node = MAPPER.readTree("{ foo : ['gs://a/b', 'gs://c/d'] }");
        assertEquals("[gs://a/b, gs://c/d]", uris("foo", node).toString());
    }

    @Test
    public void testCreateGoogleCloudStorageParameters() throws IOException {
        JsonNode node = MAPPER.readTree("{"
                + " gsCredentials : 'file:/cfile',\n"
                + " graph : 'gs://b/g.obj',\n"
                + " streetGraph : 'file:/b/bg.obj',\n"
                + " osm : [ 'file:/b/osm.pbf' ],\n"
                + " dem : [ 'file:/b/dem.tif' ],\n"
                + " netex : [ 'gs://b/netex.zip' ],\n"
                + " gtfs : [ 'file:/b/gtfs.zip' ],\n"
                + " buildReportDir : 'gs://b/report'\n"
                + "}");
        StorageParameters c =  new StorageParameters(node);

        assertEquals("file:/cfile", c.gsCredentials);
        assertEquals("gs://b/g.obj", c.graph.toString());
        assertEquals("file:/b/bg.obj", c.streetGraph.toString());
        assertEquals("[file:/b/osm.pbf]", c.osm.toString());
        assertEquals("[file:/b/dem.tif]", c.dem.toString());
        assertEquals("[gs://b/netex.zip]", c.netex.toString());
        assertEquals("[file:/b/gtfs.zip]", c.gtfs.toString());
        assertEquals("gs://b/report", c.buildReportDir.toString());
    }
}
