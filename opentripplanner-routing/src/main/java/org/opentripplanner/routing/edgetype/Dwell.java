package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.StopTime;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class Dwell extends AbstractEdge {

    /*
     * Models waiting in a station where passengers may remain on the vehicle. This may be useful
     * for its simplicity, but has been largely replaced by PatternDwell
     */


    private static final long serialVersionUID = -7761092317912812048L;

    private StopTime stopTime;

    private AgencyAndId _serviceId;

    private int elapsed;

    public AgencyAndId getServiceId() {
        return _serviceId;
    }

    public Dwell(Vertex startJourney, Vertex endJourney, StopTime stopTime)
            throws Exception {
        super(startJourney, endJourney);
        this.stopTime = stopTime;
        this._serviceId = stopTime.getTrip().getServiceId();
        this.elapsed = stopTime.getDepartureTime() - stopTime.getArrivalTime();
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(elapsed);
        return new TraverseResult(elapsed, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(-elapsed);
        return new TraverseResult(elapsed, state1);
    }

    public String toString() {
        return "Dwell(" + this.stopTime + ")";
    }

    public String getDirection() {
        return stopTime.getTrip().getTripHeadsign();
    }

    public double getDistance() {
        return 0;
    }

    public String getEnd() {
        return null;
    }

    public TransportationMode getMode() {
        return GtfsLibrary.getTransportationMode(stopTime.getTrip().getRoute());
    }

    public String getStart() {
        return null;
    }

    public String getName() {
        return GtfsLibrary.getRouteName(stopTime.getTrip().getRoute());
    }

    public Geometry getGeometry() {
        return null;
    }
}
