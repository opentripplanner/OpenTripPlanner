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

package org.opentripplanner.routing.impl;

import com.vividsolutions.jts.geom.*;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Find the nearest street edges to the given point, check if they are served by flag stop routes.
 *
 * @author dbenoff
 */
public class FlagStopSplitterService {

    private static GeometryFactory gf = new GeometryFactory();

    private static final long serialVersionUID = -3729628250159575313L;

    private static final Logger LOG = LoggerFactory.getLogger(FlagStopSplitterService.class);

    public static List<StreetEdge> getClosestStreetEdgesToOrigin(RoutingContext ctx, Graph graph) {
        return getClosestStreetEdges(ctx, graph, true);
    }

    public static List<StreetEdge> getClosestStreetEdgesToDestination(RoutingContext ctx, Graph graph) {
        return getClosestStreetEdges(ctx, graph, false);
    }

    private static List<StreetEdge> getClosestStreetEdges(RoutingContext ctx, Graph graph, Boolean isOrigin) {

        RoutingRequest opt = ctx.opt;
        opt.rctx = ctx;

        Double lon = opt.to.lng;
        Double lat = opt.to.lat;

        if(isOrigin){
            lon = opt.from.lng;
            lat = opt.from.lat;
        }


        Coordinate pointLocation = new Coordinate(lon, lat);

        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(500);

        Envelope env = new Envelope(pointLocation);

        // local equirectangular projection
        final double xscale = Math.cos(lat * Math.PI / 180);

        env.expandBy(radiusDeg / xscale, radiusDeg);

        Collection<Edge> edges = graph.streetIndex.getEdgesForEnvelope(env);
        Map<Double, List<StreetEdge>> edgeDistanceMap = new TreeMap<>();
        for(Edge edge : edges){
            if(edge instanceof StreetEdge){
                LineString line = edge.getGeometry();
                double dist = SphericalDistanceLibrary.fastDistance(pointLocation, line);
                double roundOff = (double) Math.round(dist * 100) / 100;
                if(!edgeDistanceMap.containsKey(roundOff))
                    edgeDistanceMap.put(roundOff, new ArrayList<>());
                edgeDistanceMap.get(roundOff).add((StreetEdge) edge);
            }
        }

        List<StreetEdge> closestEdges = edgeDistanceMap.values().iterator().next();
        List<StreetEdge> ret = new ArrayList<>();
        for(StreetEdge closestEdge : closestEdges){
            List<PatternHop> patternHops = graph.index.getHopsForEdge(closestEdge)
                    .stream()
                    //.filter(e -> e.getPattern() == originalTripPattern)
                    //todo: check if these are flag stop hops
                    .collect(Collectors.toList());
            if(patternHops.size() > 0){
                ret.add(closestEdge);
            }
        }
        return ret;
    }

}
