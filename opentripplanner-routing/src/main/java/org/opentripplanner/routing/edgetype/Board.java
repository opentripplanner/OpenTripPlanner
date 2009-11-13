package org.opentripplanner.routing.edgetype;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class Board extends AbstractEdge {

    String start_id; // a street vertex's id

    String end_id; // a transit node's GTFS id

    public Hop hop;

    private static final int SECS_IN_DAY = 86400;

    private static final long serialVersionUID = 2L;

    public Board(Vertex startStation, Vertex startJourney, Hop hop) {
        super(startStation, startJourney);
        this.hop = hop;
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
        return TransportationMode.BOARDING;
    }

    public String getName() {
        // "Enter 7th Avenue Station"
        return "leave street network for transit network";
    }

    public String getStart() {
        return null;
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {

        long currentTime = state0.getTime();
        Date serviceDate = getServiceDate(currentTime, false);
        int secondsSinceMidnight = (int) ((currentTime - serviceDate.getTime()) / 1000);

        CalendarService service = wo.getGtfsContext().getCalendarService();
        Set<Date> serviceDates = service.getServiceDatesForServiceId(hop.getServiceId());
        if (!serviceDates.contains(serviceDate))
            return null;

        int wait = hop.getStartStopTime().getDepartureTime() - secondsSinceMidnight;
        if (wait < 0) {
            return null;
        }

        State state1 = state0.clone();
        state1.incrementTimeInSeconds(wait);
        return new TraverseResult(wait, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        long currentTime = state0.getTime();
        Date serviceDate = getServiceDate(currentTime, true);
        int secondsSinceMidnight = (int) ((currentTime - serviceDate.getTime()) / 1000);

        CalendarService service = wo.getGtfsContext().getCalendarService();
        if (!service.getServiceDatesForServiceId(hop.getServiceId()).contains(serviceDate))
            return null;

        int wait = secondsSinceMidnight - hop.getEndStopTime().getArrivalTime();
        if (wait < 0) {
            return null;
        }

        State state1 = state0.clone();
        state1.incrementTimeInSeconds(-wait);
        return new TraverseResult(wait, state1);
    }

    private Date getServiceDate(long currentTime, boolean useArrival) {
        int scheduleTime = useArrival ? hop.getEndStopTime().getArrivalTime() : hop
                .getStartStopTime().getDepartureTime();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int dayOverflow = scheduleTime / SECS_IN_DAY;
        c.add(Calendar.DAY_OF_YEAR, -dayOverflow);
        return c.getTime();
    }
}
