package org.opentripplanner.routing.edgetype;

import java.util.Calendar;
import java.util.Date;

import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class Alight extends AbstractEdge {

    public Hop hop;

    private static final long serialVersionUID = 1L;

    public Alight(Vertex fromv, Vertex tov, Hop hop) {
        super(fromv, tov);
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
        return TransportationMode.ALIGHTING;
    }

    public String getName() {
        // this text won't be used -- the streetTransitLink or StationEntrance's text will
        return "alight from vehicle";
    }

    public String getStart() {
        return null;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        State s1 = s0.clone();
        return new TraverseResult(1, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        if (!wo.transitAllowed()) {
            return null;
        }
        long currentTime = s0.getTime();
        Date serviceDate = getServiceDate(currentTime, true);
        int secondsSinceMidnight = (int) ((currentTime - serviceDate.getTime()) / 1000);

        CalendarService service = wo.getCalendarService();
        if (!service.getServiceDatesForServiceId(hop.getServiceId()).contains(serviceDate))
            return null;

        int wait = secondsSinceMidnight - hop.getEndStopTime().getArrivalTime();
        if (wait < 0) {
            return null;
        }

        State state1 = s0.clone();
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

        int dayOverflow = scheduleTime / Board.SECS_IN_DAY;
        c.add(Calendar.DAY_OF_YEAR, -dayOverflow);
        return c.getTime();
    }
}
