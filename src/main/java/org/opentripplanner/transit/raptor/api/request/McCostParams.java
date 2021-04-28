package org.opentripplanner.transit.raptor.api.request;


import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.RoutingRequest;

import java.util.Objects;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * This class define how to calculate the cost when cost is part of the multi-criteria pareto function.
 */
public class McCostParams {
    public static final double DEFAULT_TRANSIT_RELUCTANCE = 1.0;

    static final McCostParams DEFAULTS = new McCostParams();

    private final int boardCost;
    private final int transferCost;
    private final double[] transitReluctanceFactors;
    private final double walkReluctanceFactor;
    private final double waitReluctanceFactor;

    /**
     * Default constructor defines default values. These defaults are
     * overridden by defaults in the {@link RoutingRequest}.
     */
    private McCostParams() {
        this.boardCost = 600;
        this.transferCost = 0;
        this.transitReluctanceFactors = null;
        this.walkReluctanceFactor = 4.0;
        this.waitReluctanceFactor = 1.0;
    }

    McCostParams(McCostParamsBuilder builder) {
        this.boardCost = builder.boardCost();
        this.transferCost = builder.transferCost();
        this.transitReluctanceFactors = builder.transitReluctanceFactors();
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
     * The normal transit reluctance is 1.0 - this is the baseline for all other costs. This
     * parameter is used to set a specific reluctance (other than 1.0) to some trips. For example
     * most people like TRAINS over other type of public transport, so it is possible to set the
     * reluctance for RAIL to e.g. 0.9 to give it a small advantage. The OTP domain is responsible
     * for the mapping between this arrays of reluctance values and the index in the {@link
     * RaptorTripSchedule#transitReluctanceFactorIndex()}. Raptor is agnostic to the meaning of the index.
     * But, it MUST match the the {@link RaptorTripSchedule#transitReluctanceFactorIndex()}.
     * <p>
     * If {@code null} is returned the default reluctance 1.0 is used.
     */
    @Nullable
    public double[] transitReluctanceFactors() {
        return transitReluctanceFactors;
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
