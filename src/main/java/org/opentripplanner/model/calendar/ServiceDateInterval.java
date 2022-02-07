package org.opentripplanner.model.calendar;

import javax.validation.constraints.NotNull;
import java.util.Objects;

import static org.opentripplanner.model.calendar.ServiceDate.MAX_DATE;
import static org.opentripplanner.model.calendar.ServiceDate.MIN_DATE;


/**
 * Value object which represent an service date interval from a starting date
 * until an end date. Both start and end is inclusive.
 *
 * The {@code start} must be equals or before the {@code end} to form a valid
 * period.
 *
 * {@code null} is used to represent an unbounded interval. One or both the of the {@code start} and
 * {@code end} can be {@code null} (unbounded).
 */
public final class ServiceDateInterval {

    private static final ServiceDateInterval UNBOUNDED = new ServiceDateInterval(
            MIN_DATE, MAX_DATE
    );

    private final ServiceDate start;
    private final ServiceDate end;

    public ServiceDateInterval(ServiceDate start, ServiceDate end) {
        this.start = start == null ? MIN_DATE : start;
        this.end = end == null ? MAX_DATE : end;

        // Guarantee that the start is before or equal the end.
        if(this.end.isBefore(this.start)) {
            throw new IllegalArgumentException(
                    "Invalid interval, the end " + end + " is before the start " +  start
            );
        }
    }

    /**
     * Return a interval with start or end unbounded ({@code null}).
     */
    public static ServiceDateInterval unbounded() {
        return UNBOUNDED;
    }

    public boolean isUnbounded() {
        return start.equals(MIN_DATE) && end.equals(MAX_DATE);
    }

    /**
     * Return the interval start, inclusive. If the period start is unbounded the
     * {@link ServiceDate#MIN_DATE} is returned.
     */
    @NotNull
    public ServiceDate getStart() {
        return start;
    }

    /**
     * Return the interval end, inclusive. If the period start is unbounded the
     * {@link ServiceDate#MAX_DATE} is returned.
     */
    @NotNull
    public ServiceDate getEnd() {
        return end;
    }

    /**
     * The intervals have at least one day in common.
     * @see #intersection(ServiceDateInterval)
     */
    public boolean overlap(ServiceDateInterval other) {
        if(start.isBeforeOrEq(other.end)) {
            return end.isAfterOrEq(other.start);
        }
        return false;
    }

    /**
     * Return a new service interval that contains the period with all dates that exist in both
     * periods (intersection of {@code this} and {@code other}).
     *
     * @see #overlap(ServiceDateInterval) for checking an intersection exist.
     *
     * @throws IllegalArgumentException it the to periods do not overlap.
     */
    public ServiceDateInterval intersection(ServiceDateInterval other) {
        return new ServiceDateInterval(
                start.max(other.start),
                end.min(other.end)
        );
    }

    /**
     * Return {@code true} is the given {@code date} exist in this period.
     */
    public boolean include(ServiceDate date) {
        return start.isBeforeOrEq(date) && end.isAfterOrEq(date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ServiceDateInterval that = (ServiceDateInterval) o;
        return start.equals(that.start) && end.equals(that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}
