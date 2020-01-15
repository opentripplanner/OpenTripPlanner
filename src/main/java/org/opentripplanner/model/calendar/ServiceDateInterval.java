package org.opentripplanner.model.calendar;

import java.util.Objects;


/**
 * Value object witch represent an service date interval from a starting date
 * until an end date. Both start and end is inclusive.
 *
 * The {@code start} must be equals or before the {@code end} to form a valid
 * period.
 *
 * {@code null} is used to represent an unbounded interval. One or both the of the {@code start} and
 * {@code end} can be {@code null} (unbounded).
 */
public final class ServiceDateInterval {

    /**
     * The unbounded values will represent an unbounded value internally in this class.
     * They are converted to {@code null} when exported outside the class. This little
     * trick make internal logic a bit simpler, since there is no need for {@code null} checks.
     */
    private static final ServiceDate UNBOUNDED_START = new ServiceDate(0, 1, 1);
    private static final ServiceDate UNBOUNDED_END = new ServiceDate(9999, 12, 31);
    private static final ServiceDateInterval UNBOUNDED = new ServiceDateInterval(null, null);

    private final ServiceDate start;
    private final ServiceDate end;


    public ServiceDateInterval(ServiceDate start, ServiceDate end) {
        this.start = start == null ? UNBOUNDED_START : start;
        this.end = end == null ? UNBOUNDED_END : end;

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
        return start.equals(UNBOUNDED_START) && end.equals(UNBOUNDED_END);
    }

    public ServiceDate getStart() {
        return start == UNBOUNDED_START ? null : start;
    }

    public ServiceDate getEnd() {
        return end == UNBOUNDED_END ? null : end;
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
        return isUnbounded() ? "UNBOUNDED" : "[" + str(getStart()) + ", " + str(getEnd()) + "]";
    }

    private String str(ServiceDate it) {
        return it == null ? "" : it.toString();
    }
}
