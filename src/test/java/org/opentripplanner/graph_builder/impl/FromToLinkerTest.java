/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.graph_builder.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.impl.map.BusRouteStreetMatcher;
import org.opentripplanner.graph_builder.impl.osm.DebugNamer;
import org.opentripplanner.graph_builder.impl.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 *
 * @author mabu
 */
public class FromToLinkerTest {
    private HashMap<Class<?>, Object> extra;
    private Graph graph;
    private Envelope extent;
    StreetVertexIndexServiceImpl index;
    
    @Before
    public void init() throws Exception {
        extra = new HashMap<Class<?>, Object>();
        loadGraph("maribor_clean.osm.gz", "marprom_fake_gtfs_small.zip", false, false);
    }
            
    
    /**
     * Creates graph from OSM and GTFS data and runs {@link TransitToTaggedStopsGraphBuilderImpl} and {@link TransitToStreetNetworkGraphBuilderImpl}.
     * @param osm_filename filename for OSM (in resource folder of class)
     * @param gtfs_filename filename for GTFS (in resource folder of class)
     * @param wanted_con_filename filename for saved connections (in resource folder of class)
     * @throws Exception 
     */
    private void loadGraph(String osm_filename, String gtfs_filename,
            boolean taggedBuilder, boolean normalBuilder) throws Exception{

        graph = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        //names streets based on osm ids (osm:way:osmid)
        loader.customNamer = new DebugNamer();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();
        loader.skipVisibility = true;
        loader.staticParkAndRide = true;
        
        PruneFloatingIslands pfi = new PruneFloatingIslands();

        File file = new File(getClass().getResource(osm_filename).getFile());

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(graph, extra);
        
        extent = graph.getExtent();
                
        //pfi.buildGraph(gg, extra);
        
        index = new StreetVertexIndexServiceImpl(graph);
               
        
        
        if (taggedBuilder || normalBuilder) {
             File gtfs_file = new File(getClass().getResource(gtfs_filename).getFile());
            List<GtfsBundle> gtfsBundles = Lists.newArrayList();
        
            GtfsBundle gtfsBundle = new GtfsBundle(gtfs_file);
            gtfsBundle.setTransfersTxtDefinesStationPaths(false);
            gtfsBundles.add(gtfsBundle);
            
            GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl(gtfsBundles);
        
            BusRouteStreetMatcher matcher = new BusRouteStreetMatcher();
            
            gtfsBuilder.buildGraph(graph, extra);

            matcher.buildGraph(graph, extra);
        }
        
        if (taggedBuilder) {
            TransitToTaggedStopsGraphBuilderImpl transitToTaggedStopsGraphBuilderImpl = new TransitToTaggedStopsGraphBuilderImpl();
            transitToTaggedStopsGraphBuilderImpl.buildGraph(graph, extra);
        }
        
        if (normalBuilder) {
            TransitToStreetNetworkGraphBuilderImpl transitToStreetNetworkGraphBuilderImpl = new TransitToStreetNetworkGraphBuilderImpl();
            transitToStreetNetworkGraphBuilderImpl.buildGraph(graph, extra);
        }
        graph.index(new DefaultStreetVertexIndexFactory());
    }
    
        /**
     * Tests if correct start vertex is chosen when closest vertex is
     * corner of two bike paths
     * @throws Exception 
     */
    @Test
    public void testStartStop() throws Exception {
        //Graph graph = loadGraph("maribor_clean.osm.gz", "marprom_fake_gtfs_small.zip", false, false);
        
        RoutingRequest options = new RoutingRequest(new TraverseModeSet("CAR"));
        options.from = new GenericLocation(46.543140807175675, 15.633641481399536);
        //options.from = new GenericLocation(46.543081772091305, 15.63375413417816);
        options.to = new GenericLocation(46.541369726720106, 15.637820363044739);
        options.setRoutingContext(graph);
        //ShortestPathTree tree = aStar.getShortestPathTree(options);
        
        assertNotNull(options.rctx.toVertex);
        assertNotNull(options.rctx.fromVertex);
        
        
        for (Edge e :options.rctx.fromVertex.getOutgoing()) {
            if (e instanceof TemporaryFreeEdge) {
                for(StreetEdge se: Iterables.filter(e.getToVertex().getOutgoingStreetEdges(), StreetEdge.class)) { 
                    assertTrue("Start vertex isn't connected to car traversable street",
                        se.getPermission().allows(StreetTraversalPermission.CAR));
                }
            }
        }
        
        for(StreetEdge se: Iterables.filter(options.rctx.fromVertex.getOutgoingStreetEdges(), StreetEdge.class)) { 
            assertTrue("Start vertex isn't connected to car traversable street",
                    se.getPermission().allows(StreetTraversalPermission.CAR));
        }
        
        for(StreetEdge se: Iterables.filter(options.rctx.toVertex.getIncoming(), StreetEdge.class)) {
            assertTrue("End vertex isn't connected to car traversable street",
                    se.getPermission().allows(StreetTraversalPermission.CAR));
        }
        
        
        assertTrue("Start vertex isn't connected to car traversable street", 
        options.rctx.fromVertex.getName().contains("osm:way:5563224") || options.rctx.fromVertex.getName().contains("osm:way:6217378"));
        
        
    }
    
    /**
     * Tests that connection between start and stop location is optimal for traverse mode
     * 
     * Meaning that if bikepath is next to a street it is used for cycling instead of street.
     * Same if sidewalk is used
     * @param start
     * @param stop
     * @param mode 
     */
    private void testStartDiffModes(GenericLocation start, GenericLocation stop, TraverseModeSet mode) {
        RoutingRequest options = new RoutingRequest(mode);
        options.from = start;
        options.to = stop;
        options.setRoutingContext(graph);
        
        assertNotNull(options.rctx.toVertex);
        assertNotNull(options.rctx.fromVertex);
        
        GenericAStar aStar = new GenericAStar();
        ShortestPathTree spt;
        GraphPath path;
        
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(options.rctx.toVertex, true);
        assertNotNull(path);
        
        for (StreetEdge se: Iterables.filter(path.edges, StreetEdge.class)) {
            //We want to check first normal edge.
            if (se instanceof TemporaryEdge) {
                continue;
            }
            if (mode.equals(new TraverseModeSet("CAR"))) {
                assertTrue("Start vertex isn't connected to car traversable street",
                            se.getPermission().allows(StreetTraversalPermission.CAR));
            } else {
                assertTrue("Start vertex isn't connected to " + mode.toString() +" traversable street",
                            !se.getPermission().allows(StreetTraversalPermission.CAR) && se.getPermission().allows(mode));
            }
            break;
        }
    }
    
    @Test
    public void testStartDiffModes() throws Exception {
        GenericLocation start = new GenericLocation(46.54252462532098, 15.634263753890991);
        GenericLocation stop = new GenericLocation(46.54410380792452, 15.637364387512205);
        
        testStartDiffModes(start, stop, new TraverseModeSet("CAR"));
        testStartDiffModes(start, stop, new TraverseModeSet("BICYCLE"));
        testStartDiffModes(start, stop, new TraverseModeSet("WALK"));
        
        //Tests start and stop next to bike path
        //(walking + biking need to go on bikepath, Car on the street next to it)
        start = new GenericLocation(46.56075622184689,15.639365315437317);
        stop = new GenericLocation(46.56033573197034,15.641698837280273);
        
        testStartDiffModes(start, stop, new TraverseModeSet("CAR"));
        testStartDiffModes(start, stop, new TraverseModeSet("BICYCLE"));
        testStartDiffModes(start, stop, new TraverseModeSet("WALK"));
        
    }
    
}
