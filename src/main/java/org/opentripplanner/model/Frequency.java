/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.model.base.ToStringBuilder;

public final class Frequency implements Serializable {

    private static final long serialVersionUID = 1L;

    private Trip trip;

    private int startTime;

    private int endTime;

    private int headwaySecs;

    private boolean exactHeadway;

    private int labelOnly = 0;

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public int getHeadwaySecs() {
        return headwaySecs;
    }

    public void setHeadwaySecs(int headwaySecs) {
        this.headwaySecs = headwaySecs;
    }

    /** This return {@code true} if the trip run on a exact same headway throughout the day. */
    public boolean isExactHeadway() {
        return exactHeadway;
    }

    public void setExactHeadway(boolean exactHeadway) {
        this.exactHeadway = exactHeadway;
    }

    public int getLabelOnly() {
        return labelOnly;
    }

    public void setLabelOnly(int labelOnly) {
        this.labelOnly = labelOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Frequency frequency = (Frequency) o;
        return startTime == frequency.startTime && endTime == frequency.endTime
                && headwaySecs == frequency.headwaySecs && Objects.equals(trip, frequency.trip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trip, startTime, endTime, headwaySecs);
    }

    public String toString() {
      return ToStringBuilder.of(Frequency.class)
            .addEntityId("trip", trip)
              .addServiceTime("start", startTime, -1)
              .addServiceTime("end", endTime, -1)
              .addDurationSec("headway", headwaySecs)
              .addBool("exactHeadway", exactHeadway)
              .toString();
    }
}
