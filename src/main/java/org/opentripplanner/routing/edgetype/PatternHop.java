package org.opentripplanner.routing.edgetype;

import java.util.Locale;

import org.locationtech.jts.geom.Point;
import org.opentripplanner.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * A transit vehicle's journey between departure at one stop and arrival at the next.
 * This version represents a set of such journeys specified by a TripPattern.
 */
public class PatternHop extends TablePatternEdge implements OnboardEdge, HopEdge {

    private static final long serialVersionUID = 2L;

    private Stop begin, end;

    public int stopIndex;

    private LineString geometry = null;

    protected PatternHop(PatternStopVertex from, PatternStopVertex to, Stop begin, Stop end, int stopIndex, boolean setInPattern) {
        super(from, to);
        this.begin = begin;
        this.end = end;
        this.stopIndex = stopIndex;
        if (setInPattern) {
            getPattern().setPatternHop(stopIndex, this);
        }
    }

    public PatternHop(PatternStopVertex from, PatternStopVertex to, Stop begin, Stop end, int stopIndex) {
        this(from, to, begin, end, stopIndex, true);
    }

    // made more accurate
    public double getDistance() {
        double distance = 0;
        LineString line = getGeometry();
        for (int i = 0; i < line.getNumPoints() - 1; i++) {
            Point p0 = line.getPointN(i), p1 = line.getPointN(i+1);
            distance += SphericalDistanceLibrary.distance(p0.getCoordinate(), p1.getCoordinate());
        }
        return distance;
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(getPattern().route);
    }
    
    public String getName() {
        return GtfsLibrary.getRouteName(getPattern().route);
    }
    
    @Override
    public String getName(Locale locale) {
        return this.getName();
    }

    public State optimisticTraverse(State state0) {
        RoutingRequest options = state0.getOptions();
        
        // Ignore this edge if either of its stop is banned hard
        if (!options.bannedStopsHard.isEmpty()) {
            if (options.bannedStopsHard.matches(((PatternStopVertex) fromv).getStop())
                    || options.bannedStopsHard.matches(((PatternStopVertex) tov).getStop())) {
                return null;
            }
        }

        int runningTime = (int) timeLowerBound(options);
    	StateEditor s1 = state0.edit(this);
    	s1.incrementTimeInSeconds(runningTime);
    	s1.setBackMode(getMode());
    	s1.incrementWeight(runningTime);
    	return s1.makeState();
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return getPattern().scheduledTimetable.getBestRunningTime(stopIndex);
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }

    @Override
    public State traverse(State s0) {
        return traverse(s0, s0.edit(this));
    }

    public State traverse(State s0, StateEditor s1) {

        RoutingRequest options = s0.getOptions();

        // Ignore this edge if either of its stop is banned hard
        if (!options.bannedStopsHard.isEmpty()) {
            if (options.bannedStopsHard.matches(((PatternStopVertex) fromv).getStop())
                    || options.bannedStopsHard.matches(((PatternStopVertex) tov).getStop())) {
                return null;
            }
        }

        int runningTime = getRunningTime(s0);

        s1.incrementTimeInSeconds(runningTime);
        if (s0.getOptions().arriveBy)
            s1.setZone(getBeginStop().getZoneId());
        else
            s1.setZone(getEndStop().getZoneId());
        //s1.setRoute(pattern.getExemplar().route.getId());
        s1.incrementWeight(getWeight(s0, runningTime));
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    public int getRunningTime(State s0) {
        TripTimes tripTimes = s0.getTripTimes();
        return tripTimes.getRunningTime(stopIndex);
    }

    // allow subclasses to add a weight
    public int getWeight(State s0, int runningTime) {
        return runningTime;
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }

    public LineString getGeometry() {
        if (geometry == null) {

            Coordinate c1 = new Coordinate(begin.getLon(), begin.getLat());
            Coordinate c2 = new Coordinate(end.getLon(), end.getLat());

            geometry = GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] { c1, c2 });
        }
        return geometry;
    }

    @Override
    public Stop getEndStop() {
        return end;
    }

    @Override
    public Stop getBeginStop() {
        return begin;
    }

    @Override
    public String getFeedId() {
        // stops don't really have an agency id, they have the per-feed default id
        return begin.getId().getAgencyId();
    }

    public String toString() {
    	return "PatternHop(" + getFromVertex() + ", " + getToVertex() + ")";
    }

    /**
     * Return true if any GTFS-Flex service is defined for this hop.
     */
    public boolean hasFlexService() {
        return false;
    }

    @Override
    public int getStopIndex() {
        return stopIndex;
    }
}
