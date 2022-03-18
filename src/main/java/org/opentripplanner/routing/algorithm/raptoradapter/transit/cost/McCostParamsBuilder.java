package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;


import org.opentripplanner.routing.api.request.RoutingRequest.AccessibilityMode;

/**
 * Mutable version of the {@link McCostParams}.
 */
@SuppressWarnings("UnusedReturnValue")
public class McCostParamsBuilder {
    private int boardCost;
    private int transferCost;
    private int unknownAccessibilityCost;
    private double[] transitReluctanceFactors;
    private double waitReluctanceFactor;
    private AccessibilityMode accessibilityMode;

    public McCostParamsBuilder() {
        this(McCostParams.DEFAULTS);
    }

    private McCostParamsBuilder(McCostParams other) {
        this.boardCost = other.boardCost();
        this.transferCost = other.transferCost();
        this.transitReluctanceFactors = other.transitReluctanceFactors();
        this.waitReluctanceFactor = other.waitReluctanceFactor();
        this.accessibilityMode = other.accessibilityMode();
    }

    public int boardCost() {
        return boardCost;
    }

    public McCostParamsBuilder boardCost(int boardCost) {
        this.boardCost = boardCost;
        return this;
    }

    public int transferCost() {
        return transferCost;
    }

    public McCostParamsBuilder transferCost(int transferCost) {
        this.transferCost = transferCost;
        return this;
    }

    public double[] transitReluctanceFactors() {
        return transitReluctanceFactors;
    }

    public McCostParamsBuilder transitReluctanceFactors(double[] transitReluctanceFactors) {
        this.transitReluctanceFactors = transitReluctanceFactors;
        return this;
    }

    public double waitReluctanceFactor() {
        return waitReluctanceFactor;
    }

    public McCostParamsBuilder waitReluctanceFactor(double waitReluctanceFactor) {
        this.waitReluctanceFactor = waitReluctanceFactor;
        return this;
    }

    public AccessibilityMode accessibilityMode() {
        return accessibilityMode;
    }

    public McCostParamsBuilder accessibilityMode(AccessibilityMode mode) {
        accessibilityMode = mode;
        return this;
    }

    public int unknownAccessibilityCost() {
        return unknownAccessibilityCost;
    }

    public void unknownAccessibilityCost(int unknownAccessibilityCost) {
        this.unknownAccessibilityCost = unknownAccessibilityCost;
    }

    public McCostParams build() {
        return new McCostParams(this);
    }


}
