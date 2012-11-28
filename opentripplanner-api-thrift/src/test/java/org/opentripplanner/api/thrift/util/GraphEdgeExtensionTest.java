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
import org.opentripplanner.api.thrift.definition.GraphEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

/**
 * Tests for TripUtil class.
 * 
 * @author flamholz
 */
public class GraphEdgeExtensionTest extends TestCase {
	private Graph _graph;

	@Before
	public void before() {
		_graph = new Graph();
	}

	@Test
	public void testConstructFromEdge() {
		SimpleVertex head = new SimpleVertex(_graph, "head", 75.0239,
				-45.139023);
		SimpleVertex tail = new SimpleVertex(_graph, "tail", 75.0239,
				-45.139023);

		StreetEdge e = new PlainStreetEdge(head, tail, null, "fake", 1.0,
				StreetTraversalPermission.BICYCLE_AND_CAR, false);

		GraphEdgeExtension graphEdge = new GraphEdgeExtension(e);

		assertEdgeEquals(e, graphEdge);
	}

	private void assertEdgeEquals(Edge e, GraphEdge ge) {
		assertTrue(((SimpleVertex) e.getFromVertex()).equals(ge.getHead()));
		assertTrue(((SimpleVertex) e.getToVertex()).equals(ge.getTail()));
	}
}