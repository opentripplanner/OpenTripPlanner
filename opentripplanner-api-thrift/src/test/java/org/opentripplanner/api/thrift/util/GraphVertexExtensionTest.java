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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.graph.Graph;

/**
 * Tests for TripUtil class.
 * 
 * @author flamholz
 */
public class GraphVertexExtensionTest {
	private Graph _graph;

	@Before
	public void before() {
		_graph = new Graph();
	}

	@Test
	public void testConstructFromVertex() {
		SimpleVertex v = new SimpleVertex(_graph, "fake", 75.0239, -45.139023);
		GraphVertexExtension graphVert = new GraphVertexExtension(v);
		assertTrue(v.equals(graphVert));
	}
}