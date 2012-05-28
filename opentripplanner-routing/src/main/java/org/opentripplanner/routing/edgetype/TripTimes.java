package org.opentripplanner.routing.edgetype;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

import org.opentripplanner.common.MavenVersion;

/**
 * Holds the arrival / departure times for a single trip in an ArrayTripPattern Also gets carried
 * along by States when routing to ensure that they have a consistent view of the trip when realtime
 * updates are being taken into account. 
 * All times are in seconds since midnight (as in GTFS). The array indexes are actually *hop* indexes,
 * not stop indexes, in the sense that 0 refers to the hop between stops 0 and 1, so arrival 0 is actually
 * an arrival at stop 1. 
 * By making indexes refer to stops not hops we could reuse the departures array for arrivals, 
 * at the cost of an extra entry. This seems more coherent to me (AMB) but would probably break things elsewhere.
 */
public class TripTimes implements Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    @XmlElement
    protected int[] departureTimes;

    @XmlElement
    protected int[] arrivalTimes;

    public TripTimes(int nStops, boolean nullArrivals) {
        // departure arrays could be 1 shorter when arrivals are present, but they are not
        departureTimes = new int[nStops];
        if (nullArrivals) {
            arrivalTimes = null;
        } else {
            arrivalTimes = new int[nStops];
        }
    }

    public int getDepartureTime(int hop) {
        return departureTimes[hop];
    }

    public int getArrivalTime(int hop) {
        if (arrivalTimes == null)
            return departureTimes[hop + 1];
        return arrivalTimes[hop];
    }

}
