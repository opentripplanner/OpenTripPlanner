package org.opentripplanner.model;

import java.util.Objects;

/**
 * Immutable value object for stop level. This is currently only supported by
 * the GTFS import.
 */
public class StopLevel {
    private final String name;
    private final double index;

    public StopLevel(String name, double index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public double getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return name + "(" + index + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        StopLevel other = (StopLevel) o;
        return Math.abs(other.index - index) < 0.001 && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, index);
    }
}
