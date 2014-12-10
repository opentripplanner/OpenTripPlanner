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

package org.opentripplanner.graph_builder.impl;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.api.resource.SimpleIsochrone;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * {@link GraphBuilder} plugin that links up the stops of a transit network among themselves,
 * without using the street network at all. For now this just considers distance, but it should
 * also consider parent station specifications, which are present in the Dutch KV7 data.
 *
 * We only create links to the closest stop on each other pattern passing in the vicinity to cut down on useless edges.
 */
public class StreetlessStopLinker implements GraphBuilder {

    private double radius = 500; 

    private static Logger LOG = LoggerFactory.getLogger(StreetlessStopLinker.class); 

    DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();
    
    public List<String> provides() {
        return Arrays.asList("linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("transit");
    }



    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        StreetVertexIndexService streetIndex = new StreetVertexIndexServiceImpl(graph);
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        GraphIndex gidx = new GraphIndex(graph);
        LOG.info("Linking stops directly to one another for long distance routing...");
        int nTransfers = 0;
        for (TransitStop ts0 : Iterables.filter(graph.getVertices(), TransitStop.class)) {
            Coordinate c0 = ts0.getCoordinate();
            Map<TripPattern, StopAtDistance> closestStops = new SimpleIsochrone.MinMap<TripPattern, StopAtDistance>();
            for (TransitStop ts1 : streetIndex.getNearbyTransitStops(c0, radius)) {
                if(!ts1.isStreetLinkable())
                    continue;
                double distance = distanceLibrary.distance(c0, ts1.getCoordinate());
                StopAtDistance nearbyStop = new StopAtDistance(ts1, distance);
                for (TripPattern pattern : gidx.patternsForStop.get(ts1.getStop())) {
                    closestStops.put(pattern, nearbyStop);
                }
            }
            // Make a transfer to the closest stop on each pattern.
            Set<StopAtDistance> uniqueStops = Sets.newHashSet();
            uniqueStops.addAll(closestStops.values());
            for (StopAtDistance nearbyStop : uniqueStops) {
                if (nearbyStop.tstop == ts0) continue; // skip the origin stop, no loop transfers needed
                Coordinate coordinates[] = new Coordinate[] {c0, nearbyStop.tstop.getCoordinate()};
                LineString geometry = geometryFactory.createLineString(coordinates);
                new SimpleTransfer(ts0, nearbyStop.tstop, nearbyStop.dist, geometry);
                nTransfers += 1;
            }
            LOG.debug("Linked stop {} to {} nearby stops on other patterns.", ts0.getStop(), uniqueStops.size());
        }
        LOG.info("Done linking stops to one another. Created {} transfers.", nTransfers);
    }

    @Override
    public void checkInputs() {
        //no inputs
    }

    /**
     * Represents a stop that is comparable to other stops on the basis of its distance from some point.
     */
    public class StopAtDistance implements Comparable<StopAtDistance> {

        public TransitStop tstop;
        public double dist;

        public StopAtDistance(TransitStop tstop, double dist) {
            this.tstop = tstop;
            this.dist = dist;
        }

        @Override
        public int compareTo(StopAtDistance that) {
            return (int)(this.dist) - (int)(that.dist);
        }

        public String toString() {
            return String.format("stop %s at %d meters", tstop, dist);
        }

    }

}
