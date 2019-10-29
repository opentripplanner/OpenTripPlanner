package org.opentripplanner.transit.raptor.api.request;


/**
 * Mutable version of the {@link McCostParams}.
 */
public class McCostParamsBuilder {
    private int boardCost;
    private double walkReluctanceFactor;
    private double waitReluctanceFactor;


    McCostParamsBuilder(McCostParams defaults) {
        this.boardCost = defaults.boardCost();
        this.walkReluctanceFactor = defaults.walkReluctanceFactor();
        this.waitReluctanceFactor = defaults.waitReluctanceFactor();
    }

    public int boardCost() {
        return boardCost;
    }

    public org.opentripplanner.transit.raptor.api.request.McCostParamsBuilder boardCost(int boardCost) {
        this.boardCost = boardCost;
        return this;
    }

    public double walkReluctanceFactor() {
        return walkReluctanceFactor;
    }

    public org.opentripplanner.transit.raptor.api.request.McCostParamsBuilder walkReluctanceFactor(double walkReluctanceFactor) {
        this.walkReluctanceFactor = walkReluctanceFactor;
        return this;
    }

    public double waitReluctanceFactor() {
        return waitReluctanceFactor;
    }

    public org.opentripplanner.transit.raptor.api.request.McCostParamsBuilder waitReluctanceFactor(double waitReluctanceFactor) {
        this.waitReluctanceFactor = waitReluctanceFactor;
        return this;
    }
}
