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

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.strategies.TransitLocalStreetService;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;

public class SkipNonTransferEdgeStrategy implements SkipEdgeStrategy {

    private TransitLocalStreetService transitLocalStreets;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private Coordinate finalTarget;

    private double maxWalkDistance;

    public SkipNonTransferEdgeStrategy(RoutingRequest request) {
        Graph graph = request.rctx.graph;
        transitLocalStreets = graph.getService(TransitLocalStreetService.class);
        finalTarget = request.rctx.target.getCoordinate();
        maxWalkDistance = request.getMaxWalkDistance();
    }

    @Override
    public boolean shouldSkipEdge(Vertex origin, Vertex target, State current, Edge edge,
            ShortestPathTree spt, RoutingRequest traverseOptions) {

        final Vertex toVertex = edge.getToVertex();
        if (distanceLibrary.fastDistance(toVertex.getCoordinate(), finalTarget)
                + current.getWalkDistance() > maxWalkDistance)
            
            return !transitLocalStreets.transferrable(toVertex);
        else
            return false;
    }

}
