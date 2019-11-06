package org.opentripplanner.transit.raptor.speed_test.transit;

import com.conveyal.r5.profile.StreetPath;
import com.conveyal.r5.streets.StreetRouter;
import com.conveyal.r5.transit.TransportNetwork;
import gnu.trove.map.TIntIntMap;
import org.opentripplanner.transit.raptor.speed_test.SpeedTestRequest;
import org.opentripplanner.transit.raptor.util.AvgTimer;

public class EgressAccessRouter {

    private final static AvgTimer TIMER = AvgTimer.timerMilliSec("EgressAccessRouter:route");
    private final TransportNetwork transportNetwork;
    private final SpeedTestRequest request;

    StreetRouter egressRouter;
    StreetRouter accessRouter;

    public TIntIntMap egressTimesToStopsInSeconds;
    public TIntIntMap accessTimesToStopsInSeconds;

    public EgressAccessRouter(TransportNetwork transportNetwork, SpeedTestRequest request) {
        this.transportNetwork = transportNetwork;
        this.request = request;
    }

    public void route() {
        TIMER.time(() -> {
            egressRouter = streetRoute(request, true);
            accessRouter = streetRoute(request, false);

            egressTimesToStopsInSeconds = egressRouter.getReachedStops();
            accessTimesToStopsInSeconds = accessRouter.getReachedStops();
        });
    }

    public StreetPath accessPath(int boardStopIndex) {
        StreetRouter.State accessState = accessRouter.getStateAtVertex(
                transportNetwork.transitLayer.streetVertexForStop.get(boardStopIndex)
        );
        return new StreetPath(accessState, transportNetwork, false);
    }

    public StreetPath egressPath(int alightStopIndex) {
        StreetRouter.State egressState = egressRouter.getStateAtVertex(
                transportNetwork.transitLayer.streetVertexForStop.get(alightStopIndex)
        );
        return new StreetPath(egressState, transportNetwork, false);
    }

    private StreetRouter streetRoute(SpeedTestRequest request, boolean fromDest) {
        // Search for access to / egress from transit on streets.
        StreetRouter sr = new StreetRouter(transportNetwork.streetLayer);
        sr.profileRequest = request;
        if (!fromDest ? !sr.setOrigin(request.fromLat, request.fromLon) : !sr.setOrigin(request.toLat, request.toLon)) {
            throw new RuntimeException("Point not near a road.");
        }
        sr.timeLimitSeconds = request.maxWalkTime * 60;
        sr.quantityToMinimize = StreetRouter.State.RoutingVariable.DURATION_SECONDS;
        sr.route();
        return sr;
    }

}
