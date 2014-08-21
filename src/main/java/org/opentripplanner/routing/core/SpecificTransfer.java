package org.opentripplanner.routing.core;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

/**
 * SpecificTransfer class used by Transfer. Represents a specific transfer between two stops.
 * See the links described at TransferTable for more details about the specifications.
 * @see TransferTable
 */
public class SpecificTransfer implements Serializable {
    
    private static final long serialVersionUID = 5058028994896044775L;

    /**
     * Constant containing the minimum specificity that is allowed by the specifications
     */
    public static final int MIN_SPECIFICITY = 0;

    /**
     * Constant containing the maximum specificity that is allowed by the specifications
     */
    public static final int MAX_SPECIFICITY = 4;
    
    /**
     * Route id of arriving trip. Is allowed to be null. Is ignored when fromTripId is not null.
     */
    final private AgencyAndId fromRouteId;
        
    /**
     * Route id of departing trip. Is allowed to be null. Is ignored when toTripId is not null.
     */
    final private AgencyAndId toRouteId;
    
    /**
     * Trip id of arriving trip. Is allowed to be null.
     */
    final private AgencyAndId fromTripId;
    
    /**
     * Trip id of departing trip. Is allowed to be null.
     */
    final private AgencyAndId toTripId;
    
    /**
     * Value indicating the minimum transfer time in seconds. May contain special (negative) values which meaning
     * can be found in the Transfer.*_TRANSFER constants.
     */
    final int transferTime;
    
    public SpecificTransfer(AgencyAndId fromRouteId, AgencyAndId toRouteId, AgencyAndId fromTripId, AgencyAndId toTripId, int transferTime) {
        this.fromRouteId = fromRouteId;
        this.toRouteId = toRouteId;
        this.fromTripId = fromTripId;
        this.toTripId = toTripId;
        this.transferTime = transferTime;
    }

    public SpecificTransfer(Route fromRoute, Route toRoute, Trip fromTrip, Trip toTrip, int transferTime) {
        if (fromRoute != null) {
            this.fromRouteId = fromRoute.getId();
        }
        else {
            this.fromRouteId = null;
        }
        
        if (toRoute != null) {
            this.toRouteId = toRoute.getId();
        }
        else {
            this.toRouteId = null;
        }
        
        if (fromTrip != null) {
            this.fromTripId = fromTrip.getId();
        }
        else {
            this.fromTripId = null;
        }
        
        if (toTrip != null) {
            this.toTripId = toTrip.getId();
        }
        else {
            this.toTripId = null;
        }

        this.transferTime = transferTime;
    }
    
    /**
     * @return specificity as defined in the specifications
     */
    public int getSpecificity() {
        int specificity = getFromSpecificity() + getToSpecificity();
        assert(specificity >= MIN_SPECIFICITY);
        assert(specificity <= MAX_SPECIFICITY);
        return specificity;
    }
    
    private int getFromSpecificity() {
        int specificity = 0;
        if (fromTripId != null) {
            specificity = 2;
        }
        else if (fromRouteId != null) {
            specificity = 1;
        }
        return specificity;
    }

    private int getToSpecificity() {
        int specificity = 0;
        if (toTripId != null) {
            specificity = 2;
        }
        else if (toRouteId != null) {
            specificity = 1;
        }
        return specificity;
    }
    
    /**
     * Returns whether this specific transfer is applicable to a transfer between
     * two trips.
     * @param fromTrip is the arriving trip
     * @param toTrip is the departing trip
     * @return true if this specific transfer is applicable to a transfer between
     *   two trips.
     */
    public boolean matches(Trip fromTrip, Trip toTrip) {
        boolean match = matchesFrom(fromTrip) && matchesTo(toTrip);
        return match;
    }

    private boolean matchesFrom(Trip trip) {
        checkNotNull(trip);
        
        boolean match = false;
        int specificity = getFromSpecificity();
        if (specificity == 0) {
            match = true;
        }
        else if (specificity == 1) {
            if (trip.getRoute().getId().equals(fromRouteId)) {
                match = true;
            }
        }
        else if (specificity == 2) {
            if (trip.getId().equals(fromTripId)) {
                match = true;
            }
        }
        return match;
    }
    
    private boolean matchesTo(Trip trip) {
        checkNotNull(trip);

        boolean match = false;
        int specificity = getToSpecificity();
        if (specificity == 0) {
            match = true;
        }
        else if (specificity == 1) {
            if (trip.getRoute().getId().equals(toRouteId)) {
                match = true;
            }
        }
        else if (specificity == 2) {
            if (trip.getId().equals(toTripId)) {
                match = true;
            }
        }
        return match;
    }
    
}
