package org.opentripplanner.routing.edgetype;

import java.util.Calendar;
import java.util.Date;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class PatternAlight extends AbstractEdge {

    /**
     * Models alighting from a vehicle - that is to say, traveling from a station on vehicle to a
     * station off vehicle. When traversed backwards, the the resultant state has the time of the
     * previous arrival, in addition the pattern that was boarded. When traversed forwards, the
     * result state is unchanged. An boarding penalty can also be applied to discourage transfers.
     */

    private static final long serialVersionUID = 1042740795612978747L;

    private static final long MILLI_IN_DAY = 24 * 60 * 60 * 1000;

    private static final int SEC_IN_DAY = 24 * 60 * 60;

    private static final int BOARD_COST = 120;

    private TripPattern pattern;

    private int stopIndex;

    public PatternAlight(Vertex startStation, Vertex startJourney, TripPattern pattern,
            int stopIndex) {
        super(startStation, startJourney);
        this.pattern = pattern;
        this.stopIndex = stopIndex;
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public String getEnd() {
        return null;
    }

    public Geometry getGeometry() {
        return null;
    }

    public TransportationMode getMode() {
        return TransportationMode.ALIGHTING;
    }

    public String getName() {
        return "leave transit network for street network";
    }

    public String getStart() {
        return null;
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        if (!wo.transitAllowed()) {
            return null;
        }
        long currentTime = state0.getTime();
        Date serviceDate = getServiceDate(currentTime, wo.calendar);
        Date serviceDateYesterday = getServiceDate(currentTime - MILLI_IN_DAY, wo.calendar);
        int secondsSinceMidnight = (int) ((currentTime - serviceDate.getTime()) / 1000);

        int wait = 1;
        int patternIndex = -1;
        AgencyAndId service = pattern.exemplar.getServiceId();
        if (wo.serviceOn(service, serviceDate)) {
            // try to get the departure time on today's schedule
            patternIndex = pattern.getPreviousPattern(stopIndex, secondsSinceMidnight);
            if (patternIndex >= 0) {
                wait = pattern.getArrivalTime(stopIndex, patternIndex) - secondsSinceMidnight;
            }
        }
        if (wo.serviceOn(service, serviceDateYesterday)) {
            // now, try to get the departure time on yesterday's schedule -- assuming that
            // yesterday's is on the same schedule as today. If it's not, then we'll worry about it
            // when we get to the pattern(s) which do contain yesterday.
            int yesterdayPatternIndex = pattern.getPreviousPattern(stopIndex, secondsSinceMidnight
                    + SEC_IN_DAY);
            if (yesterdayPatternIndex >= 0) {
                int waitYesterday = pattern.getArrivalTime(stopIndex, yesterdayPatternIndex)
                        - secondsSinceMidnight - SEC_IN_DAY;
                if (wait > 0 || waitYesterday > wait) {
                    // choose the better time
                    wait = waitYesterday;
                    patternIndex = yesterdayPatternIndex;
                }
            }
        }

        if (wait > 0) {
            return null;
        }
        State state1 = state0.clone();
        state1.setPattern(patternIndex);
        state1.incrementTimeInSeconds(wait);
        return new TraverseResult(-wait + BOARD_COST, state1);
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State s1 = state0.clone();
        return new TraverseResult(1, s1);
    }

    private Date getServiceDate(long currentTime, Calendar c) {
        c.setTimeInMillis(currentTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
