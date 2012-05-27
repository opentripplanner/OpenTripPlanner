package org.opentripplanner.routing.edgetype;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

import org.opentripplanner.common.MavenVersion;

/**
 * Holds the arrival / departure times for a single trip in an ArrayTripPattern Also gets carried
 * along by States when routing to ensure that they have a consistent view of the trip when realtime
 * updates are being taken into account.
 */
public class TripTimes implements Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    @XmlElement
    protected int[] departureTimes;

    @XmlElement
    protected int[] arrivalTimes;

    // by making indexes refer to stops not hops we could reuse the departures array for arrivals,
    // at the cost of 2 extra entries
    public TripTimes(int nStops, boolean nullArrivals) {
        if (nullArrivals) {
            departureTimes = new int[nStops];
            arrivalTimes = null;
        } else {
            // arrays could be 1 shorter when arrivals are present, but they are not
            departureTimes = new int[nStops];
            arrivalTimes = new int[nStops];
        }
    }

    public int getDepartureTime(int hop) {
        return departureTimes[hop];
    }

    // array indexes are actually _hop_ indexes, not stop indexes; 0 refers to the hop between stops
    // 0 and 1.
    public int getArrivalTime(int hop) {
        if (arrivalTimes == null)
            return departureTimes[hop + 1];
        return arrivalTimes[hop];
    }

}
