package org.opentripplanner.standalone.config;


import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

public class StorageConfigTest {

    @Test
    public void testCreateGoogleCloudStorageParameters() throws IOException {
        NodeAdapter nodeAdapter = newNodeAdapterForTest(
                "{"
                + " gsCredentials : 'file:/cfile',\n"
                + " graph : 'gs://b/g.obj',\n"
                + " streetGraph : 'file:/b/bg.obj',\n"
                + " osm : [ 'file:/b/osm.pbf' ],\n"
                + " dem : [ 'file:/b/dem.tif' ],\n"
                + " netex : [ 'gs://b/netex.zip' ],\n"
                + " gtfs : [ 'file:/b/gtfs.zip' ],\n"
                + " buildReportDir : 'gs://b/report'\n"
                + "}"
        );
        StorageConfig c =  new StorageConfig(nodeAdapter);

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
