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

package org.opentripplanner.routing.graph;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.AlertPatch;

public class EdgeTest {

	Graph _graph;

	@Before
	public void before() {
		_graph = new Graph();
	}

	@Test
	public void testConstruct() {
		Vertex head = new SimpleConcreteVertex(
				_graph, "head", 47.669457, -122.387577);
		Vertex tail = new SimpleConcreteVertex(
				_graph, "tail", 47.669462, -122.384739);
		Edge e = new SimpleConcreteEdge(head, tail);

		assertEquals(head, e.getFromVertex());
		assertEquals(tail, e.getToVertex());
		assertTrue(e.getId() >= 0);
	}

	/**
	 * Creates a simple edge without an ID.
	 * 
	 * @return
	 */
	private SimpleConcreteEdge makeSimpleEdge() {
		Vertex head = new SimpleConcreteVertex(_graph, "head", 47.669457,
				-122.387577);
		Vertex tail = new SimpleConcreteVertex(_graph, "tail", 47.669462,
				-122.384739);
		return new SimpleConcreteEdge(head, tail);
	}

	@Test
	public void testGetId() {
		Edge e = makeSimpleEdge();
		assertTrue(e.getId() >= 0);
	}
        
        @Test
        public void testPatches() {
            Edge edge = makeSimpleEdge();
            AlertPatch[] alerts = new AlertPatch[]{ new AlertPatch(), new AlertPatch(), new AlertPatch() };

            alerts[0].setAlert(new Alert());
            alerts[1].setAlert(new Alert());
            alerts[2].setAlert(new Alert());

            alerts[0].setId("0");
            alerts[1].setId("1");
            alerts[2].setId("2");
            
            edge.addPatch(alerts[0]);
            edge.addPatch(alerts[1]);
            
            assertEquals(2, edge.getPatches().size());
            assertTrue(edge.getPatches().contains(alerts[0]));
            assertTrue(edge.getPatches().contains(alerts[1]));
            
            edge.removePatch(alerts[0]);
            
            assertEquals(1, edge.getPatches().size());
            assertFalse(edge.getPatches().contains(alerts[0]));
            assertTrue(edge.getPatches().contains(alerts[1]));
            
            edge.removePatch(alerts[0]);
            assertEquals(1, edge.getPatches().size());
            assertFalse(edge.getPatches().contains(alerts[0]));
            assertTrue(edge.getPatches().contains(alerts[1]));
            
            edge.removePatch(alerts[1]);
            assertTrue(edge.getPatches().isEmpty());
        }
}