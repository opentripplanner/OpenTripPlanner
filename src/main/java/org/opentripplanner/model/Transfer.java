/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.io.Serializable;

public final class Transfer implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    private Stop fromStop;

    private Route fromRoute;

    private Trip fromTrip;

    private Stop toStop;

    private Route toRoute;

    private Trip toTrip;

    private int transferType;

    private int minTransferTime = MISSING_VALUE;

    public Transfer() {
    }

    public Transfer(Transfer obj) {
        this.fromStop = obj.fromStop;
        this.fromRoute = obj.fromRoute;
        this.fromTrip = obj.fromTrip;
        this.toStop = obj.toStop;
        this.toRoute = obj.toRoute;
        this.toTrip = obj.toTrip;
        this.transferType = obj.transferType;
        this.minTransferTime = obj.minTransferTime;
    }

    public Stop getFromStop() {
        return fromStop;
    }

    public void setFromStop(Stop fromStop) {
        this.fromStop = fromStop;
    }

    public Route getFromRoute() {
        return fromRoute;
    }

    public void setFromRoute(Route fromRoute) {
        this.fromRoute = fromRoute;
    }

    public Trip getFromTrip() {
        return fromTrip;
    }

    public void setFromTrip(Trip fromTrip) {
        this.fromTrip = fromTrip;
    }

    public Stop getToStop() {
        return toStop;
    }

    public void setToStop(Stop toStop) {
        this.toStop = toStop;
    }

    public Route getToRoute() {
        return toRoute;
    }

    public void setToRoute(Route toRoute) {
        this.toRoute = toRoute;
    }

    public Trip getToTrip() {
        return toTrip;
    }

    public void setToTrip(Trip toTrip) {
        this.toTrip = toTrip;
    }

    public int getTransferType() {
        return transferType;
    }

    public void setTransferType(int transferType) {
        this.transferType = transferType;
    }

    public boolean isMinTransferTimeSet() {
        return minTransferTime != MISSING_VALUE;
    }

    public int getMinTransferTime() {
        return minTransferTime;
    }

    public void setMinTransferTime(int minTransferTime) {
        this.minTransferTime = minTransferTime;
    }

    public void clearMinTransferTime() {
        this.minTransferTime = MISSING_VALUE;
    }

    public String toString() {
        return "<Transfer"
                + toStrOpt(" stop=", fromStop, toStop)
                + toStrOpt(" route=", fromRoute, toRoute)
                + toStrOpt(" trip=", fromTrip, toTrip)
                + ">";
    }

    private static String toStrOpt(String lbl, IdentityBean arg1, IdentityBean arg2) {
        return  (arg1 == null ? "" : (lbl + arg1.getId() + ".." + arg2.getId()));
    }
}
