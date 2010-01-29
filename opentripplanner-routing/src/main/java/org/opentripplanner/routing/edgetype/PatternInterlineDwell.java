package org.opentripplanner.routing.edgetype;

import java.io.Serializable;
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

class InterlineDwellData implements Serializable {

    private static final long serialVersionUID = 1L;
    public int dwellTime;
    public int patternIndex;
    public InterlineDwellData(int dwellTime, int patternIndex) {
        this.dwellTime = dwellTime;
        this.patternIndex = patternIndex;
    }
}

public class PatternInterlineDwell extends AbstractEdge {

    private static final long serialVersionUID = 1L;

    private Map<AgencyAndId, InterlineDwellData> tripIdToInterlineDwellData;

    private Map<AgencyAndId, InterlineDwellData> reverseTripIdToInterlineDwellData;

    private Trip targetTrip;

    public PatternInterlineDwell(Vertex startJourney, Vertex endJourney, Trip targetTrip) {
        super(startJourney, endJourney);
        this.tripIdToInterlineDwellData = new HashMap<AgencyAndId, InterlineDwellData>();
        this.reverseTripIdToInterlineDwellData = new HashMap<AgencyAndId, InterlineDwellData>();
        this.targetTrip = targetTrip;
    }

    public void addTrip(AgencyAndId trip, AgencyAndId reverseTrip, int dwellTime, int oldPatternIndex, int newPatternIndex) {
        if (dwellTime < 0) {
            throw new RuntimeException("Negative dwell time");
        }
        tripIdToInterlineDwellData.put(trip, new InterlineDwellData(dwellTime, newPatternIndex));
        reverseTripIdToInterlineDwellData.put(reverseTrip, new InterlineDwellData(dwellTime, newPatternIndex));
    }

    public String getDirection() {
        return targetTrip.getTripHeadsign();
    }

    public double getDistance() {
        return 0;
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(targetTrip.getRoute());
    }

    public String getName() {
        return GtfsLibrary.getRouteName(targetTrip.getRoute());
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();

        AgencyAndId tripId = state0.tripId;
        InterlineDwellData dwellData = tripIdToInterlineDwellData.get(tripId);
        if (dwellData == null) {
            return null;
        }
        state1.incrementTimeInSeconds(dwellData.dwellTime);
        state1.tripId = targetTrip.getId();
        state1.setPattern(dwellData.patternIndex);

        return new TraverseResult(dwellData.dwellTime, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();

        AgencyAndId tripId = state0.tripId;
        InterlineDwellData dwellData = reverseTripIdToInterlineDwellData.get(tripId);
        if (dwellData == null) {
            return null;
        }
        state1.incrementTimeInSeconds(-dwellData.dwellTime);
        state1.tripId = targetTrip.getId();
        state1.setPattern(dwellData.patternIndex);
        return new TraverseResult(dwellData.dwellTime, state1);
    }

    public Geometry getGeometry() {
        return null;
    }

    public String toString() {
        return "PatterninterlineDwell(" + super.toString() + ")";
    }
}
