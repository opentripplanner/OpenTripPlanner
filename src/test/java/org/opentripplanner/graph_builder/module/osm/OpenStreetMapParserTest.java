/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.zip.GZIPInputStream;
import java.net.URLDecoder;

import org.junit.Test;



import org.opentripplanner.openstreetmap.model.OSMMap;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.impl.BinaryFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.impl.FileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.impl.OpenStreetMapParser;
import org.opentripplanner.openstreetmap.impl.StreamedFileBasedOpenStreetMapProviderImpl;

public class OpenStreetMapParserTest {
    @Test
    public void testAFBinaryParser() throws Exception {
        AnyFileBasedOpenStreetMapProviderImpl pr = new AnyFileBasedOpenStreetMapProviderImpl();
        OSMMap map = new OSMMap();
        pr.setPath(new File(URLDecoder.decode(getClass().getResource("map.osm.pbf").getPath(), "UTF-8")));
        pr.readOSM(map);
        testParser(map);
    }

    @Test
    public void testAFXMLParser() throws Exception {
        AnyFileBasedOpenStreetMapProviderImpl pr = new AnyFileBasedOpenStreetMapProviderImpl();
        OSMMap map = new OSMMap();
        pr.setPath(new File(URLDecoder.decode(getClass().getResource("map.osm.gz").getPath(), "UTF-8")));
        pr.readOSM(map);
        testParser(map);
    }

    @Test
    public void testBinaryParser() throws Exception {
        BinaryFileBasedOpenStreetMapProviderImpl pr = new BinaryFileBasedOpenStreetMapProviderImpl();
        OSMMap map = new OSMMap();
        pr.setPath(new File(URLDecoder.decode(getClass().getResource("map.osm.pbf").getPath(), "UTF-8")));
        pr.readOSM(map);
        testParser(map);
    }

    @Test
    public void testXMLParser() throws Exception {
        FileBasedOpenStreetMapProviderImpl pr = new FileBasedOpenStreetMapProviderImpl();
        OSMMap map = new OSMMap();
        pr.setPath(new File(URLDecoder.decode(getClass().getResource("map.osm.gz").getPath(), "UTF-8")));
        pr.readOSM(map);
        testParser(map);
    }

    @Test
    public void testStreamedXMLParser() throws Exception {
        StreamedFileBasedOpenStreetMapProviderImpl pr = new StreamedFileBasedOpenStreetMapProviderImpl();
        OSMMap map = new OSMMap();
        pr.setPath(new File(URLDecoder.decode(getClass().getResource("map.osm.gz").getPath(), "UTF-8")));
        pr.readOSM(map);
        testParser(map);
    }

    @Test
    public void testBasicParser() throws Exception {
        InputStream in = new GZIPInputStream(getClass().getResourceAsStream("map.osm.gz"));
        OpenStreetMapParser parser = new OpenStreetMapParser();
        OSMMap map = new OSMMap();
        parser.parseMap(in, map);
        testParser(map);
    }

    public void testParser(OSMMap map) throws Exception {
        Map<Long, OSMNode> nodes = map.getNodes();
        assertEquals(7197, nodes.size());

        OSMNode nodeA = map.getNodeForId(27308461);
        assertEquals(27308461, nodeA.getId());
        assertEquals(52.3887673, nodeA.lat, 0.0000001);
        assertEquals(16.8506243, nodeA.lon, 0.0000001);
        Map<String, String> tags = nodeA.getTags();
        assertEquals("JOSM", tags.get("created_by"));
        assertEquals("survey", tags.get("source"));

        OSMNode nodeB = map.getNodeForId(27308457);
        assertEquals(27308457, nodeB.getId());
        assertEquals(52.3850672, nodeB.lat, 0.0000001);
        assertEquals(16.8396962, nodeB.lon, 0.0000001);
        tags = nodeB.getTags();
        assertEquals("Wieruszowska", tags.get("name"));
        assertEquals("tram_stop", tags.get("railway"));
        assertEquals("survey", tags.get("source"));
        assertEquals("1", tags.get("layer"));

        OSMNode nodeC = map.getNodeForId(299769943);
        assertTrue(nodeC.hasTag("name"));
        assertNull(nodeC.getTag("not-existing-tag"));
        assertEquals("Apteka Junikowska", nodeC.getTag("name"));
        assertTrue(nodeC.isTagTrue("dispensing"));
        assertFalse(nodeC.isTagFalse("dispensing"));
        assertFalse(nodeC.isTagTrue("not-existing-tag"));
        assertFalse(nodeC.isTagFalse("not-existing-tag"));

        OSMNode nodeD = map.getNodeForId(338912397);
        assertTrue(nodeD.isTagFalse("dispensing"));
        assertFalse(nodeD.isTagTrue("dispensing"));

        Map<Long, OSMWay> ways = map.getWays();
        assertEquals(1511, ways.size());

        OSMWay wayA = map.getWayForId(13490353);
        assertEquals(13490353, wayA.getId());
        List<Long> nodeRefsA = wayA.getNodeRefs();
        assertEquals(2, nodeRefsA.size());
        assertEquals(123978834, nodeRefsA.get(0).longValue());
        assertEquals(123980465, nodeRefsA.get(1).longValue());
        tags = wayA.getTags();
        assertEquals("Potlatch 0.9a", tags.get("created_by"));
        assertEquals("secondary", tags.get("highway"));
    }
}
