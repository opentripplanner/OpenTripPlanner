package org.opentripplanner.routing.edgetype;

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class PatternInterlineDwell extends AbstractEdge {

    private static final long serialVersionUID = 1L;

    private Map<AgencyAndId, Integer> tripIdToDwellTime;

    private Map<AgencyAndId, Integer> reverseTripIdToDwellTime;

    private Trip targetTrip;

    public PatternInterlineDwell(Vertex startJourney, Vertex endJourney, Trip targetTrip) {
        super(startJourney, endJourney);
        this.tripIdToDwellTime = new HashMap<AgencyAndId, Integer>();
        this.reverseTripIdToDwellTime = new HashMap<AgencyAndId, Integer>();
        this.targetTrip = targetTrip;
    }

    public void addTrip(AgencyAndId trip, AgencyAndId reverseTrip, int dwellTime) {
        if (dwellTime < 0) {
            throw new RuntimeException("Negative dwell time");
        }
        tripIdToDwellTime.put(trip, dwellTime);
        reverseTripIdToDwellTime.put(reverseTrip, dwellTime);
    }

    public String getDirection() {
        return targetTrip.getTripHeadsign();
    }

    public double getDistance() {
        return 0;
    }

    public String getEnd() {
        return null;
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(targetTrip.getRoute());
    }

    public String getStart() {
        return null;
    }

    public String getName() {
        return GtfsLibrary.getRouteName(targetTrip.getRoute());
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();

        AgencyAndId tripId = state0.tripId;
        Integer dwellTime = tripIdToDwellTime.get(tripId);
        if (dwellTime == null) {
            return null;
        }
        state1.incrementTimeInSeconds(dwellTime);
        state1.tripId = targetTrip.getId();
        return new TraverseResult(dwellTime, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();

        AgencyAndId tripId = state0.tripId;
        Integer dwellTime = reverseTripIdToDwellTime.get(tripId);
        if (dwellTime == null) {
            return null;
        }
        state1.incrementTimeInSeconds(-dwellTime);
        state1.tripId = targetTrip.getId();
        return new TraverseResult(dwellTime, state1);
    }

    public Geometry getGeometry() {
        return null;
    }

    public String toString() {
        return "PatternDwell(" + super.toString() + ")";
    }
}
