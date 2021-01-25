package org.opentripplanner.transit.raptor.speed_test.transit;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.graph_builder.linking.StreetSplitMode;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.location.RequestScopedStreetLocation;
import org.opentripplanner.transit.raptor.speed_test.model.Place;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Perform a access or egress transit search for stops nearby.
 */
class StreetSearch {
    private static final Logger LOG = LoggerFactory.getLogger(StreetSearch.class);

    private final TransitLayer transitLayer;
    private final Graph graph;
    private final SimpleStreetSplitter splitter;
    private final NearbyStopFinder nearbyStopFinder;
    final TIntIntMap resultTimesSecByStopIndex = new TIntIntHashMap();
    final Map<Integer, NearbyStop> pathsByStopIndex = new HashMap<>();

    StreetSearch(
            TransitLayer transitLayer,
            Graph graph,
            SimpleStreetSplitter splitter,
            NearbyStopFinder nearbyStopFinder
    ) {
        this.transitLayer = transitLayer;
        this.graph = graph;
        this.splitter = splitter;
        this.nearbyStopFinder = nearbyStopFinder;
    }

    /** return access times (in seconds) by stop index */
    void route(Place place, boolean fromOrigin) {
        Vertex vertex = null;

        if(place.stopId != null) {
            vertex = graph.getVertex(place.stopId.getId());
        }
        if(vertex == null) {
            vertex = new RequestScopedStreetLocation(
                    place.name,
                    new Coordinate(place.coordinate.longitude(), place.coordinate.latitude()),
                    new NonLocalizedString(place.name),
                    !fromOrigin
            );
            splitter.linkToWalkableEdge(vertex, StreetSplitMode.REQUEST_SCOPED);
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
