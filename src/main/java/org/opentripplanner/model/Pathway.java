/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Pathway extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = -2404871423254094109L;

    private static final int MISSING_VALUE = -999;

    private FeedScopedId id;

    private int pathwayType;

    private Stop fromStop;

    private Stop toStop;

    private int traversalTime;

    private int wheelchairTraversalTime = MISSING_VALUE;

    @Override
    public FeedScopedId getId() {
        return id;
    }

    @Override
    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public void setPathwayType(int pathwayType) {
        this.pathwayType = pathwayType;
    }

    public int getPathwayType() {
        return pathwayType;
    }

    public void setFromStop(Stop fromStop) {
        this.fromStop = fromStop;
    }

    public Stop getFromStop() {
        return fromStop;
    }

    public void setToStop(Stop toStop) {
        this.toStop = toStop;
    }

    public Stop getToStop() {
        return toStop;
    }

    public void setTraversalTime(int traversalTime) {
        this.traversalTime = traversalTime;
    }

    public int getTraversalTime() {
        return traversalTime;
    }

    public void setWheelchairTraversalTime(int wheelchairTraversalTime) {
        this.wheelchairTraversalTime = wheelchairTraversalTime;
    }

    public int getWheelchairTraversalTime() {
        return wheelchairTraversalTime;
    }

    public boolean isWheelchairTraversalTimeSet() {
        return wheelchairTraversalTime != MISSING_VALUE;
    }

    public void clearWheelchairTraversalTime() {
        this.wheelchairTraversalTime = MISSING_VALUE;
    }

    @Override
    public String toString() {
        return "<Pathway " + this.id + ">";
    }
}
