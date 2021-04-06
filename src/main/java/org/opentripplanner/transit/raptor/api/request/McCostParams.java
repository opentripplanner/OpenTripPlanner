package org.opentripplanner.transit.raptor.api.request;


import org.opentripplanner.routing.api.request.RoutingRequest;

import java.util.Objects;

/**
 * This class define how to calculate the cost when cost is part of the multi-criteria pareto function.
 */
public class McCostParams {
    static final McCostParams DEFAULTS = new McCostParams();

    private final int boardCost;
    private final int transferCost;
    private final double walkReluctanceFactor;
    private final double waitReluctanceFactor;

    /**
     * Default constructor defines default values. These defaults are
     * overridden by defaults in the {@link RoutingRequest}.
     */
    private McCostParams() {
        this.boardCost = 600;
        this.transferCost = 0;
        this.walkReluctanceFactor = 4.0;
        this.waitReluctanceFactor = 1.0;
    }

    McCostParams(McCostParamsBuilder builder) {
        this.boardCost = builder.boardCost();
        this.transferCost = builder.transferCost();
        this.walkReluctanceFactor = builder.walkReluctanceFactor();
        this.waitReluctanceFactor = builder.waitReluctanceFactor();
    }

    public int boardCost() {
        return boardCost;
    }

    public int transferCost() {
        return transferCost;
    }

    /**
     * A walk reluctance factor of 100 regarded as neutral. 400 means the rider
     * would rater sit 4 minutes extra on a buss, than walk 1 minute extra.
     */
    public double walkReluctanceFactor() {
        return walkReluctanceFactor;
    }

    public double waitReluctanceFactor() {
        return waitReluctanceFactor;
    }

    @Override
    public String toString() {
        return "McCostParams{" +
                "boardCost=" + boardCost +
                ", transferCost=" + transferCost +
                ", transferReluctanceFactor=" + walkReluctanceFactor +
                ", waitReluctanceFactor=" + waitReluctanceFactor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        McCostParams that = (McCostParams) o;
        return boardCost == that.boardCost &&
                transferCost == that.transferCost &&
                Double.compare(that.walkReluctanceFactor, walkReluctanceFactor) == 0 &&
                Double.compare(that.waitReluctanceFactor, waitReluctanceFactor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardCost, transferCost, walkReluctanceFactor, waitReluctanceFactor);
    }
}
