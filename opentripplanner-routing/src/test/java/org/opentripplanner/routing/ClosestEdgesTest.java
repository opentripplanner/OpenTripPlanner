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

package org.opentripplanner.routing;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.LocationObservation;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdge;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class ClosestEdgesTest {

    private Graph graph;
    
    private StreetVertexIndexService finder;

    private PlainStreetEdge top, bottom, left, right;

    private IntersectionVertex br, tr, bl, tl;

    public LineString createGeometry(Vertex a, Vertex b) {
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] cs = new Coordinate[2];
        cs[0] = a.getCoordinate();
        cs[1] = b.getCoordinate();
        return factory.createLineString(cs);
    }

	@Before
	public void before() {
		graph = new Graph();
		// a 0.1 degree x 0.1 degree square
		tl = new IntersectionVertex(graph, "tl", -74.01, 40.01);
		tr = new IntersectionVertex(graph, "tr", -74.0, 40.01);
		bl = new IntersectionVertex(graph, "bl", -74.01, 40.0);
		br = new IntersectionVertex(graph, "br", -74.00, 40.0);

		top = new PlainStreetEdge(tl, tr, GeometryUtils.makeLineString(-74.01,
				40.01, -74.0, 40.01), "top", 1500,
				StreetTraversalPermission.CAR, false);
		bottom = new PlainStreetEdge(br, bl, GeometryUtils.makeLineString(
				-74.01, 40.0, -74.0, 40.0), "bottom", 1500,
				StreetTraversalPermission.BICYCLE_AND_CAR, false);
		left = new PlainStreetEdge(bl, tl, GeometryUtils.makeLineString(-74.01,
				40.0, -74.01, 40.01), "left", 1500,
				StreetTraversalPermission.BICYCLE_AND_CAR, false);
		right = new PlainStreetEdge(br, tr, GeometryUtils.makeLineString(-74.0,
				40.0, -74.0, 40.01), "right", 1500,
				StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false);

		StreetEdge topBack = new PlainStreetEdge(tr, tl, (LineString) top
				.getGeometry().reverse(), "topBack", 1500,
				StreetTraversalPermission.CAR, true);
		StreetEdge bottomBack = new PlainStreetEdge(br, bl, (LineString) bottom
				.getGeometry().reverse(), "bottomBack", 1500,
				StreetTraversalPermission.BICYCLE_AND_CAR, true);
		StreetEdge leftBack = new PlainStreetEdge(tl, bl, (LineString) left
				.getGeometry().reverse(), "leftBack", 1500,
				StreetTraversalPermission.BICYCLE_AND_CAR, true);
		StreetEdge rightBack = new PlainStreetEdge(tr, br, (LineString) right
				.getGeometry().reverse(), "rightBack", 1500,
				StreetTraversalPermission.CAR, true);

		StreetVertexIndexServiceImpl myFinder = new StreetVertexIndexServiceImpl(graph);
        myFinder.setup();
        finder = myFinder;
	}

	private void checkClosestEdgeModes(LocationObservation loc,
			TraversalRequirements reqs, int minResults) {
		CandidateEdgeBundle edges = finder.getClosestEdges(loc, reqs);
		assertTrue(minResults <= edges.size());

		// Double check that all the edges returned can be traversed.
		for (CandidateEdge e : edges) {
			assertTrue(reqs.canBeTraversed(e.getEdge()));
		}
	}

    @Test
    public void testModeRestriction() {
    	// Lies along the top right edge
        Coordinate c = new Coordinate(-74.005000001, 40.01);
        LocationObservation loc = new LocationObservation(c);
        TraversalRequirements reqs = new TraversalRequirements();
        
        // Default traversal requirements allow any traversal mode.
        checkClosestEdgeModes(loc, reqs, 1);
        
        // Only allow walking
        TraverseModeSet modes = new TraverseModeSet();
        modes.setWalk(true);
        reqs.setModes(modes);
        
        // There's only one walkable edge.        
        checkClosestEdgeModes(loc, reqs, 1);
        
        // Only allow biking: there are 5 bikeable edges.
        modes = new TraverseModeSet();
        modes.setBicycle(true);
        reqs.setModes(modes);
        checkClosestEdgeModes(loc, reqs, 2);
        
        // Only allow driving: there are 7 driveable edges.
        modes = new TraverseModeSet();
        modes.setCar(true);
        reqs.setModes(modes);
        checkClosestEdgeModes(loc, reqs, 2);
        
        // Allow driving and biking: all 8 edges can be traversed.
        modes = new TraverseModeSet();
        modes.setCar(true);
        modes.setBicycle(true);
        reqs.setModes(modes);
        checkClosestEdgeModes(loc, reqs, 2);
    }
    
    @Test
    public void testInteriorEdgeCase() {
    	// Lies smack in the middle of the box
        Coordinate c = new Coordinate(-74.005, 40.005);
        LocationObservation loc = new LocationObservation(c);
        TraversalRequirements reqs = new TraversalRequirements();
        
        // Should only return 2 edges even though all edges are equidistant.
        // TODO(flamholz): this doesn't feel like the right behavior to me.
        // Consider fixing it.
		CandidateEdgeBundle edges = finder.getClosestEdges(loc, reqs);
		assertEquals(2, edges.size());
    }
    
    @Test
    public void testHeading() {
		// TODO(flamholz): update this code once we include the difference between the heading and
    	// heading in the edge ranking.

    	// Lies along the top edge
    	Coordinate c = new Coordinate(-74.005000001, 40.01);
    	
    	// Location along the top edge, traveling with the forward edge exactly.
		LocationObservation loc = new LocationObservation.Builder()
				.setCoordinate(c).setHeading(top.getAzimuth()).build();
		
		// Request only car edges: top edge is car only.
		TraversalRequirements reqs = new TraversalRequirements();
		TraverseModeSet modes = new TraverseModeSet();
        modes.setCar(true);
        reqs.setModes(modes);
        
        // Both directions are car-only.
        checkClosestEdgeModes(loc, reqs, 2);
    }
}
