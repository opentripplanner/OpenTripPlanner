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

package org.opentripplanner.graph_builder.impl.transit_index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.GtfsBundles;
import org.opentripplanner.graph_builder.services.GraphBuilderWithGtfsDao;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;

public class TransitIndexBuilderTest extends TestCase {

	public void testTransitIndexBuilder() throws IOException {
		Graph graph = new Graph();

		File testGtfs = new File("../opentripplanner-routing/src/test/resources/testagency.zip");
		GtfsBundle bundle = new GtfsBundle();
		bundle.setPath(testGtfs);
		
		GtfsBundles bundles = new GtfsBundles();
		bundles.setBundles(Arrays.asList(bundle));

		GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl();
		gtfsBuilder.setGtfsBundles(bundles);
		
		TransitIndexBuilder builder = new TransitIndexBuilder();
		List<GraphBuilderWithGtfsDao> builders = new ArrayList<GraphBuilderWithGtfsDao>();
		builders.add(builder);
		gtfsBuilder.setGtfsGraphBuilders(builders);
		
		gtfsBuilder.buildGraph(graph, new HashMap<Class<?>, Object>());
		
		TransitIndexService index = graph.getService(TransitIndexService.class); 
	
		assertNotNull(index);
		
		Edge prealightEdge = index.getPreAlightEdge(new AgencyAndId("agency", "A"));
		assertTrue(prealightEdge instanceof PreAlightEdge);
		
		//route 18 is the only bidirectional route in the test data
		List<RouteVariant> variantsForRoute = index.getVariantsForRoute(new AgencyAndId("agency", "18"));
		assertEquals(2, variantsForRoute.size());

		Collection<String> directionsForRoute = index.getDirectionsForRoute(new AgencyAndId("agency", "18"));
		assertEquals(2, directionsForRoute.size());
		
		variantsForRoute = index.getVariantsForRoute(new AgencyAndId("agency", "2"));
		assertEquals(1, variantsForRoute.size());
		
		directionsForRoute = index.getDirectionsForRoute(new AgencyAndId("agency", "2"));
		assertEquals(1, directionsForRoute.size());
		assertEquals(null, directionsForRoute.iterator().next());

	}
}
