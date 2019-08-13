package org.opentripplanner.routing.flex;

import org.opentripplanner.api.model.BoardAlightType;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.edgetype.flex.PartialPatternHop;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Model a transit trip so that assertions can be made against it
 *
 * Similar to DefaultFareServiceImpl.Ride - could be merged
 * Cherry-picked from MTA code
 * */
public class Ride {

    private String startZone;

    private String endZone;

    private List<String> zones = new ArrayList<>();

    private FeedScopedId route;

    private String agency;

    private long startTime;

    private long endTime;

    private Stop firstStop;

    private Stop lastStop;

    private FeedScopedId trip;

    private BoardAlightType boardType = BoardAlightType.DEFAULT;

    private BoardAlightType alightType = BoardAlightType.DEFAULT;

    public String getStartZone() {
        return startZone;
    }

    public String getEndZone() {
        return endZone;
    }

    public List<String> getZones() {
        return zones;
    }

    public FeedScopedId getRoute() {
        return route;
    }

    public String getAgency() {
        return agency;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Stop getFirstStop() {
        return firstStop;
    }

    public Stop getLastStop() {
        return lastStop;
    }

    public FeedScopedId getTrip() {
        return trip;
    }

    public String getFirstStopId() {
        return firstStop.getId().getId();
    }

    public String getLastStopId() {
        return lastStop.getId().getId();
    }

    public BoardAlightType getBoardType() {
        return boardType;
    }

    public BoardAlightType getAlightType() {
        return alightType;
    }

    public static List<Ride> createRides(GraphPath path) {
        List<Ride> rides = new ArrayList<>();
        Ride ride = null;
        for (State state : path.states) {
            Edge edge = state.getBackEdge();
            if ( ! (edge instanceof HopEdge))
                continue;
            HopEdge hEdge = (HopEdge) edge;
            if (ride == null || ! state.getRoute().equals(ride.route)) {
                ride = new Ride();
                rides.add(ride);
                ride.startZone = hEdge.getBeginStop().getZoneId();
                ride.zones.add(ride.startZone);
                ride.agency = state.getBackTrip().getRoute().getAgency().getId();
                ride.route = state.getRoute();
                ride.startTime = state.getBackState().getTimeSeconds();
                ride.firstStop = hEdge.getBeginStop();
                ride.trip = state.getTripId();
                if (hEdge instanceof PartialPatternHop) {
                    PartialPatternHop hop = (PartialPatternHop) hEdge;
                    if (hop.isFlagStopBoard()) {
                        ride.boardType = BoardAlightType.FLAG_STOP;
                    } else if (hop.isDeviatedRouteBoard()) {
                        ride.boardType = BoardAlightType.DEVIATED;
                    }
                }
            }
            ride.lastStop = hEdge.getEndStop();
            ride.endZone  = ride.lastStop.getZoneId();
            ride.zones.add(ride.endZone);
            ride.endTime = state.getTimeSeconds();
            ride.alightType = BoardAlightType.DEFAULT;
            if (hEdge instanceof PartialPatternHop) {
                PartialPatternHop hop = (PartialPatternHop) hEdge;
                if (hop.isFlagStopAlight()) {
                    ride.alightType = BoardAlightType.FLAG_STOP;
                } else if (hop.isDeviatedRouteAlight()) {
                    ride.alightType = BoardAlightType.DEVIATED;
                }
            }

            // in default fare service, classify rides by mode
        }
        return rides;
    }
}
