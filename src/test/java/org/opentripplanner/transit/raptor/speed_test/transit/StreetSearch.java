package org.opentripplanner.transit.raptor.speed_test.transit;

import static org.opentripplanner.graph_builder.linking.LinkingDirection.INCOMING;
import static org.opentripplanner.graph_builder.linking.LinkingDirection.OUTGOING;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.transit.raptor.speed_test.model.Place;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Perform a access or egress transit search for stops nearby.
 */
class StreetSearch {
    private static final Logger LOG = LoggerFactory.getLogger(StreetSearch.class);

    private final TransitLayer transitLayer;
    private final Graph graph;
    private final VertexLinker linker;
    private final NearbyStopFinder nearbyStopFinder;
    final TIntIntMap resultTimesSecByStopIndex = new TIntIntHashMap();
    final Map<Integer, NearbyStop> pathsByStopIndex = new HashMap<>();

    StreetSearch(
            TransitLayer transitLayer,
            Graph graph,
            VertexLinker splitter,
            NearbyStopFinder nearbyStopFinder
    ) {
        this.transitLayer = transitLayer;
        this.graph = graph;
        this.linker = splitter;
        this.nearbyStopFinder = nearbyStopFinder;
    }

    /** return access times (in seconds) by stop index */
    void route(Place place, boolean fromOrigin) {
        Vertex vertex = null;

        if(place.stopId != null) {
            vertex = graph.getVertex(place.stopId.getId());
        }
        if(vertex == null) {
            vertex = new TemporaryStreetLocation(
                    place.name,
                    new Coordinate(place.coordinate.longitude(), place.coordinate.latitude()),
                    new NonLocalizedString(place.name),
                    !fromOrigin
            );

            LinkingDirection direction;
            BiFunction<Vertex, StreetVertex, List<Edge>> createEdgeOp;

            if(fromOrigin) {
                direction = INCOMING;
                createEdgeOp = (t, v) -> List.of(
                        new TemporaryFreeEdge((TemporaryStreetLocation) t, v)
                );
            }
            else {
                direction = OUTGOING;
                createEdgeOp = (t, v) -> List.of(
                        new TemporaryFreeEdge(v, (TemporaryStreetLocation) t)
                );
            }
            linker.linkVertexForRequest(
                vertex,
                new TraverseModeSet(TraverseMode.WALK),
                direction,
                createEdgeOp
            );
        }

        List<NearbyStop> nearbyStopList = nearbyStopFinder.findNearbyStopsViaStreets(
                Set.of(vertex), !fromOrigin, true
        );

        if(nearbyStopList.isEmpty()) {
            throw new RuntimeException("No stops found nearby: " + place);
        }

        for (NearbyStop nearbyStop : nearbyStopList) {
            if (!(nearbyStop.stop instanceof Stop)) continue;
            int stopIndex = transitLayer.getIndexByStop((Stop) nearbyStop.stop);
            int accessTimeSec = (int) nearbyStop.edges.stream().map(Edge::getDistanceMeters)
                    .collect(Collectors.summarizingDouble(Double::doubleValue)).getSum();
            resultTimesSecByStopIndex.put(stopIndex, accessTimeSec);
            pathsByStopIndex.put(stopIndex, nearbyStop);
        }

        LOG.debug("Found {} {} stops", resultTimesSecByStopIndex.size(), fromOrigin ?  "access" : "egress");
    }
}
