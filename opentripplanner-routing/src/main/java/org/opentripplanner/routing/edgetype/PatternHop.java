package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class PatternHop extends AbstractPayload {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private TripPattern pattern;

    private Stop start, end;

    private int stopIndex;

    public PatternHop(Stop start, Stop end, int stopIndex, TripPattern tripPattern) {
        this.start = start;
        this.end = end;
        this.stopIndex = stopIndex;
        this.pattern = tripPattern;
    }

    public String getDirection() {
        return pattern.exemplar.getTripHeadsign();
    }

    public double getDistance() {
        return GtfsLibrary.distance(start.getLat(), start.getLon(), end.getLat(), end.getLon());
    }

    public String getEnd() {
        return end.getName();
    }

    public TransportationMode getMode() {
        return GtfsLibrary.getTransportationMode(pattern.exemplar.getRoute());
    }

    public String getStart() {
        return start.getName();
    }

    public String getName() {
        return GtfsLibrary.getRouteName(pattern.exemplar.getRoute());
    }

    public Geometry getGeometry() {
        // FIXME: use shape if available
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING),
                4326);

        Coordinate c1 = new Coordinate(start.getLon(), start.getLat());
        Coordinate c2 = new Coordinate(end.getLon(), end.getLat());

        return factory.createLineString(new Coordinate[] { c1, c2 });
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        int runningTime = pattern.getRunningTime(stopIndex, state0.getPattern());
        state1.incrementTimeInSeconds(runningTime);
        return new TraverseResult(runningTime, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        int runningTime = pattern.getRunningTime(stopIndex, state0.getPattern());
        state1.incrementTimeInSeconds(-runningTime);
        return new TraverseResult(runningTime, state1);
    }

}
