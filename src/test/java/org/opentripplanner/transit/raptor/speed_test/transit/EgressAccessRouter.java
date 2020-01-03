package org.opentripplanner.transit.raptor.speed_test.transit;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.transit.raptor.speed_test.SpeedTestRequest;
import org.opentripplanner.transit.raptor.speed_test.testcase.Place;
import org.opentripplanner.transit.raptor.util.AvgTimer;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EgressAccessRouter {
    private static final Logger LOG = LoggerFactory.getLogger(EgressAccessRouter.class);
    private static final AvgTimer TIMER_INIT = AvgTimer.timerMilliSec("EgressAccessRouter:init");
    private static final AvgTimer TIMER_ROUTE = AvgTimer.timerMilliSec("EgressAccessRouter:route");

    private final TransitLayer transitLayer;
    private final Graph graph;
    private final SimpleStreetSplitter splitter;

    public TIntIntMap egressTimesInSecondsByStopIndex;
    public TIntIntMap accessTimesInSecondsByStopIndex;

    public EgressAccessRouter(Graph graph, TransitLayer transitLayer) {
        try {
            TIMER_INIT.start();
            this.graph = graph;
            this.transitLayer = transitLayer;

            this.splitter = new SimpleStreetSplitter(
                    graph,
                    null,
                    null,
                    false,
                    new DataImportIssueStore(false)
            );
        }
        catch (RuntimeException e) {
            TIMER_INIT.failIfStarted();
            throw e;
        }
        finally {
            TIMER_INIT.stop();
        }
    }

    public void route(SpeedTestRequest request) {
        TIMER_ROUTE.time(() -> {
            // Search for access to / egress from transit on streets.
            NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(
                    graph, request.getAccessEgressMaxWalkDistanceMeters(), true
            );
            accessTimesInSecondsByStopIndex = streetRoute(nearbyStopFinder, request.tc().fromPlace, true);
            egressTimesInSecondsByStopIndex = streetRoute(nearbyStopFinder, request.tc().toPlace, false);
        });
    }

    /** return access times (in seconds) by stop index */
    private TIntIntMap streetRoute(NearbyStopFinder nearbyStopFinder, Place place, boolean fromOrigin) {
        Vertex vertex = null;

        if(place.getStopId() != null) {
            vertex = graph.getVertex(place.getStopId().getId());
        }
        if(vertex == null) {
            vertex = new TemporaryStreetLocation(
                    place.getDescription(),
                    place.getCoordinate(),
                    new NonLocalizedString(place.getDescription()),
                    !fromOrigin
            );
            splitter.link(vertex);
        }

        List<NearbyStopFinder.StopAtDistance> stopAtDistanceList =
                nearbyStopFinder.findNearbyStopsViaStreets(
                        Set.of(vertex),
                        !fromOrigin,
                        false
                );

        if(stopAtDistanceList.isEmpty()) {
            throw new RuntimeException("Point not near a road: " + place);
        }

        TIntIntMap res = new TIntIntHashMap();

        for (NearbyStopFinder.StopAtDistance stopAtDistance : stopAtDistanceList) {
            int stopIndex = transitLayer.getIndexByStop(stopAtDistance.tstop.getStop());
            int accessTimeSec = (int)stopAtDistance.edges.stream().map(Edge::getDistanceMeters)
                    .collect(Collectors.summarizingDouble(Double::doubleValue)).getSum();
            res.put(stopIndex, accessTimeSec);
        }

        LOG.info("Found {} {} stops", res.size(), fromOrigin ?  "access" : "egress");

        return res;
    }
}
