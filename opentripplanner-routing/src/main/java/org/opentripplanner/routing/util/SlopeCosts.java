package org.opentripplanner.routing.util;

public class SlopeCosts {
    public double slopeSpeedEffectiveLength;
    public double slopeWorkCost; // the cost in watt-seconds at 5 m/s 
    public double maxSlope;
    public double slopeSafetyCost; //an additional safety cost caused by the slope
    
    public SlopeCosts(double slopeSpeedEffectiveLength, double slopeWorkCost, double slopeSafetyCost, double maxSlope) {
        this.slopeSpeedEffectiveLength = slopeSpeedEffectiveLength;
        this.slopeWorkCost = slopeWorkCost;
        this.slopeSafetyCost = slopeSafetyCost;
        this.maxSlope = maxSlope;
    }
}
