package org.opentripplanner.routing.edgetype;

public class SlopeCosts {
    public double slopeSpeedEffectiveLength;
    public double slopeWorkCost; // the cost in watt-seconds at 5 m/s
    public double maxSlope;
    
    public SlopeCosts(double slopeSpeedEffectiveLength, double slopeWorkCost, double maxSlope) {
        this.slopeSpeedEffectiveLength = slopeSpeedEffectiveLength;
        this.slopeWorkCost = slopeWorkCost;
        this.maxSlope = maxSlope;
    }
}
