package org.opentripplanner.routing.util;

public class SlopeCosts {
    public final boolean flattened;
    public final double slopeSpeedFactor; // The slope speed factor multiplier, w/o units
    public final double slopeWorkFactor; // The slope work factor in joules per meters at 5 m/s
    public final double maxSlope; // Max{abs(slope)}
    public final double slopeSafetyCost; // An additional safety cost caused by the slope
    public final double lengthMultiplier; // Multiplier to get true length based on flat (projected) length
    public final byte[] gradients; // array of gradients as percents
    public final short[] gradientLengths; // array of the length of each gradient in meters
    public final double maximumDragResistiveForceComponent; // the maximum resistive drag force component along an edge

    /**
     * The distance ajusted to incorporate the effect of the slope. Let say the
     * distance is 1000 m and 5% uphill, then we can use e.g. the Tobler function
     * to calculate the increase of 19% to walk such a distance. We add that
     * percentage to the 'flat' distance and get 1190m.
     */
    public final double effectiveWalkFactor;

    public SlopeCosts(double slopeSpeedFactor, double slopeWorkFactor, double slopeSafetyCost,
                      double maxSlope, double lengthMultiplier, boolean flattened, double effectiveWalkFactor,
                      byte[] gradients, short[] gradientLengths, double maximumDragResistiveForceComponent) {
        this.slopeSpeedFactor = slopeSpeedFactor;
        this.slopeWorkFactor = slopeWorkFactor;
        this.slopeSafetyCost = slopeSafetyCost;
        this.maxSlope = maxSlope;
        this.lengthMultiplier = lengthMultiplier;
        this.flattened = flattened;
        this.gradients = gradients;
        this.gradientLengths = gradientLengths;
        this.maximumDragResistiveForceComponent = maximumDragResistiveForceComponent;
        this.effectiveWalkFactor = effectiveWalkFactor;
    }
}
