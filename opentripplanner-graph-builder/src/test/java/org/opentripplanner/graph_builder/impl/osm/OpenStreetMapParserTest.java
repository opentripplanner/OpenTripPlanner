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

package org.opentripplanner.graph_builder.impl.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

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

        Map<Long, OSMNode> nodes = map.getNodes();
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
