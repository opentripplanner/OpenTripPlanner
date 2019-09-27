package org.opentripplanner.routing.edgetype.flex;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

/** A PatternHop with GTFS-Flex service enabled. */
public class FlexPatternHop extends PatternHop {

    private static final long serialVersionUID = 1L;

    private RequestStops requestPickup = RequestStops.NO;

    private RequestStops requestDropoff = RequestStops.NO;

    private double serviceAreaRadius = 0d;

    private Geometry serviceArea = null;

    public FlexPatternHop(PatternStopVertex from, PatternStopVertex to, Stop begin, Stop end, int stopIndex) {
        super(from, to, begin, end, stopIndex, true);
    }

    protected FlexPatternHop(PatternStopVertex from, PatternStopVertex to, Stop begin, Stop end, int stopIndex, boolean setInPattern) {
        super(from, to, begin, end, stopIndex, setInPattern);
    }

    /**
     * Return the permissions associated with unscheduled pickups in between the endpoints of this
     * PatternHop. This relates to flag-stops in the GTFS-Flex specification; if flex and/or flag
     * stops are not enabled, this will always be RequestStops.NO.
     */
    public RequestStops getRequestPickup() {
        return requestPickup;
    }

    /**
     * Return the permissions associated with unscheduled dropoffs in between the endpoints of this
     * PatternHop. This relates to flag-stops in the GTFS-Flex specification; if flex and/or flag
     * stops are not enabled, this will always be RequestStops.NO.
     */
    public RequestStops getRequestDropoff() {
        return requestDropoff;
    }

    /**
     * Return whether flag stops are enabled in this hop. Flag stops are enabled if either pickups
     * or dropoffs at unscheduled locations can be requested. This is a GTFS-Flex feature.
     */
    private boolean hasFlagStopService() {
        return requestPickup.allowed() || requestDropoff.allowed();
    }

    @Override
    public boolean hasFlexService() {
        return hasFlagStopService() || getServiceAreaRadius() > 0 || getServiceArea() != null;
    }

    public boolean canRequestService(boolean boarding) {
        return boarding ? requestPickup.allowed() : requestDropoff.allowed();
    }

    public double getServiceAreaRadius() {
        return serviceAreaRadius;
    }

    public Geometry getServiceArea() {
        return serviceArea;
    }

    public boolean hasServiceArea() {
        return serviceArea != null;
    }

    public void setRequestPickup(RequestStops requestPickup) {
        this.requestPickup = requestPickup;
    }

    public void setRequestPickup(int code) {
        setRequestPickup(RequestStops.fromGtfs(code));
    }

    public void setRequestDropoff(RequestStops requestDropoff) {
        this.requestDropoff = requestDropoff;
    }

    public void setRequestDropoff(int code) {
        setRequestDropoff(RequestStops.fromGtfs(code));
    }

    public void setServiceAreaRadius(double serviceAreaRadius) {
        this.serviceAreaRadius = serviceAreaRadius;
    }

    public void setServiceArea(Geometry serviceArea) {
        this.serviceArea = serviceArea;
    }

    private enum RequestStops {
        NO(1), YES(0), PHONE(2), COORDINATE_WITH_DRIVER(3);

        final int gtfsCode;

        RequestStops(int gtfsCode) {
            this.gtfsCode = gtfsCode;
        }

        private static RequestStops fromGtfs(int code) {
            for (RequestStops it : values()) {
                if(it.gtfsCode == code) {
                    return it;
                }
            }
            return NO;
        }

        boolean allowed() {
            return this != NO;
        }
    }
}
