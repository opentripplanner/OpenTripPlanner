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

package org.opentripplanner.routing.core;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RoutingFeaturesTest extends TestCase {

    AStar aStar = new AStar();

    private static Graph graph;

    @Override
    public void setUp() {
        GraphBuilder graphBuilder = new GraphBuilder();

        List<OpenStreetMapProvider> osmProviders = Lists.newArrayList();
        OpenStreetMapProvider osmProvider = new AnyFileBasedOpenStreetMapProviderImpl(new File(ConstantsForTests.OSLO_MINIMAL_OSM));
        osmProviders.add(osmProvider);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
        DefaultStreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
        osmModule.edgeFactory = streetEdgeFactory;
        osmModule.skipVisibility = true;
        graphBuilder.addModule(osmModule);
        List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        GtfsBundle gtfsBundle = new GtfsBundle(new File(ConstantsForTests.OSLO_MINIMAL_GTFS));
        gtfsBundle.linkStopsToParentStations = true;
        gtfsBundle.parentStationTransfers = true;
        gtfsBundles.add(gtfsBundle);
        GtfsModule gtfsModule = new GtfsModule(gtfsBundles);
        graphBuilder.addModule(gtfsModule);
        graphBuilder.addModule(new StreetLinkerModule());
        graphBuilder.serializeGraph = false;
        graphBuilder.run();

        graph = graphBuilder.getGraph();
        graph.index(new DefaultStreetVertexIndexFactory());
    }

    /**
     * Test kiss and ride feature
     */
    public void testKissAndRide() {
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("Europe/Oslo"
                , 2017, 10, 15, 7, 0, 0);
        options.from = new GenericLocation(59.9113032, 10.7489964);
        options.to = new GenericLocation(59.90808, 10.607298);
        options.setNumItineraries(1);
        options.setRoutingContext(graph);
        options.kissAndRide = true;
        options.modes = TraverseModeSet.allModes();
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPaths().get(0);

        // Car leg before transit leg
        boolean carLegSeen = false;
        boolean transitLegSeen = false;
        for (int i = 0; i < path.states.size(); i++) {
            TraverseMode mode = path.states.get(i).getBackMode();
            if (mode != null) {
                assertFalse(transitLegSeen && mode.isDriving());
                if (mode.isDriving()) {
                    carLegSeen = true;
                }
                if (mode.isTransit()) {
                    transitLegSeen = true;
                }
            }
        }
        assertTrue(carLegSeen && transitLegSeen);
    }

    /**
     * Test ride and kiss feature
     */
    public void testRideAndKiss() {
        RoutingRequest options = new RoutingRequest();
        options.dateTime = TestUtils.dateInSeconds("Europe/Oslo"
                , 2017, 10, 15, 7, 0, 0);
        options.from = new GenericLocation(60.1913707, 11.0928143);
        options.to = new GenericLocation(59.9113032, 10.7489964);
        options.setNumItineraries(1);
        options.setRoutingContext(graph);
        options.rideAndKiss = true;
        options.modes = TraverseModeSet.allModes();
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPaths().get(0);

        // Transit leg before car leg
        boolean carLegSeen = false;
        boolean transitLegSeen = false;
        for (int i = 0; i < path.states.size(); i++) {
            TraverseMode mode = path.states.get(i).getBackMode();
            if (mode != null) {
                assertFalse(carLegSeen && mode.isTransit());
                if (mode.isDriving()) {
                    carLegSeen = true;
                }
                if (mode.isTransit()) {
                    transitLegSeen = true;
                }
            }
        }
        assertTrue(carLegSeen && transitLegSeen);
    }
}
