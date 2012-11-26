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

package org.opentripplanner.api.thrift.util;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.api.thrift.definition.GraphVertex;
import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

/**
 * Tests for TripUtil class.
 * 
 * @author flamholz
 */
public class GraphVertexExtensionTest extends TestCase {
    private Graph _graph;

    @Before
    public void before() {
        _graph = new Graph();
    }
    
    @Test
    public void testConstructFromVertex() {
    	Vertex v = vertex("fake", 75.0239, -45.139023);
    	GraphVertexExtension graphVert = new GraphVertexExtension(v);
    	assertVertexEquals(v, graphVert);
    }
    
    /****
     * Private Methods
     ****/
    private SimpleVertex vertex(String label, double lat, double lon) {
        SimpleVertex v = new SimpleVertex(_graph, label, lat, lon);
        return v;
    }
    
    private void assertVertexEquals(Vertex v, GraphVertex gv) {
    	assertEquals(gv.getLabel(), v.getLabel());
    	assertEquals(gv.getName(), v.getName());
    	assertEquals(gv.getIn_degree(), v.getDegreeIn());
    	assertEquals(gv.getOut_degree(), v.getDegreeOut());
    	
    	Location loc = gv.getLocation();
    	LatLng ll = loc.getLat_lng();
    	assertEquals(ll.getLat(), v.getY());
    	assertEquals(ll.getLng(), v.getX());
    }

    private static class SimpleVertex extends StreetVertex {

        private static final long serialVersionUID = 1L;

        public SimpleVertex(Graph g, String label, double lat, double lon) {
            super(g, label, lon, lat, label);
        }
    }
}