package org.opentripplanner.transit.raptor.speed_test.transit;

import gnu.trove.map.TIntIntMap;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.raptor.speed_test.SpeedTestRequest;

public class EgressAccessRouter {

    private final TransitLayer transitLayer;
    private final Graph graph;
    private final VertexLinker linker;
    private final Timer routeTimer;

    private StreetSearch egressSearch;
    private StreetSearch accessSearch;

    public EgressAccessRouter(Graph graph, TransitLayer transitLayer, MeterRegistry registry) {
        this.graph = graph;
        this.transitLayer = transitLayer;
        this.linker = graph.getLinker();
        this.routeTimer = Timer.builder("egressAccessRouter.route").register(registry);
    }

    public void route(SpeedTestRequest request) {
        routeTimer.record(() -> {
            // Search for access to / egress from transit on streets.
            NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(
                    graph, request.getAccessEgressMaxWalkDurationSeconds(), true
            );
            accessSearch = new StreetSearch(transitLayer, graph, linker, nearbyStopFinder);
            egressSearch = new StreetSearch(transitLayer, graph, linker, nearbyStopFinder);

            accessSearch.route(request.tc().fromPlace, true);
            egressSearch.route(request.tc().toPlace, false);
        });
    }

    public TIntIntMap getAccessTimesInSecondsByStopIndex() {
        return accessSearch.resultTimesSecByStopIndex;
    }

    public TIntIntMap getEgressTimesInSecondsByStopIndex() {
        return egressSearch.resultTimesSecByStopIndex;
    }

    NearbyStop getAccessPath(int stopIndex) {
        return accessSearch.pathsByStopIndex.get(stopIndex);
    }

    NearbyStop getEgressPath(int stopIndex) {
        return egressSearch.pathsByStopIndex.get(stopIndex);
    }
}
