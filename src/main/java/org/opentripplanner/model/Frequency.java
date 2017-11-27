/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.model;

import java.io.Serializable;
import java.util.Objects;

import static org.opentripplanner.util.TimeToStringConverter.toHH_MM_SS;

public final class Frequency implements Serializable {

    private static final long serialVersionUID = 1L;

    private Trip trip;

    private int startTime;

    private int endTime;

    private int headwaySecs;

    private int exactTimes = 0;

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

    public int getExactTimes() {
        return exactTimes;
    }

    public void setExactTimes(int exactTimes) {
        this.exactTimes = exactTimes;
    }

    public int getLabelOnly() {
        return labelOnly;
    }

    public void setLabelOnly(int labelOnly) {
        this.labelOnly = labelOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Frequency frequency = (Frequency) o;
        return startTime == frequency.startTime && endTime == frequency.endTime
                && headwaySecs == frequency.headwaySecs && Objects.equals(trip, frequency.trip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trip, startTime, endTime, headwaySecs);
    }

    public String toString() {
        return "<Frequency trip=" + trip.getId()
                + " start=" + toHH_MM_SS(startTime)
                + " end=" + toHH_MM_SS(endTime)
                + ">";
    }
}
