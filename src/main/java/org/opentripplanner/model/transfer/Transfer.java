/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfer;

import java.io.Serializable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;

public final class Transfer implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Stop fromStop;

    private final Route fromRoute;

    private final Trip fromTrip;

    private final Stop toStop;

    private final Route toRoute;

    private final Trip toTrip;

    private final TransferType transferType;

    private final int minTransferTimeSeconds;

    public Transfer(Transfer obj) {
        this.fromStop = obj.fromStop;
        this.fromRoute = obj.fromRoute;
        this.fromTrip = obj.fromTrip;
        this.toStop = obj.toStop;
        this.toRoute = obj.toRoute;
        this.toTrip = obj.toTrip;
        this.transferType = obj.transferType;
        this.minTransferTimeSeconds = obj.minTransferTimeSeconds;
    }

    public Transfer(
            Stop fromStop,
            Stop toStop,
            Route fromRoute,
            Route toRoute,
            Trip fromTrip,
            Trip toTrip,
            TransferType transferType,
            int minTransferTimeSeconds
    ) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.fromRoute = fromRoute;
        this.toRoute = toRoute;
        this.fromTrip = fromTrip;
        this.toTrip = toTrip;
        this.transferType = transferType;
        this.minTransferTimeSeconds = minTransferTimeSeconds;
    }

    public Stop getFromStop() {
        return fromStop;
    }

    public Route getFromRoute() {
        return fromRoute;
    }

    public Trip getFromTrip() {
        return fromTrip;
    }

    public Stop getToStop() {
        return toStop;
    }

    public Route getToRoute() {
        return toRoute;
    }

    public Trip getToTrip() {
        return toTrip;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public int getMinTransferTimeSeconds() {
        return minTransferTimeSeconds;
    }

    public String toString() {
        return "<Transfer"
                + toStrOpt(" stop=", fromStop, toStop)
                + toStrOpt(" route=", fromRoute, toRoute)
                + toStrOpt(" trip=", fromTrip, toTrip)
                + ">";
    }

    private static String toStrOpt(String lbl, TransitEntity arg1, TransitEntity arg2) {
        return  (arg1 == null ? "" : (lbl + arg1.getId() + ".." + arg2.getId()));
    }
}
