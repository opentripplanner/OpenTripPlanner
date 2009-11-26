package org.opentripplanner.graph_builder.impl.osm;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Test;
import org.opentripplanner.graph_builder.model.osm.OSMMap;
import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMWay;

public class OpenStreetMapParserTest {

    @Test
    public void testParser() throws Exception {
        InputStream in = new GZIPInputStream(getClass().getResourceAsStream("map.osm.gz"));
        OpenStreetMapParser parser = new OpenStreetMapParser();
        OSMMap map = new OSMMap();
        parser.parseMap(in, map);

        Map<Integer, OSMNode> nodes = map.getNodes();
        assertEquals(7197, nodes.size());

        OSMNode nodeA = map.getNodeForId(27308461);
        assertEquals(27308461, nodeA.getId());
        assertEquals(52.3887673, nodeA.getLat(), 0.0);
        assertEquals(16.8506243, nodeA.getLon(), 0.0);
        Map<String, String> tags = nodeA.getTags();
        assertEquals("JOSM", tags.get("created_by"));
        assertEquals("survey", tags.get("source"));

        OSMNode nodeB = map.getNodeForId(27308457);
        assertEquals(27308457, nodeB.getId());
        assertEquals(52.3850672, nodeB.getLat(), 0.0);
        assertEquals(16.8396962, nodeB.getLon(), 0.0);
        tags = nodeB.getTags();
        assertEquals("Wieruszowska", tags.get("name"));
        assertEquals("tram_stop", tags.get("railway"));
        assertEquals("survey", tags.get("source"));
        assertEquals("1", tags.get("layer"));

        Map<Integer, OSMWay> ways = map.getWays();
        assertEquals(1511, ways.size());

        OSMWay wayA = map.getWayForId(13490353);
        assertEquals(13490353, wayA.getId());
        List<Integer> nodeRefsA = wayA.getNodeRefs();
        assertEquals(2, nodeRefsA.size());
        assertEquals(123978834, nodeRefsA.get(0).intValue());
        assertEquals(123980465, nodeRefsA.get(1).intValue());
        tags = wayA.getTags();
        assertEquals("Potlatch 0.9a", tags.get("created_by"));
        assertEquals("secondary", tags.get("highway"));
    }
}
