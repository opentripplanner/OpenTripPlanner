package org.opentripplanner.scripting.api;

/**
 * This class encapsulate both an individual and evaluated values (time...).
 * 
 * Objects are returned by the eval method of the SPT.
 * 
 * @see OtpsSPT.eval()
 * 
 * @author laurent
 */
public class OtpsEvaluatedIndividual {

    private OtpsIndividual individual;

    private long time;

    private int boardings;

    private double walkDistance;

    protected OtpsEvaluatedIndividual(OtpsIndividual individual, long time, int boardings,
            double walkDistance) {
        this.individual = individual;
        this.time = time;
        this.boardings = boardings;
        this.walkDistance = walkDistance;
    }

    /**
     * @return The time, in seconds, for this evualuated individual. Return null/None if no time is
     *         available (position not snapped or out of time range).
     */
    public Long getTime() {
        if (time == Long.MAX_VALUE)
            return null;
        return time;
    }

    /**
     * @return The number of boardings to get to this point (this is the number of transfers +1).
     *         Return 0 for walk only path. Return null/None if no information is available at this
     *         point (position not snapped or out of evaluated time range).
     */
    public Integer getBoardings() {
        if (boardings == 255) // TODO Use a constant
            return null;
        return boardings;
    }

    /**
     * @return The distance in meters walked to get to this point. Return null/None if no
     *         information is available at this point.
     */
    public Double getWalkDistance() {
        if (Double.isNaN(walkDistance))
            return null;
        return walkDistance;
    }

    /**
     * @return The individual evaluated (the same individual from the evuluated population).
     */
    public OtpsIndividual getIndividual() {
        return individual;
    }

    @Override
    public String toString() {
        return individual.toString() + " -> t=" + (getTime() == null ? "null" : (getTime() + "s"));
    }
}
