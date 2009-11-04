package org.opentripplanner.jags.edgetype;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;
import org.opentripplanner.jags.gtfs.GtfsLibrary;

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

    private int runningTime;

    public PatternHop(Stop start, Stop end, int runningTime, TripPattern pattern) {
        this.start = start;
        this.end = end;
        this.runningTime = runningTime;
        this.pattern = pattern;
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

    public WalkResult walk(State state0, WalkOptions wo) {
        System.out.println("hopped from " + state0 + " for  " + runningTime + " to " + end);
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(runningTime);
        return new WalkResult(runningTime, state1);
    }

    public WalkResult walkBack(State state0, WalkOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(-runningTime);
        return new WalkResult(runningTime, state1);
    }

}
