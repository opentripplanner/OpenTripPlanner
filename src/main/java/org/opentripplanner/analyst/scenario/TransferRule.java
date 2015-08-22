package org.opentripplanner.analyst.scenario;

import org.opentripplanner.profile.RaptorWorkerTimetable;

import java.util.stream.IntStream;

/**
 * A transfer rule allows specification, on a per-stop basis, of a different assumption for transfer
 * boarding times than that used in the graph-wide search.
 */
public class TransferRule extends Modification {
    private static final long serialVersionUID = 1L;

    /** The boarding assumption to use for matched transfers */
    public RaptorWorkerTimetable.BoardingAssumption assumption;

    /** From GTFS modes; note constants in Route */
    public int[] fromMode;

    /** To GTFS modes */
    public int[] toMode;

    /** Stop label; if null will be applied to all stops. */
    public String stop;

    /**
     * The transfer time in seconds; only applied if assumption == FIXED.
     * If specified and assumption == FIXED, this transfer will always take this amount of time,
     * so this should be the time difference between vehicles. No provision is made for additional
     * slack for walking, etc., so be sure to allow enough time to be reasonable for transferring
     * at this stop.
     */
    public Integer transferTimeSeconds;

    /**
     * The proportion of the headway to apply when transferring, when assumption = PROPORTION.
     *
     * So, for example, if you set this to 0.33, and the buses run at thirty-minute frequency,
     * the wait time will be ten minutes.
     *
     * There is theoretical justification for setting this below 0.5 when the frequency-based network
     * under consideration will eventually have a schedule, even if transfers have not been and will
     * not be explicitly synchronized. Suppose all possible transfer times as a
     * proportion of headway are drawn from a distribution centered on 0.5. Now consider that there are
     * likely several competing options for each trip (how many depends on how well-connected the
     * network is). The trip planner will pick the best one, and the best one is likely to have a
     * transfer time of less than half headway (because transfer time is correlated with total trip
     * time, the objective function). Thus the average transfer time in optimal trips is less than
     * half headway.
     *
     * The same is not true of the initial wait, assuming the user gets to their the same way each time
     * they go there (which is probably true of most people who do not write transportation analytics
     * software for a living). If you leave your origin at random and do the same thing every day,
     * you will experience, on average, half headway waits. However, the schedule is fixed (the fact
     * that you're hitting a slightly different part of it each day notwithstanding), so it is reasonable
     * to optimize the choice of which sequence of routes to take, just not which to take on a given day.
     */
    public Double waitProportion;

    @Override public String getType() {
        return "transfer-rule";
    }

    public boolean matches (RaptorWorkerTimetable from, RaptorWorkerTimetable to) {
        if (fromMode != null && !IntStream.of(fromMode).anyMatch(m -> m == from.mode))
            return false;

        if (toMode != null && !IntStream.of(toMode).anyMatch(m -> m == to.mode))
            return false;

        return true;
    }
}
